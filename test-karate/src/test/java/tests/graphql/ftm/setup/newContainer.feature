Feature: Create a Content Type
  Background:
    * def containerNameVariable = 'MyContainer' + Math.floor(Math.random() * 1000)

  Scenario Outline: Create a content type and expect 200 OK
    Given url baseUrl + '/api/v1/containers'
    And headers commonHeaders
    And request
      """
      {
        "title":"#(containerNameVariable)",
        "friendlyName":"My test container.",
        "maxContentlets":10,
        "notes":"Notes",
        "containerStructures":[
          {
            "structureId":"#(contentTypeId)",
            "code":"$!{dotContentMap.title}"
          }
        ]
      }
      """
    When method POST
    Then status 200

    Examples:
      | name           | description          |
      | newC | THE DESCRIPTION 1    |
