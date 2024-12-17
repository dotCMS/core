Feature: Test Time Machine functionality

  Background:
    * callonce read('classpath:graphql/ftm/setup.feature')

  @smoke @positive
  Scenario: Test Time Machine functionality when no publish date is provided
    Given url baseUrl + '/api/v1/page/render/'+pageUrl+'?language_id=1&mode=LIVE'
    And headers commonHeaders
    When method GET
    Then status 200
    * def pageContents = extractContentlets (response)

    * def contentPieceOne = getContentletByUUID(contentPieceOne, contentPieceOneId)
    * def contentPieceTwo = getContentletByUUID(contentPieceTwo, contentPieceTwoId)

    * def titles = pageContents.map(x => x.title)
    # This is the first version of the content, test 1 v2 as the title says it will be published in the future
    * match titles contains 'test 1'
    # This is the second version of the content, Thisone is already published therefore it should be displayed
    * match titles contains 'test 2 v2'

  @positive
  Scenario: Test Time Machine functionality when a publish date is provided expect the future content to be displayed

    Given url baseUrl + '/api/v1/page/render/'+pageUrl+'?language_id=1&mode=LIVE&publishDate='+formattedFutureDateTime
    And headers commonHeaders
    When method GET
    Then status 200
    * def pageContents = extractContentlets (response)

    * def contentPieceOne = getContentletByUUID(contentPieceOne, contentPieceOneId)
    * def contentPieceTwo = getContentletByUUID(contentPieceTwo, contentPieceTwoId)

    * def titles = pageContents.map(x => x.title)
    * match titles contains 'test 1 v2 (This ver will be publshed in the future)'

  @smoke @positive
  Scenario: Send GraphQL query to fetch page details no publish date is sent
    * def graphQLRequestPayLoad = buildGraphQLRequestPayload (pageUrl)
    Given url baseUrl + '/api/v1/graphql'
    And headers commonHeaders
    And request graphQLRequestPayLoad

    When method post
    Then status 200
    * def contentlets = contentletsFromGraphQlResponse(response)
    * karate.log('contentlets:', contentlets)
    * match contentlets contains 'test 1'
    * match contentlets contains 'test 2 v2'

  @smoke @positive
  Scenario: Send GraphQL query to fetch page details, publish date is sent expect the future content to be displayed
    * def graphQLRequestPayLoad = buildGraphQLRequestPayload (pageUrl, formattedFutureDateTime)
    Given url baseUrl + '/api/v1/graphql'
    And headers commonHeaders
    And request graphQLRequestPayLoad

    When method post
    Then status 200
    * def contentlets = contentletsFromGraphQlResponse(response)
    * karate.log('contentlets:', contentlets)
    * match contentlets contains 'test 1 v2 (This ver will be publshed in the future)'
