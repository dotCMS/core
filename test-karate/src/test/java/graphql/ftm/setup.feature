Feature: Setting up the Future Time Machine Test

  Background:
    * callonce read('classpath:graphql/ftm/helpers.feature')
    # Make the prefix available to the scenario
    # Setup required data
    # Lets start by creating a new content type, container, template and publish the template
    # First the Content Type
    * def contentTypeResult = callonce read('classpath:graphql/ftm/newContentType.feature')
    * def contentTypeId = contentTypeResult.response.entity[0].id
    * def contentTypeVariable = contentTypeResult.response.entity[0].variable
    # Now the container, template and publish the template
    * def containerResult = callonce read('classpath:graphql/ftm/newContainer.feature') { contentTypeId: '#(contentTypeId)' }
    * def containerId = containerResult.response.entity.identifier
    * def templateResult = callonce read('classpath:graphql/ftm/newTemplate.feature') { containerId: '#(containerId)' }
    * def templateId = templateResult.response.entity.identifier
    * callonce read('classpath:graphql/ftm/publishTemplate.feature') { templateId: '#(templateId)' }

    # Create a couple of new pieces of content
    * def createContentPieceOneResult = callonce read('classpath:graphql/ftm/newContent.feature') { contentTypeId: '#(contentTypeId)', title: 'test 1' }
    * def contentPieceOne = createContentPieceOneResult.response.entity.results
    * def contentPieceOneId = contentPieceOne.map(result => Object.keys(result)[0])
    * def contentPieceOneId = contentPieceOneId[0]

    * def createContentPieceTwoResult = callonce read('classpath:graphql/ftm/newContent.feature') { contentTypeId: '#(contentTypeId)', title: 'test 2' }
    * def contentPieceTwo = createContentPieceTwoResult.response.entity.results
    * def contentPieceTwoId = contentPieceTwo.map(result => Object.keys(result)[0])
    * def contentPieceTwoId = contentPieceTwoId[0]

    # Now lets create a new version for each piece of content
    * def formatter = java.time.format.DateTimeFormatter.ofPattern('yyyy-MM-dd')
    * def now = java.time.LocalDateTime.now()
    * def futureDateTime = now.plusDays(10)
    * def formattedFutureDateTime = futureDateTime.format(formatter)

    * def newContentPiceOneVersion2 = callonce read('classpath:graphql/ftm/newContentVersion.feature') {  contentTypeId: '#(contentTypeId)', identifier: '#(contentPieceOneId)', title: 'test 1 v2 (This ver will be publshed in the future)', publishDate: '#(formattedFutureDateTime)' }
    * def newContentPiceTwoVersion2 = callonce read('classpath:graphql/ftm/newContentVersion.feature') {  contentTypeId: '#(contentTypeId)', identifier: '#(contentPieceTwoId)', title: 'test 2 v2' }

    # Lets create a new non-published piece of content with a publish date in the future
    * def nonPublishedPieceResult = callonce read('classpath:graphql/ftm/newContent.feature') { contentTypeId: '#(contentTypeId)', title: 'Working version Only! with publish date', publishDate: '#(formattedFutureDateTime)', action: 'NEW' }
    * def nonPublishedPiece = nonPublishedPieceResult.response.entity.results
    * def nonPublishedPieceId = nonPublishedPiece.map(result => Object.keys(result)[0])
    * def nonPublishedPieceId = nonPublishedPieceId[0]

    * def suffix = Math.floor(Math.random() * 10000)
    * def pageUrl = 'ftm-test-page' + suffix

    # Finally lets create a new page
    * def createPageResult = callonce read('classpath:graphql/ftm/newPage.feature') { pageUrl:'#(pageUrl)' ,title: 'Future Time Machine Test page', templateId:'#(templateId)' }

    * def pages = createPageResult.response.entity.results
    * def pageId = pages.map(result => Object.keys(result)[0])
    * def pageId = pageId[0]

    # Now lets add the pieces of content to the page
    * def publishPageResult = callonce read('classpath:graphql/ftm/publishPage.feature') { page_id: '#(pageId)', content_ids: ['#(contentPieceOneId)', '#(contentPieceTwoId)', '#(nonPublishedPieceId)'], container_id: '#(containerId)' }

    * karate.log('Page created and Published ::', pageUrl)

  Scenario: