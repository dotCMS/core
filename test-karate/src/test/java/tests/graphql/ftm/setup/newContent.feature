Feature: Create an instance of a new Content Type and expect 200 OK

  Scenario Outline: Create an instance of a new Content Type and expect 200 OK
    Given url baseUrl + '/api/v1/workflow/actions/default/fire/PUBLISH?indexPolicy=WAIT_FOR'
    And headers commonHeaders
    And request
      """
      {
        "contentlets": [
          {
            "contentType": "#(contentTypeVariable)",
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
      | newC | THE DESCRIPTION 1    |

