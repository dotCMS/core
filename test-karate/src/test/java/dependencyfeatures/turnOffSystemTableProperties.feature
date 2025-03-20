Feature: Turn off system table properties for default content to default language

  Background:
    * def authString = 'admin@dotcms.com:admin'
    * def encodedAuth = function(s) { return java.util.Base64.getEncoder().encodeToString(s.getBytes('UTF-8')); }
    * def baseUrl = 'http://localhost:8080'

  Scenario: TurnOFF_DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE

    Given url baseUrl + '/api/v1/system-table'
    And request { key: 'DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE', value: false }
    And header Content-Type = 'application/json'
    And header Authorization = 'Basic ' + encodedAuth(authString)
    When method POST
    Then status 200
    And match response.entity contains("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE saved/updated")