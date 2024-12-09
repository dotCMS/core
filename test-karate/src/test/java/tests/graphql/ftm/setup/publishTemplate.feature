Feature: Publish a Template
  Background:

  Scenario Outline: Create a new Template
    Given url baseUrl + '/api/v1/templates/_publish'
    And headers commonHeaders
    And request
    """
      ["#(templateId)"]
    """
    When method PUT
    Then status 200

    Examples:
      | name             | description          |
      | publish template | THE DESCRIPTION 1    |
