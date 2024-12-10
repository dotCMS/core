Feature: Create an instance of a new Content Type and expect 200 OK
Background:
  * def extractErrors =
    """
    function(response) {
      var errors = [];
      var results = response.entity.results;
      if (results && results.length > 0) {
        for (var i = 0; i < results.length; i++) {
          var result = results[i];
        // Handle both nested error messages and direct error messages
          for (var key in result) {
            if (result[key] && result[key].errorMessage) {
              errors.push(result[key].errorMessage);
            }
          }
        }
      }
      return errors;
    }
    """
  * def buildRequestPayload =
    """
    function(contentType, title, publishDate, expiresOn) {
      var payload = {
        "contentlets": [
          {
            "contentType": contentType,
            "title": title,
            "contentHost": "default"
          }
        ]
      };
      if (publishDate) payload.contentlets[0].publishDate = publishDate;
      if (expiresOn) payload.contentlets[0].expiresOn = expiresOn;
      return payload;
    }
    """
  Scenario Outline: Create an instance of a new Content Type and expect 200 OK
    Given url baseUrl + '/api/v1/workflow/actions/default/fire/PUBLISH?indexPolicy=WAIT_FOR'
    And headers commonHeaders

    * karate.log('args:::', __arg)
    # Params are expected as arguments to the feature file
    * def contentTypeId = __arg.contentTypeId
    * def title = __arg.title
    * def publishDate = __arg.publishDate
    * def expiresOn = __arg.expiresOn

    * def requestPayload = buildRequestPayload (contentTypeId, title, publishDate, expiresOn)
    And request requestPayload

    When method POST
    Then status 200
    * def errors = call extractErrors response
    * match errors == []
    Examples:
      | name           | description          |
      | new            | THE DESCRIPTION 1    |

