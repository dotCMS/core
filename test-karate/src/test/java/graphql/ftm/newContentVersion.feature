Feature: Create a new version of a piece of content
  Scenario: Create a new version of a piece of content

    # Params are expected as arguments to the feature file
    * def contentTypeId = __arg.contentTypeId
    * def title = __arg.title
    * def publishDate = __arg.publishDate
    * def expiresOn = __arg.expiresOn
    * def identifier = __arg.identifier
    * def urlTitle = __arg.urlTitle
    * def imageId = __arg.imageId

    Given url baseUrl + '/api/v1/workflow/actions/default/fire/PUBLISH?identifier='+identifier+'&indexPolicy=WAIT_FOR'
    And headers commonHeaders
    * def requestPayload = buildContentRequestPayload (contentTypeId, title, publishDate, expiresOn, identifier, urlTitle, imageId)
    And request requestPayload
    When method POST
    Then status 200
    * def errors = call extractErrors response
    * match errors == []
