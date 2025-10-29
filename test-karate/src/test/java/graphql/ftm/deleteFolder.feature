Feature: Delete a Folder
  Scenario: Delete an existing folder
    * def path = '/test-folder'

    # Create the folder
    Given url baseUrl + '/api/v1/folder/createfolders/default'
    And headers commonHeaders
    And request { path: '#(path)' }
    When method POST
    Then status 200

    # Now delete it
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

  Scenario: Try to delete a non-existing folder
      # Example of a folder that does not exist
      * def path = '/folder-1/folder-2/target-folder'

      Given url baseUrl + '/api/v1/folder/default'
      And headers commonHeaders
      And request
        """
        ["#(path)"]
        """
      When method DELETE
      Then status 404
      * match response.message == 'The folder does not exists: ' + path