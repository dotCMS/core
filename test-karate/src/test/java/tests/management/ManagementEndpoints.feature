Feature: Management Endpoints Port Restriction Tests

  Background:
    * def baseUrl = karate.properties['karate.base.url'] || 'http://localhost:8080'
    * def managementUrl = karate.properties['karate.management.url'] || 'http://localhost:8090'
    * def regularPort = karate.properties['karate.regular.port'] || '8080'
    * def managementPort = karate.properties['karate.management.port'] || '8090'
    * def authString = 'admin@dotcms.com:admin'
    * def encodedAuth = function(s) { return java.util.Base64.getEncoder().encodeToString(s.getBytes('UTF-8')); }
    * def authHeader = 'Basic ' + encodedAuth(authString)
    * def commonHeaders = { 'Content-Type': 'application/json', 'Authorization': '#(authHeader)' }
    
    # Wait for application to be ready before running tests
    * call read('classpath:common/utils.feature@waitForReady') { managementUrl: '#(managementUrl)' }

  @management @security
  Scenario Outline: Management endpoints should be blocked on regular port
    Given url baseUrl + '/dotmgt/<endpoint>'
    And headers commonHeaders
    When method get
    Then status 404
    And match response contains 'Management endpoints are only available on the management port'

    Examples:
      | endpoint |
      | livez    |
      | readyz   |
      | health   |
      | info     |

  @management @health
  Scenario: Management endpoints should work correctly on management port
    # Test liveness endpoint - should return alive when ready
    Given url managementUrl + '/dotmgt/livez'
    And headers commonHeaders
    When method get
    Then status 200
    And match response == 'alive'

    # Test readiness endpoint - should return ready when ready 
    Given url managementUrl + '/dotmgt/readyz'
    And headers commonHeaders
    When method get
    Then status 200
    And match response == 'ready'

    # Test health endpoint - should return UP status when ready
    Given url managementUrl + '/dotmgt/health'
    And headers commonHeaders
    When method get
    Then status 200
    And match response.status == 'UP'

  @management @security
  Scenario: Management endpoints without authentication should still be blocked on regular port
    Given url baseUrl + '/dotmgt/livez'
    When method get
    Then status 404
    And match response contains 'Management endpoints are only available on the management port'

  @management @health
  Scenario: Management endpoints without authentication should work on management port
    Given url managementUrl + '/dotmgt/livez'
    When method get
    Then status 200
    And match response == 'alive'

  @management @security 
  Scenario: Non-existent management endpoint should return 404 with available endpoints list
    Given url managementUrl + '/dotmgt/nonexistent'
    When method get
    Then status 404
    And match response contains 'Endpoint not found'
    And match response contains 'Available management endpoints:'
    And match response contains 'livez'
    And match response contains 'readyz'

  @management @security
  Scenario Outline: Management endpoints with proxy headers should work
    Given url managementUrl + '/dotmgt/<endpoint>'
    And header X-Forwarded-Port = managementPort
    When method get
    Then status 200

    Examples:
      | endpoint |
      | livez    |
      | readyz   |

  @management @security
  Scenario: Management endpoints should reject requests with wrong proxy port header
    Given url baseUrl + '/dotmgt/livez'
    And header X-Forwarded-Port = regularPort
    When method get
    Then status 404
    And match response contains 'Management endpoints are only available on the management port'