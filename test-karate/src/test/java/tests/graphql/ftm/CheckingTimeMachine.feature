Feature: Test Time Machine functionality

  Background:
    * callonce read('classpath:tests/graphql/ftm/setup/helpers.feature')

    # Make the prefix available to the scenario
    # Setup required data
    # Lets start by creating a new content type, container, template and publish the template
    # First the Content Type
    * def contentTypeResult = callonce read('classpath:tests/graphql/ftm/setup/newContentType.feature')
    * def contentTypeId = contentTypeResult.response.entity[0].id
    * def contentTypeVariable = contentTypeResult.response.entity[0].variable
    # Now the container, template and publish the template
    * def containerResult = callonce read('classpath:tests/graphql/ftm/setup/newContainer.feature') { contentTypeId: '#(contentTypeId)' }
    * def containerId = containerResult.response.entity.identifier
    * def templateResult = callonce read('classpath:tests/graphql/ftm/setup/newTemplate.feature') { containerId: '#(containerId)' }
    * def templateId = templateResult.response.entity.identifier
    * callonce read('classpath:tests/graphql/ftm/setup/publishTemplate.feature') { templateId: '#(templateId)' }

    # Create a couple of new pieces of content
    * def createContentPieceOneResult = callonce read('classpath:tests/graphql/ftm/setup/newContent.feature') { contentTypeId: '#(contentTypeId)', title: 'test 1' }
    * def contentPieceOne = createContentPieceOneResult.response.entity.results
    * def contentPieceOneId = contentPieceOne.map(result => Object.keys(result)[0])
    * def contentPieceOneId = contentPieceOneId[0]

    * def createContentPieceTwoResult = callonce read('classpath:tests/graphql/ftm/setup/newContent.feature') { contentTypeId: '#(contentTypeId)', title: 'test 2' }
    * def contentPieceTwo = createContentPieceTwoResult.response.entity.results
    * def contentPieceTwoId = contentPieceTwo.map(result => Object.keys(result)[0])
    * def contentPieceTwoId = contentPieceTwoId[0]

    # Now lets create a new version for each piece of content
    * def formatter = java.time.format.DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')
    * def now = java.time.LocalDateTime.now()
    * def futureDateTime = now.plusDays(10)
    * def formattedFutureDateTime = futureDateTime.format(formatter)

    * def newContentPiceOneVersion2 = callonce read('classpath:tests/graphql/ftm/setup/newContentVersion.feature') {  contentTypeId: '#(contentTypeId)', identifier: '#(contentPieceOneId)', title: 'test 1 v2 (This ver will be publshed in the future)', publishDate: '#(formattedFutureDateTime)' }
    * def newContentPiceTwoVersion2 = callonce read('classpath:tests/graphql/ftm/setup/newContentVersion.feature') {  contentTypeId: '#(contentTypeId)', identifier: '#(contentPieceTwoId)', title: 'test 2 v2' }

    * def pageUrl = 'ftm-test-page' + Math.floor(Math.random() * 10000)
    # Finally lets create a new page
    * def createPageResult = callonce read('classpath:tests/graphql/ftm/setup/newPage.feature') { pageUrl:'#(pageUrl)' ,title: 'Future Time Machine Test page', templateId:'#(templateId)' }
    * karate.log('createPageResult:', createPageResult)
    * def pages = createPageResult.response.entity.results
    * def pageId = pages.map(result => Object.keys(result)[0])
    * def pageId = pageId[0]

    * def publishPageResult =  callonce read('classpath:tests/graphql/ftm/setup/publishPage.feature') { page_id: '#(pageId)', content1_id: '#(contentPieceOneId)', content2_id: '#(contentPieceTwoId)', container_id: '#(containerId)' }
    #
  @smoke
  Scenario Outline: Test Time Machine functionality when no publish date is provided
    Given url baseUrl + '/api/v1/page/render/'+pageUrl+'?language_id=1&mode=LIVE'
    And headers commonHeaders
    When method GET
    Then status 200
    * def pageContents = call extractContentlets response
    * karate.log('pageContents:', pageContents)

    * def contentPieceOne = getContentletByUUID(contentPieceOne, contentPieceOneId)
    * def contentPieceTwo = getContentletByUUID(contentPieceTwo, contentPieceTwoId)

    # This is the first version of the content, test 1 v2 as the title says it will be published in the future
    * match pageContents[0].title == 'test 1'
    # This is the second version of the content, Thisone is already published therefore it should be displayed
    * match pageContents[1].title == 'test 2 v2'

    Examples:
        | scenario                                                                                           | expected result                                 |
        | We simply create a new Piece Content which we will use to work on. No publishDate is provided here | We should succeed creating the piece of content |

    Scenario Outline: Send GraphQL query to fetch page details
      Given url baseUrl + '/api/v1/graphql'
      And headers { "Content-Type": "application/json" }
      And request
        """
        {
          "query": "query Page { page(url: \"#(pageUrl)\") { containers { containerContentlets { contentlets { title } } } } }"
        }
        """
      When method post
      Then status 200
      * print response
      Examples:
        | scenario                                                                                           | expected result                                 |
        | We simply create a new Piece Content which we will use to work on. No publishDate is provided here | We should succeed creating the piece of content |
