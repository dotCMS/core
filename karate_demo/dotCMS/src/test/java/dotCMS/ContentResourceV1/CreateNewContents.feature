Feature: Create New Contents

  Background:
    * def authString = 'admin@dotcms.com:admin'
    * def encodedAuth = function(s) { return java.util.Base64.getEncoder().encodeToString(s.getBytes('UTF-8')); }
    * def baseUrl = 'http://localhost:8080'
    * def newContent = karate.callSingle('classpath:dotCMS/ContentResourceV1/DependencieFeatures/newContent.feature')
    * def contentId = newContent.contentId
    * def contentInode = newContent.contentInode


  Scenario: Content exists by identifier
    Given url 'http://localhost:8080/api/v1/content/' + contentId
    And header Content-Type = 'application/json'
    And header Authorization = 'Basic ' + encodedAuth(authString)
    When method get
    Then status 200
    And match response.entity.identifier == contentId


  Scenario: Content exists by identifier (Default Content to Default Language)
    #turn on default content to default language property
    * karate.callSingle('classpath:dotCMS/ContentResourceV1/DependencieFeatures/turnOnSystemTableProperties.feature')
    Given url 'http://localhost:8080/api/v1/content/' + contentId + '?language=2'
    And header Content-Type = 'application/json'
    And header Authorization = 'Basic ' + encodedAuth(authString)
    When method get
    Then status 200
    And match response.entity.identifier == contentId
    #turn off default content to default language property
    * karate.callSingle('classpath:dotCMS/ContentResourceV1/DependencieFeatures/turnOffSystemTableProperties.feature')

  Scenario: Content exists by inode
    Given url 'http://localhost:8080/api/v1/content/' + contentInode
    And header Content-Type = 'application/json'
    And header Authorization = 'Basic ' + encodedAuth(authString)
    When method get
    Then status 200
    And match response.entity.inode == contentInode

  Scenario: Content exists by inode (Default Content to Default Language)
    #turn on default content to default language property
    * karate.callSingle('classpath:dotCMS/ContentResourceV1/DependencieFeatures/turnOnSystemTableProperties.feature')
    Given url 'http://localhost:8080/api/v1/content/' + contentInode + '?language=2'
    And header Content-Type = 'application/json'
    And header Authorization = 'Basic ' + encodedAuth(authString)
    When method get
    Then status 200
    And match response.entity.inode == contentInode
    #turn off default content to default language property
    * karate.callSingle('classpath:dotCMS/ContentResourceV1/DependencieFeatures/turnOffSystemTableProperties.feature')

  Scenario: Content can lock
    Given url 'http://localhost:8080/api/v1/content/_canlock/' + contentId
    And header Content-Type = 'application/json'
    And header Authorization = 'Basic ' + encodedAuth(authString)
    When method get
    Then status 200
    And match response.entity.id == contentId
    And match response.entity.canLock == true


    #  ------------------------ Here the test ------------------------

  Scenario: Content not exists by identifier
    * def WRONG_ID = "6faf3063-5478-4e0a-a44b-dba540ec79"
    Given url 'http://localhost:8080/api/v1/content/' + WRONG_ID
    And header Content-Type = 'application/json'
    And header Authorization = 'Basic ' + encodedAuth(authString)
    When method get
    Then status 404
    And match response.message contains ('contentlet 6faf3063-5478-4e0a-a44b-dba540ec79 and language 1 does not exist')

  Scenario: Content not exists by inode
    * def WRONG_Inode = "21b19188-bd85-4baa-bba2-5b54cd1cb3ea"
    Given url 'http://localhost:8080/api/v1/content/' + WRONG_Inode
    And header Content-Type = 'application/json'
    And header Authorization = 'Basic ' + encodedAuth(authString)
    When method get
    Then status 404
    And match response.message contains ('contentlet 21b19188-bd85-4baa-bba2-5b54cd1cb3ea and language 1 does not exist')



  Scenario: UpdateContent_WithoutCredentials
    * karate.callSingle('classpath:dotCMS/ContentResourceV1/DependencieFeatures/turnOffSystemTableProperties.feature')
    Given url baseUrl + '/api/v1/workflow/actions/default/fire/PUBLISH'
    And request { "contentlet": { "stInode" : "f4d7c1b8-2c88-4071-abf1-a5328977b07d", "languageId" : 1, "identifier": '#(contentId)' , "key": "UpdatedKey", "value": "Updatedvalue"    }  }
    And header Content-Type = 'application/json'
    #And header Authorization = 'Basic ' + encodedAuth('admin@dotcms.com:admin1')
    When method put
    Then status 401
    * print 'Response:', response
    And match response contains ('CONTENT_APIS_ALLOW_ANONYMOUS permission exceeded - system set to READ but WRITE was required')

  Scenario: UpdateContent_WrongCredentials
    * karate.callSingle('classpath:dotCMS/ContentResourceV1/DependencieFeatures/turnOffSystemTableProperties.feature')
    Given url baseUrl + '/api/v1/workflow/actions/default/fire/PUBLISH'
    And request { "contentlet": { "stInode" : "f4d7c1b8-2c88-4071-abf1-a5328977b07d", "languageId" : 1, "identifier": '#(contentId)' , "key": "UpdatedKey", "value": "Updatedvalue"    }  }
    And header Content-Type = 'application/json'
    And header Authorization = 'Basic ' + encodedAuth('admin@dotcms.com:admin1')
    When method put
    Then status 401
    * print 'Response:', response
    And match response contains ('Authentication credentials are required')