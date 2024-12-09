Feature: Test Time Machine functionality

  Background:
    # Setup required data
    * def contentTypeResult = callonce read('classpath:tests/graphql/ftm/setup/newContentType.feature')
    * def contentTypeId = contentTypeResult.response.entity[0].id
    * def containerResult = callonce read('classpath:tests/graphql/ftm/setup/newContainer.feature') { contentTypeId: '#(contentTypeId' }
    * def containerId = containerResult.response.entity.identifier
    * def templateResult = callonce read('classpath:tests/graphql/ftm/setup/newTemplate.feature') { containerId: '#(containerId)' }
    * def templateId = templateResult.response.entity.identifier
    * callonce read('classpath:tests/graphql/ftm/setup/publishTemplate.feature') { templateId: '#(templateId)' }
    * def contentResult = callonce read('classpath:tests/graphql/ftm/setup/newContent.feature') { contentTypeId: '#(contentTypeId)', containerId: '' }
  Scenario: