Feature: Test Time Machine functionality

  Background:
    # Setup required data
    * def contentTypeResult = callonce read('classpath:tests/graphql/ftm/setup/newContentType.feature')
    * def contentTypeId = contentTypeResult.response.entity[0].id
    * def contentTypeVariable = contentTypeResult.response.entity[0].variable
    * def containerResult = callonce read('classpath:tests/graphql/ftm/setup/newContainer.feature') { contentTypeId: '#(contentTypeId)' }
    * def containerId = containerResult.response.entity.identifier
    * def templateResult = callonce read('classpath:tests/graphql/ftm/setup/newTemplate.feature') { containerId: '#(containerId)' }
    * def templateId = templateResult.response.entity.identifier
    * callonce read('classpath:tests/graphql/ftm/setup/publishTemplate.feature') { templateId: '#(templateId)' }
    * def createContentPieceOneResult = callonce read('classpath:tests/graphql/ftm/setup/newContent.feature') { contentTypeId: '#(contentTypeId)', title: 'test 1' }
    * def contentPieceOneId = createContentPieceOneResult.response.entity.results[0].identifier
    * def createContentPieceTwoResult = callonce read('classpath:tests/graphql/ftm/setup/newContent.feature') { contentTypeId: '#(contentTypeId)', title: 'test 2' }
    * def contentPieceTwoId = createContentPieceTwoResult.response.entity.results[0].identifier

    * def newContentPiceOneVrsion2 = callonce read('classpath:tests/graphql/ftm/setup/newContentVersion.feature') {  contentTypeId: '#(contentTypeId)', identifier: '#(contentPieceOneId)', title: 'test 1 v2' }
    * def newContentPiceTwoVrsion2 = callonce read('classpath:tests/graphql/ftm/setup/newContentVersion.feature') {  contentTypeId: '#(contentTypeId)', identifier: '#(contentPieceTwoId)', title: 'test 2 v2' }

    * def createPageResult = callonce read('classpath:tests/graphql/ftm/setup/newPage.feature') { title: 'test page' }
  Scenario: