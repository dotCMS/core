 Feature: Create a Page
 Scenario: Create a new version of a piece of content
    Given url baseUrl + '/api/v1/workflow/actions/default/fire/PUBLISH?indexPolicy=WAIT_FOR'
    And headers commonHeaders
    And request
      """
      {
        "contentlet" : {
          "title" : "#(title)",
          "url": "#(pageUrl)",
          "languageId" : 1,
          "stInode": "c541abb1-69b3-4bc5-8430-5e09e5239cc8",
          "template": "#(templateId)",
          "friendlyName": "#(title)",
          "hostFolder": "8a7d5e23-da1e-420a-b4f0-471e7da8ea2d",
          "cachettl": 0,
          "sortOrder": 0
        }
      }
      """
    When method POST
    Then status 200
    * def errors = call extractErrors response
    * match errors == []