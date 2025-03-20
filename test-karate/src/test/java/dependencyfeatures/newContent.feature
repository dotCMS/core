Feature: Create content dependencies for tests

  Background:
    * def authString = 'admin@dotcms.com:admin'
    * def encodedAuth = function(s) { return java.util.Base64.getEncoder().encodeToString(s.getBytes('UTF-8')); }
    * def baseUrl = 'http://localhost:8080'

  Scenario: SuccessRequest

    # Adding the content dependencies
    Given url baseUrl + '/api/v1/workflow/actions/default/fire/PUBLISH'

    #Variables to set timestamp name in the body request
    * def DateTimeFormatter = Java.type('java.time.format.DateTimeFormatter')
    * def LocalDateTime = Java.type('java.time.LocalDateTime')
    * def formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    * def now = LocalDateTime.now()
    * def timestamp = now.format(formatter)
    * def timestampString = 'SucessRequest' + timestamp

    And request { "contentlet": { "stInode" : "f4d7c1b8-2c88-4071-abf1-a5328977b07d", "languageId" : 1, "key": "#(timestampString)", "value": "#(timestampString)"  }  }
    And header Content-Type = 'application/json'
    And header Authorization = 'Basic ' + encodedAuth(authString)
    When method put
    Then status 200
    #* print 'Response:', response
    And match response.errors == []
    And match response.entity.title contains("#(timestampString)")
    * def contentId = response.entity.identifier
    * def contentInode = response.entity.inode