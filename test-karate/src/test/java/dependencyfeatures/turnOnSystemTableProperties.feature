Feature: turnOnSystemTableProperties

  Background:
    * def baseUrl = 'http://localhost:8080'

  Scenario: TurnON_DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE

    Given url baseUrl + '/api/v1/system-table'
    And request { key: 'DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE', value: true }
    And header Content-Type = 'application/json'
    And header Authorization = authHeader
    When method POST
    Then status 200
    And match response.entity contains("DEFAULT_CONTENT_TO_DEFAULT_LANGUAGE saved/updated")