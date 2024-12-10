Feature: Create a new version of a piece of content

  Scenario Outline: Create a new version of a piece of content
    Given url baseUrl + '/api/v1/workflow/actions/default/fire/PUBLISH?identifier='+identifier+'&indexPolicy=WAIT_FOR'
    And headers commonHeaders
    And request
      """
      {
        "contentlets": [
          {
            "identifier":"#(identifier)",
            "contentType": "#(contentType)",
            "title": "#(title)",
            "publishDate": "#(publishDate)",
            "expiresOn": "#(expiresOn)",
            "contentHost": "default"
          }
        ]
      }
      """
    When method POST
    Then status 200

    Examples:
      | name           | description          |
      | new | THE DESCRIPTION 1    |
