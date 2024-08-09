Feature: Checking JSON Attributes

  Background:
    * def authString = 'admin@dotcms.com:admin'
    * def encodedAuth = function(s) { return java.util.Base64.getEncoder().encodeToString(s.getBytes('UTF-8')); }
    * def baseUrl = 'http://localhost:8080'

  Scenario: Checking Content Audit Attributes

    # Adding the content dependencies
    Given url baseUrl + '/api/v1/workflow/actions/default/fire/PUBLISH'
    And request { "contentlet": [{ "contentType": "webPageContent", "title": "Test Generic Content", "contentHost": "demo.dotcms.com", "body": "This is my Test Generic Content" }] }
    And header Content-Type = 'application/json'
    And header Authorization = 'Basic ' + encodedAuth(authString)
    When method post
    Then status 200
    * print 'Response:', response
    And match response.errors == []
    * def firstResult = response.entity.results[0]
    * def contentIdKey = karate.keysOf(firstResult)[0]
    * def contentId = firstResult[contentIdKey].identifier


    # Checking the content attributes
    Given url 'http://localhost:8080/api/v1/content/' + contentId
    And header Content-Type = 'application/json'
    And header Authorization = 'Basic ' + encodedAuth(authString)
    When method get
    Then status 200
    And match response.entity.contentType == 'webPageContent'
    And match response.entity.title == 'Test Generic Content'
    And match response.entity.hostName == 'demo.dotcms.com'
    And match response.entity.body == 'This is my Test Generic Content'
    And match response.entity.creationDate != null
    And match response.entity.owner != null
    And match response.entity.ownerName != null
    And match response.entity.modDate != null
    And match response.entity.modUser != null
    And match response.entity.modUserName != null
    And match response.entity.publishDate != null
    And match response.entity.publishUser != null
    And match response.entity.publishUserName != null


  Scenario: Creating Test Generic Content without parameters

    Given url baseUrl + '/api/v1/workflow/actions/default/fire/PUBLISH'
    And request { "contentlet": [{  }] }
    And header Content-Type = 'application/json'
    And header Authorization = 'Basic ' + encodedAuth(authString)
    When method post
    Then status 200
    * def jsonData = response
    * def results = jsonData.entity.results
    * def errors = []
    * results.forEach(function(result) { var key = karate.keysOf(result)[0]; var errorMessage = result[key].errorMessage; errors.push(errorMessage); })
    * match each errors contains 'Content Type does not exist'


  Scenario: Creating Test Generic Content - No title

    Given url baseUrl + '/api/v1/workflow/actions/default/fire/PUBLISH'
    And request { "contentlet": [{ "contentType": "webPageContent", "contentHost": "default", "body": "This is my Test Generic Content" }] }
    And header Content-Type = 'application/json'
    And header Authorization = 'Basic ' + encodedAuth(authString)
    When method post
    Then status 200
    * def jsonData = response
    * def results = jsonData.entity.results
    * def errors = []
    * results.forEach(function(result) { var key = karate.keysOf(result)[0]; var errorMessage = result[key].errorMessage; errors.push(errorMessage); })
    * match each errors contains 'has invalid / missing field(s)'

  Scenario: Creating Test Generic Content - No body

    Given url baseUrl + '/api/v1/workflow/actions/default/fire/PUBLISH'
    And request { "contentlet": [{ "contentType": "webPageContent", "title": "Test Generic Content",  "contentHost": "default" }] }
    And header Content-Type = 'application/json'
    And header Authorization = 'Basic ' + encodedAuth(authString)
    When method post
    Then status 200
    * def jsonData = response
    * def results = jsonData.entity.results
    * def errors = []
    * results.forEach(function(result) { var key = karate.keysOf(result)[0]; var errorMessage = result[key].errorMessage; errors.push(errorMessage); })
    * match each errors contains 'has invalid / missing field(s)'

  Scenario: Retrieving Test Generic Content - Wrong ID

    Given url 'http://localhost:8080/api/v1/content/wrongID'
    And header Content-Type = 'application/json'
    And header Authorization = 'Basic ' + encodedAuth(authString)
    When method get
    Then status 404
    * match response.message contains 'The contentlet wrongID and language 1 does not exist'

  Scenario: Creating Test Generic Content - No auth

    Given url baseUrl + '/api/v1/workflow/actions/default/fire/PUBLISH'
    And request { "contentlet": [{ "contentType": "webPageContent", "title": "Test Generic Content",  "contentHost": "default" }] }
    And header Content-Type = 'application/json'
    When method post
    Then status 401
    * match response contains 'CONTENT_APIS_ALLOW_ANONYMOUS permission exceeded'