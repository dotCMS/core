Feature: Test Time Machine functionality

  Background:

    * def CONTENTLET_ONE_V1 = 'test 1'
    * def CONTENTLET_ONE_V2 = 'test 1 v2 (This ver will be publshed in the future)'
    * def CONTENTLET_TWO_V2 = 'test 2 v2'
    * def CONTENTLET_THREE_V1 = 'test 3'
    * def CONTENTLET_THREE_V2 = 'test 3 v2'

    * def BANNER_CONTENTLET_ONE_V1 = 'banner 1'
    * def BANNER_CONTENTLET_ONE_V2 = 'banner 1 v2'
    * def NON_PUBLISHED_CONTENTLET = 'Working version Only! with publish date'

    * def URL_CONTENT_MAP_TITLE_V1 = 'url-content-map-test-1-v1'
    * def URL_CONTENT_MAP_TITLE_V2 = 'url-content-map-test-1-v2'

    * callonce read('classpath:common/utils.feature')
    * callonce read('classpath:graphql/ftm/setup.feature')

  @smoke @positive @ftm
  Scenario: Test Time Machine functionality when no publish date is provided
    Given url baseUrl + '/api/v1/page/render/'+pageUrl+'?language_id=1&mode=LIVE'
    And headers commonHeaders
    When method GET
    Then status 200
    * def pageContents = extractContentlets (response)
    * def titles = pageContents.map(x => x.title)
    # This is the first version of the content, test 1 v2 as the title says it will be published in the future
    * match titles contains CONTENTLET_ONE_V1
    # This is the second version of the content, This one is already published therefore it should be displayed
    * match titles contains CONTENTLET_TWO_V2
    # This is the first version of the banner content which is already published therefore it should be displayed
    * match titles contains BANNER_CONTENTLET_ONE_V1

    * match titles !contains CONTENTLET_ONE_V2
    * match titles !contains NON_PUBLISHED_CONTENTLET
    * match titles !contains BANNER_CONTENTLET_ONE_V2

    * karate.log('pageContents:', pageContents)
    # Check the rendered page. The same items included as contentlets should be displayed here too
    * def rendered = response.entity.page.rendered
    * karate.log('rendered:', rendered)
    * match rendered contains CONTENTLET_ONE_V1
    * match rendered contains CONTENTLET_TWO_V2
    * match rendered contains BANNER_CONTENTLET_ONE_V1

    * match rendered !contains NON_PUBLISHED_CONTENTLET
    * match rendered !contains CONTENTLET_ONE_V2
    * match rendered !contains BANNER_CONTENTLET_ONE_V2

    # Since the request to PageResource removed the TM_DATE session attribute,
    # we do not expect the future banner image to be displayed
    * call read('classpath:graphql/ftm/validateBanner.feature') { imageName: 'draft.png', contentLength: '18137' }

  @smoke @positive @ftm
  Scenario: Test Time Machine functionality when publish date is provided with current date
    Given url baseUrl + '/api/v1/page/render/'+pageUrl+'?language_id=1&mode=LIVE&publishDate='+formattedCurrentDateTime
    And headers commonHeaders
    When method GET
    Then status 200
    * def pageContents = extractContentlets (response)
    * def titles = pageContents.map(x => x.title)
    # This is the first version of the content, test 1 v2 as the title says it will be published in the future
    * match titles contains CONTENTLET_ONE_V1
    # This is the second version of the content, this one is already published therefore it should be displayed
    * match titles contains CONTENTLET_TWO_V2
    # This is the first version of the banner content which is already published therefore it should be displayed
    * match titles contains BANNER_CONTENTLET_ONE_V1

    * match titles !contains CONTENTLET_ONE_V2
    * match titles !contains NON_PUBLISHED_CONTENTLET
    * match titles !contains BANNER_CONTENTLET_ONE_V2

    * karate.log('pageContents:', pageContents)
    # Check the rendered page. The same items included as contentlets should be displayed here too
    * def rendered = response.entity.page.rendered
    * karate.log('rendered:', rendered)
    * match rendered contains CONTENTLET_ONE_V1
    * match rendered contains CONTENTLET_TWO_V2
    * match rendered contains BANNER_CONTENTLET_ONE_V1

    * match rendered !contains CONTENTLET_ONE_V2
    * match rendered !contains NON_PUBLISHED_CONTENTLET
    * match rendered !contains BANNER_CONTENTLET_ONE_V2

    # Since the request to PageResource set the TM_DATE session attribute with the current date,
    # we do not expect the future banner image to be displayed
    * call read('classpath:graphql/ftm/validateBanner.feature') { imageName: 'draft.png', contentLength: '18137' }

  @positive @ftm
  Scenario: Test Time Machine functionality when a publish date is provided within grace window expect the future content not to be displayed
    Given url baseUrl + '/api/v1/page/render/'+pageUrl+'?language_id=1&mode=LIVE&publishDate='+formattedFutureDateTimeInGraceWindow
    And headers commonHeaders
    When method GET
    Then status 200
    * karate.log('request date now:: ', java.time.LocalDateTime.now())
    * def pageContents = extractContentlets (response)
    * def titles = pageContents.map(x => x.title)
    # This is the first version of the content, this one is already published therefore it should be displayed
    * match titles contains CONTENTLET_THREE_V1
    # This is the second version of the content, this one is already published but is within the FTM grace window,
    # therefore it should not be displayed
    * match titles !contains CONTENTLET_THREE_V2

    * karate.log('pageContents:', pageContents)
    # Check the rendered page. The same items included as contentlets should be displayed here too
    * def rendered = response.entity.page.rendered
    * karate.log('rendered:', rendered)
    * match rendered contains CONTENTLET_THREE_V1
    * match rendered !contains CONTENTLET_THREE_V2


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
    * match titles contains NON_PUBLISHED_CONTENTLET
    * match titles contains BANNER_CONTENTLET_ONE_V2

    * match titles !contains CONTENTLET_ONE_V1
    * match titles !contains BANNER_CONTENTLET_ONE_V1

    * karate.log('pageContents:', pageContents)
    # Check the rendered page. The same items included as contentlets should be displayed here too
    * def rendered = response.entity.page.rendered
    * karate.log('rendered:', rendered)
    * match rendered contains CONTENTLET_ONE_V2
    * match rendered contains CONTENTLET_TWO_V2

    * match rendered contains NON_PUBLISHED_CONTENTLET

    * match rendered contains BANNER_CONTENTLET_ONE_V2

    # Since the request to PageResource set the TM_DATE session attribute with the future date,
    # we expect the future banner image to be displayed
    * call read('classpath:graphql/ftm/validateBanner.feature') { imageName: 'java-image.png', contentLength: '46239' }

  @positive @ftm
  Scenario: Test Time Machine functionality in UrlContentMap when the current date is provided expect urlContentMap
  title to match rendered one.

    Given url baseUrl + '/api/v1/page/render/'+urlMapContentPieceOneUrl+'?language_id=1&mode=LIVE&publishDate='+formattedCurrentDateTime
    And headers commonHeaders
    When method GET
    Then status 200
    * karate.log('response:: ', response)

    * def urlContentMap = response.entity.urlContentMap
    * def urlContentMapTitle = urlContentMap.title
    * karate.log('urlContentMapTitle:', urlContentMapTitle)

    # Expect the first version of the content to be displayed
    * match urlContentMapTitle contains URL_CONTENT_MAP_TITLE_V1

    # Check the rendered page. The same items included as contentlets should be displayed here too
    * def rendered = response.entity.page.rendered
    * karate.log('rendered:', rendered)
    * match rendered contains URL_CONTENT_MAP_TITLE_V1

  @positive @ftm
  Scenario: Test Time Machine functionality in UrlContentMap when a publish date is provided expect urlContentMap
  title to match rendered one.

    * def fullUrl = baseUrl + '/api/v1/page/render/'+urlMapContentPieceOneUrl+'?language_id=1&mode=LIVE&publishDate='+formattedFutureDateTime
    * def cleanedUrl = cleanUrl(fullUrl)
    Given url cleanedUrl
    And headers commonHeaders
    When method GET
    Then status 200
    * karate.log('response:: ', response)

    * def urlContentMap = response.entity.urlContentMap
    * def urlContentMapTitle = urlContentMap.title
    * karate.log('urlContentMapTitle:', urlContentMapTitle)

    # Expect the second version of the content to be displayed
    * match urlContentMapTitle contains URL_CONTENT_MAP_TITLE_V2

    # Check the rendered page. The same items included as contentlets should be displayed here too
    * def rendered = response.entity.page.rendered
    * karate.log('rendered:', rendered)
    * match rendered contains URL_CONTENT_MAP_TITLE_V2


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

  @positive @ftm
  Scenario: Test Time Machine functionality in UrlContentMap when mode is LIVE and a publish date is NOT provided expect 404
  title to match rendered one.

    * def fullUrl = baseUrl + '/api/v1/page/render/'+urlUnpublishedContentMap+'?language_id=1&mode=LIVE'
    * def cleanedUrl = cleanUrl(fullUrl)
    Given url cleanedUrl
    And headers commonHeaders
    When method GET
    Then status 404

  @smoke @positive @graphql @ftm
  Scenario: Send GraphQL query to fetch page details on a unpublished UrlContentMap,
  No publish date is sent expect 404 since the urlMap is unpublished

    * def fullUrl = baseUrl + '/'+urlUnpublishedContentMap+'?language_id=1&mode=LIVE'
    * def cleanedUrl = cleanUrl(fullUrl)
    * karate.log('pageUrl:', cleanedUrl)
    * def graphQLRequestPayLoad = buildGraphQLRequestPayload (cleanedUrl)
    Given url baseUrl + '/api/v1/graphql'
    And headers commonHeaders
    And request graphQLRequestPayLoad
    When method post
    Then status 200
    * match response.data.page == null