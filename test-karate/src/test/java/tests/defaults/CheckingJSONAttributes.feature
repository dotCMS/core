Feature: Checking JSON Attributes

  Background:
    * def authString = 'admin@dotcms.com:admin'
    * def encodedAuth = function(s) { return java.util.Base64.getEncoder().encodeToString(s.getBytes('UTF-8')); }
    * def baseUrl = 'http://localhost:8080'
    * def commonHeaders = { 'Content-Type': 'application/json'}
    * commonHeaders['Authorization'] = 'Basic ' + encodedAuth(authString)
    * def extractErrors =
      """
      function(response) {
        var errors = [];
        var results = response.entity.results;
        if (results && results.length > 0) {
          for (var i = 0; i < results.length; i++) {
            var result = results[i];
          // Handle both nested error messages and direct error messages
            for (var key in result) {
              if (result[key] && result[key].errorMessage) {
                errors.push(result[key].errorMessage);
              }
            }
          }
        }
        return errors;
      }
      """

  Scenario: Checking Content Audit Attributes
    Given url baseUrl + '/api/v1/workflow/actions/default/fire/PUBLISH'
    And request
      """
      {
        "contentlet": [
          {
            "contentType": "webPageContent",
            "title": "Test Generic Content",
            "contentHost": "default",
            "body": "This is my Test Generic Content"
          }
        ]
      }
      """
    And headers commonHeaders
    When method post
    Then status 200
    * def firstResult = response.entity.results[0]
    * def contentId = karate.keysOf(firstResult)[0]
    * def contentIdentifier = firstResult[contentId].identifier
    Given url baseUrl + '/api/v1/content/' + contentIdentifier
    And headers commonHeaders
    When method get
    Then status 200
    And match response.entity contains
      """
      {
        "contentType": "webPageContent",
        "title": "Test Generic Content",
        "hostName": "default",
        "body": "This is my Test Generic Content",
        "creationDate": "#notnull",
        "owner": "#notnull",
        "ownerUserName": "#notnull",
        "modDate": "#notnull",
        "modUser": "#notnull",
        "modUserName": "#notnull",
        "publishDate": "#notnull",
        "publishUser": "#notnull",
        "publishUserName": "#notnull"
      }
      """

  Scenario Outline: Testing Content Creation Validation
    Given url baseUrl + '/api/v1/workflow/actions/default/fire/PUBLISH'
    And request <payload>
    And headers commonHeaders
    When method post
    Then status 200
    * def errors = call extractErrors response
    * match errors contains <expectedError>

    Examples:
      | payload                                                                                        | expectedError                       |
      | { "contentlet": [{}] }                                                                         | 'Content Type does not exist'       |
      | { "contentlet": [{ "contentType": "webPageContent", "contentHost": "default" }] }              | 'Contentlet with ID \'Unknown/New\' [\'\'] has invalid/missing field(s). - Fields: [REQUIRED]: Title (title), Body (body)'      |
      | { "contentlet": [{ "contentType": "webPageContent", "title": "", "contentHost": "default" }] } | 'Contentlet with ID \'Unknown/New\' [\'\'] has invalid/missing field(s). - Fields: [REQUIRED]: Title (title), Body (body)'      |

  Scenario: Retrieving Test Generic Content - Wrong ID
    Given url baseUrl + '/api/v1/content/wrongID'
    And headers commonHeaders
    When method get
    Then status 404
    And match response.message contains 'The contentlet wrongID and language 1 does not exist'

  Scenario: Creating Test Generic Content - No auth
    Given url baseUrl + '/api/v1/workflow/actions/default/fire/PUBLISH'
    And request
      """
      {
        "contentlet": [
          {
            "contentType": "webPageContent",
            "title": "Test Generic Content",
            "contentHost": "default"
          }
        ]
      }
      """
    And header Content-Type = 'application/json'
    When method post
    Then status 401
    And match response contains 'CONTENT_APIS_ALLOW_ANONYMOUS permission exceeded'