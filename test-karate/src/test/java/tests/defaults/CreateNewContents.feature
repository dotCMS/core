Feature: Content Management API Tests

  Background:
    * def authString = 'admin@dotcms.com:admin'
    * def encodedAuth = function(s) { return java.util.Base64.getEncoder().encodeToString(s.getBytes('UTF-8')); }
    * def baseUrl = baseUrl + ''
    * def authHeader = 'Basic ' + encodedAuth(authString)
    * def commonHeaders = { 'Content-Type': 'application/json', 'Authorization': '#(authHeader)' }
    * def newContent = karate.callSingle('classpath:dependencyfeatures/newContent.feature')
    * def contentId = newContent.contentId
    * def contentInode = newContent.contentInode

  @smoke @positive
  Scenario Outline: Verify content retrieval by <type> with default language
    # Turn on system properties
    * call read('classpath:dependencyfeatures/turnOnSystemTableProperties.feature')
    * configure cookies = null
    Given url baseUrl + '/api/v1/content/' + <id> + '?language=2'
    And headers commonHeaders
    When method get
    Then status 200
    And match response.entity.<matchField> == <expectedValue>

    # Turn off system properties
    * call read('classpath:dependencyfeatures/turnOffSystemTableProperties.feature')

    Examples:
      | type       | id          | matchField | expectedValue |
      | identifier | contentId   | identifier | contentId     |
      | inode     | contentInode | inode     | contentInode  |

  @negative @security
  Scenario Outline: Verify authentication requirements for content update - <scenario>
    # Turn off system properties
    * call read('classpath:dependencyfeatures/turnOffSystemTableProperties.feature')
    * configure cookies = null
    Given url baseUrl + '/api/v1/workflow/actions/default/fire/PUBLISH'
    And request
      """
      {
        "contentlet": {
          "stInode": "f4d7c1b8-2c88-4071-abf1-a5328977b07d",
          "languageId": 1,
          "identifier": '#(contentId)',
          "key": "UpdatedKey",
          "value": "Updatedvalue"
        }
      }
      """
    * def auth = (useAuth == 'true') ? ('Basic ' + encodedAuth(credentials)) : null
    And header Authorization = auth
    And header Content-Type = 'application/json'
    When method put
    Then status 401
    And match response contains expectedError

    Examples:
      | scenario          | useAuth | credentials               | expectedError                                                                            |
      | No Credentials    | false   | null                      | CONTENT_APIS_ALLOW_ANONYMOUS permission exceeded - system set to READ but WRITE was required |
      | Wrong Credentials | true    | 'admin@dotcms.com:admin1' | Authentication credentials are required                                               |