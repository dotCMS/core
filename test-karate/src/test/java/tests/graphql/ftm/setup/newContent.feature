Feature: Create an instance of a new Content Type and expect 200 OK
Background:

  Scenario Outline: Create an instance of a new Content Type and expect 200 OK

    # Params are expected as arguments to the feature file
    * def contentTypeId = __arg.contentTypeId
    * def title = __arg.title
    * def publishDate = __arg.publishDate
    * def expiresOn = __arg.expiresOn

    Given url baseUrl + '/api/v1/workflow/actions/default/fire/PUBLISH?indexPolicy=WAIT_FOR'
    And headers commonHeaders

    * def requestPayload = buildContentRequestPayload (contentTypeId, title, publishDate, expiresOn)
    And request requestPayload

    When method POST
    Then status 200
    * def errors = call extractErrors response
    * match errors == []
    Examples:
      | scenario                                                                                           | expected result                                 |
      | We simply create a new Piece Content which we will use to work on. No publishDate is provided here | We should succeed creating the piece of content |