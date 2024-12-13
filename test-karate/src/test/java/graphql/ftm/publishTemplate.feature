Feature: Publish a Template
  Background:

  Scenario: Create a new Template
    Given url baseUrl + '/api/v1/templates/_publish'
    And headers commonHeaders
    And request
    """
      ["#(templateId)"]
    """
    When method PUT
    Then status 200
