Feature: Create a Page

  Scenario Outline: Create a new version of a piece of content
    Given url baseUrl + '/api/v1/workflow/actions/default/fire/PUBLISH?indexPolicy=WAIT_FOR'
    And headers commonHeaders
    And request
      """
      {
        "contentlet" : {
          "title" : "lol",
          "url": "lol",
          "languageId" : 1,
          "stInode": "c541abb1-69b3-4bc5-8430-5e09e5239cc8",
          "template": "SYSTEM_TEMPLATE",
          "friendlyName": "friendlyName",
          "contentHost": "default"
        }
      }
      """
    When method POST
    Then status 200
    Examples:
      | name           | description          |
      | new | THE DESCRIPTION 1    |
