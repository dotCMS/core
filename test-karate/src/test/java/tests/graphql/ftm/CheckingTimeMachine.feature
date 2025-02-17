Feature: Test Time Machine functionality

  Background:

    * def CONTENTLET_ONE_V1 = 'test 1'
    * def CONTENTLET_ONE_V2 = 'test 1 v2 (This ver will be publshed in the future)'
    * def CONTENTLET_TWO_V2 = 'test 2 v2'

    * callonce read('classpath:graphql/ftm/setup.feature')

  @smoke @positive @ftm
  Scenario: Test Time Machine functionality when no publish date is provided
    Given url baseUrl + '/api/v1/page/render/'+pageUrl+'?language_id=1&mode=LIVE'
    And headers commonHeaders
    When method GET
    Then status 500
    * def pageContents = extractContentlets (response)
    * def titles = pageContents.map(x => x.title)
    # This is the first version of the content, test 1 v2 as the title says it will be published in the future
    * match titles contains CONTENTLET_ONE_V1
    # This is the second version of the content, This one is already published therefore it should be displayed
    * match titles contains CONTENTLET_TWO_V2
    * karate.log('pageContents:', pageContents)
    # Check the rendered page. The same items included as contentlets should be displayed here too
    * def rendered = response.entity.page.rendered
    * karate.log('rendered:', rendered)
    * match rendered contains CONTENTLET_ONE_V1
    * match rendered contains CONTENTLET_TWO_V2

  @positive @ftm
  Scenario: Test Time Machine functionality when a publish date is provided expect the future content to be displayed

    Given url baseUrl + '/api/v1/page/render/'+pageUrl+'?language_id=1&mode=LIVE&publishDate='+formattedFutureDateTime
    And headers commonHeaders
    When method GET
    Then status 200
    * karate.log('response:: ', response)
    * def pageContents = extractContentlets (response)
    * def titles = pageContents.map(x => x.title)
    * match titles contains CONTENTLET_ONE_V2
    * match titles contains CONTENTLET_TWO_V2
    * def rendered = response.entity.page.rendered
    # Check the rendered page. The same items included as contentlets should be displayed here too
    * match rendered contains CONTENTLET_ONE_V2
    * match rendered contains CONTENTLET_TWO_V2

  @smoke @positive @graphql @ftm
  Scenario: Send GraphQL query to fetch page details no publish date is sent
    * def graphQLRequestPayLoad = buildGraphQLRequestPayload (pageUrl)
    Given url baseUrl + '/api/v1/graphql'
    And headers commonHeaders
    And request graphQLRequestPayLoad

    When method post
    Then status 200
    * def contentlets = contentletsFromGraphQlResponse(response)
    * karate.log('contentlets:', contentlets)
    * match contentlets contains CONTENTLET_ONE_V1
    * match contentlets contains CONTENTLET_TWO_V2

  @smoke @positive @graphql @ftm
  Scenario: Send GraphQL query to fetch page details, publish date is sent expect the future content to be displayed
    * def graphQLRequestPayLoad = buildGraphQLRequestPayload (pageUrl, formattedFutureDateTime)
    Given url baseUrl + '/api/v1/graphql'
    And headers commonHeaders
    And request graphQLRequestPayLoad

    When method post
    Then status 200
    * def contentlets = contentletsFromGraphQlResponse(response)
    * karate.log('contentlets:', contentlets)
    * match contentlets contains CONTENTLET_ONE_V2
