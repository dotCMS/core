Feature: Create a Page
 Background:
  Scenario Outline: Create a new version of a piece of content
    Given url baseUrl + '/api/v1/workflow/actions/default/fire/PUBLISH?indexPolicy=WAIT_FOR'
    And headers commonHeaders
    * def pageUrl = (__arg.pageUrl ? __arg.pageUrl : 'ftm-test-page' + Math.floor(Math.random() * 10000))
    * def templateId = (__arg.templateId ? __arg.templateId : 'SYSTEM_TEMPLATE')
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
    Examples:
      | scenario                                                                          | expected result                     |
      | We simply create a Page to hold the template/container and therefore the contents | We should succeed creating the page |
