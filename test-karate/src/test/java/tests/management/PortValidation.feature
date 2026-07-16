Feature: Management Port Validation and Security Tests

  Background:
    * def baseUrl = karate.properties['karate.base.url'] || 'http://localhost:8080'
    * def managementUrl = karate.properties['karate.management.url'] || 'http://localhost:8090'
    * def regularPort = karate.properties['karate.regular.port'] || '8080'
    * def managementPort = karate.properties['karate.management.port'] || '8090'

    # Wait for application to be ready before running tests
    * call read('classpath:common/utils.feature@waitForReady') { managementUrl: '#(managementUrl)' }

  @management @security @critical
  Scenario: Verify management port security - blocked on regular port
    # Test liveness endpoint blocked on regular port
    Given url baseUrl + '/dotmgt/livez'
    When method get
    Then status 404
    And match response contains 'Management endpoints are only available on the management port'

  @management @health @integration
  Scenario: Verify management endpoints work correctly on management port
    # Test liveness endpoint
    Given url managementUrl + '/dotmgt/livez'
    When method get
    Then status 200,503
    And assert response == 'alive' || response == 'unhealthy'

    # Test readiness endpoint  
    Given url managementUrl + '/dotmgt/readyz'
    When method get
    Then status 200,503
    And assert response == 'ready' || response == 'not ready'

  @management @health @json
  Scenario: Verify health endpoint returns JSON on management port
    Given url managementUrl + '/dotmgt/health'
    When method get
    Then status 200,503
    And match response.status == '#string'
    And assert response.status == 'UP' || response.status == 'DOWN'