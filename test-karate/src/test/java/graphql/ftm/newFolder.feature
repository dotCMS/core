Feature: Create a Folder
  Scenario: Create a new folder on the given path
    Given url baseUrl + '/api/v1/folder/createfolders/default'
    And headers commonHeaders
    And request
      """
      ["#(path)"]
      """
    When method POST
    Then status 200
    * def errors = call extractErrors response
    * match errors == []