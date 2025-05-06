Feature: Setting up the Future Time Machine Test

  Background:
    * callonce read('classpath:graphql/ftm/helpers.feature')
    # Make the prefix available to the scenario
    # Setup required data
    * callonce read('classpath:graphql/ftm/deleteFolder.feature') { path: '/application/containers/banner' }
    * def folderIdentifierResult = callonce read('classpath:graphql/ftm/newFolder.feature') { path: '/application/containers/banner' }
    * def folderIdentifier = folderIdentifierResult.response.entity[0].identifier
    * karate.log('Folder Identifier ::', folderIdentifier)

    * callonce read('classpath:graphql/ftm/newVTL.feature') { fileName: 'banner.vtl', folderidentifier: '#(folderIdentifier)' }

    * def imageOneResult = callonce read('classpath:graphql/ftm/newImage.feature') { fileName: 'draft.png' }
    * def imageOneId = imageOneResult.response.entity.identifier
    * karate.log('Image One Identifier ::', imageOneId)

    * def imageTwoResult = callonce read('classpath:graphql/ftm/newImage.feature') { fileName: 'java-image.png' }
    * def imageTwoId = imageTwoResult.response.entity.identifier
    * karate.log('Image Two Identifier ::', imageTwoId)

    # Lets start by creating a new content type, container, template and publish the template
    # First the Content Type
    * def contentTypeResult = callonce read('classpath:graphql/ftm/newContentType.feature')
    * def contentTypeId = contentTypeResult.response.entity[0].id
    * def contentTypeVariable = contentTypeResult.response.entity[0].variable

    * def bannerContentTypeResult = callonce read('classpath:graphql/ftm/newBannerContentType.feature')
    * def bannerContentTypeId = bannerContentTypeResult.response.entity[0].id

    # Now the container, template and publish the template
    * def containerResult = callonce read('classpath:graphql/ftm/newContainer.feature') { contentTypeId: '#(contentTypeId)', bannerContentTypeId: '#(bannerContentTypeId)' }
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

    * def createContentPieceThreeResult = callonce read('classpath:graphql/ftm/newContent.feature') { contentTypeId: '#(contentTypeId)', title: 'test 3' }
    * def contentPieceThree = createContentPieceThreeResult.response.entity.results
    * def contentPieceThreeId = contentPieceThree.map(result => Object.keys(result)[0])
    * def contentPieceThreeId = contentPieceThreeId[0]

    * def createBannerContentPieceOneResult = callonce read('classpath:graphql/ftm/newContent.feature') { contentTypeId: '#(bannerContentTypeId)', title: 'banner 1', imageId: '#(imageOneId)' }
    * def bannerContentPieceOne = createBannerContentPieceOneResult.response.entity.results
    * def bannerContentPieceOneId = bannerContentPieceOne.map(result => Object.keys(result)[0])
    * def bannerContentPieceOneId = bannerContentPieceOneId[0]

    # Now lets create a new version for each piece of content
    * def formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    * def now = java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC)
    * def formattedCurrentDateTime = now.format(formatter)
    * def futureDateTime = now.plusDays(10)
    * def formattedFutureDateTime = futureDateTime.format(formatter)
    * def futureDateTimeInGraceWindow = now.plusMinutes(4)
    * def formattedFutureDateTimeInGraceWindow = futureDateTimeInGraceWindow.format(formatter)
    * karate.log('formattedFutureDateTimeInGraceWindow:', formattedFutureDateTimeInGraceWindow)

    * def newContentPiceOneVersion2 = callonce read('classpath:graphql/ftm/newContentVersion.feature') {  contentTypeId: '#(contentTypeId)', identifier: '#(contentPieceOneId)', title: 'test 1 v2 (This ver will be publshed in the future)', publishDate: '#(formattedFutureDateTime)' }
    * def newContentPiceTwoVersion2 = callonce read('classpath:graphql/ftm/newContentVersion.feature') {  contentTypeId: '#(contentTypeId)', identifier: '#(contentPieceTwoId)', title: 'test 2 v2' }
    * def newContentPiceThreeVersion2 = callonce read('classpath:graphql/ftm/newContentVersion.feature') {  contentTypeId: '#(contentTypeId)', identifier: '#(contentPieceThreeId)', title: 'test 3 v2', publishDate: '#(formattedFutureDateTimeInGraceWindow)' }
    * def newContentBannerOneVersion2 = callonce read('classpath:graphql/ftm/newContentVersion.feature') {  contentTypeId: '#(bannerContentTypeId)', identifier: '#(bannerContentPieceOneId)', title: 'banner 1 v2', publishDate: '#(formattedFutureDateTime)', imageId: '#(imageTwoId)' }

    # Lets create a new non-published piece of content wiht a publish date in the future
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
    * def publishPageResult = callonce read('classpath:graphql/ftm/publishPage.feature') { page_id: '#(pageId)', banner_content_ids: ['#(bannerContentPieceOneId)'], content_ids: ['#(contentPieceOneId)', '#(contentPieceTwoId)', '#(contentPieceThreeId)', '#(nonPublishedPieceId)'], container_id: '#(containerId)' }

    * karate.log('Page created and Published ::', pageUrl)

    # Create a Content Type with UrlMap
    * def urlMapContentTypeResult = callonce read('classpath:graphql/ftm/newUrlMapContentType.feature') {detailPageId: '#(pageId)'}
    * def urlMapContentTypeId = urlMapContentTypeResult.response.entity[0].id

    # Create a couple of new pieces of content for the UrlMap Content Type
    * def createUrlMapContentPieceOneResult = callonce read('classpath:graphql/ftm/newContent.feature') { contentTypeId: '#(urlMapContentTypeId)', title: 'url-content-map-test-1-v1', urlTitle: 'url-content-map-test-1' }
    * def urlMapContentPieceOne = createUrlMapContentPieceOneResult.response.entity.results[0]
    * def urlMapContentPieceOneId = karate.keysOf(urlMapContentPieceOne)[0]
    * def urlMapContentPieceOneUrl = urlMapContentPieceOne[urlMapContentPieceOneId].urlMap

    # Create new version of the content for the UrlMap Content Type with future publishDate
    * def newUrlMapContentPiceOneVersion2 = callonce read('classpath:graphql/ftm/newContentVersion.feature') {  contentTypeId: '#(urlMapContentTypeId)', identifier: '#(urlMapContentPieceOneId)', title: 'url-content-map-test-1-v2', publishDate: '#(formattedFutureDateTime)' }

    # Create a separate piece of content for the UrlMap Content Type. Will use for testing 404 upon unpublishing
    * def createUrlMapContentPieceTwoResult = callonce read('classpath:graphql/ftm/newContent.feature') { contentTypeId: '#(urlMapContentTypeId)', title: 'url-content-map-piece', urlTitle: 'url-content-map-piece-two' }
    * def urlMapContentPieceTwo = createUrlMapContentPieceTwoResult.response.entity.results[0]
    * def urlMapContentPieceTwoId = karate.keysOf(urlMapContentPieceTwo)[0]
    # This piece is unpublished in the test and will be used to test the 404 error
    * def urlUnpublishedContentMap = urlMapContentPieceTwo[urlMapContentPieceTwoId].urlMap

    * def unpublishResult = callonce read('classpath:graphql/ftm/unpublish.feature') { contentlet_id: '#(urlMapContentPieceTwoId)', contentlet_inode: '#(urlMapContentPieceTwo.inode)' }
    * karate.log('Unpublish Result ::', unpublishResult)


  Scenario: