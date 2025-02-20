Feature: Delete a Folder
  Scenario: Delete a folder on the given path if exists
    Given url baseUrl + '/api/v1/folder/default'
    And headers commonHeaders
    And request
      """
      ["#(path)"]
      """
    When method DELETE
    Then status 200
    * def errors = call extractErrors response
    * match errors == []