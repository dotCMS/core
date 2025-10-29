Feature: Delete a Folder
  Scenario: Delete an existing folder
    # Create a folder
    * def assetPath = '//default' + path + '/'
    * def createRequest =
      """
      {
        "assetPath": "#(assetPath)",
        "data": {
          "title": "application/containers/banner",
          "showOnMenu": false,
          "sortOrder": 1,
          "defaultAssetType": "FileAsset"
        }
      }
      """

    Given url baseUrl + '/api/v1/assets/folders'
    And headers commonHeaders
    And request createRequest
    When method POST
    Then status 200
    * def errors = call extractErrors response
    * match errors == []

    # Delete an existing folder (previously created)
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
    * def nonExistingFolder = '/does-not-exist-folder'

    Given url baseUrl + '/api/v1/folder/default'
    And headers commonHeaders
    And request ["#(nonExistingFolder)"]
    When method DELETE
    Then status 404
    * match response.message == 'The folder does not exists: ' + nonExistingFolder