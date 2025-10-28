Feature: Delete a Folder
  Background:
    * url baseUrl + '/api/v1/folder/default'
    * headers commonHeaders

  Scenario: Try to delete a non-existing folder

    # Example of a folder that does not exist
    * def path = '/folder-1/folder-2/target-folder'
    And request
      """
      ["#(path)"]
      """
    When method DELETE
    Then status 404
    * match response.message == 'The folder does not exists: ' + path