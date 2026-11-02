Feature: Version and node response headers are sent only to requests with credentials

  # X-DotCMS-Version and X-DotCMS-Min-SDK are added by SdkVersionWebInterceptor, and x-dot-server by
  # ResponseMetaDataWebInterceptor, both registered in InterceptorFilter. A request with no credentials, or with a
  # bearer token that does not check out, gets none of them; a request with a valid API token gets both version
  # headers. The unit tests call the interceptors directly, so this feature is what runs them through the filter.

  Background:
    * configure cookies = null
    * def lowerCaseHeaderNames = function(headers) { return karate.map(karate.keysOf(headers), function(name) { return name.toLowerCase() }) }

  @negative
  Scenario: A request with no credentials gets none of the headers
    Given url baseUrl + '/api/v1/appconfiguration'
    When method get
    Then status 200
    * def names = lowerCaseHeaderNames(responseHeaders)
    And match names !contains 'x-dotcms-version'
    And match names !contains 'x-dotcms-min-sdk'
    And match names !contains 'x-dot-server'

  @negative
  Scenario: A request with a bearer token dotCMS did not issue gets none of the headers
    Given url baseUrl + '/api/v1/appconfiguration'
    And header Authorization = 'Bearer not-a-token'
    When method get
    Then status 200
    * def names = lowerCaseHeaderNames(responseHeaders)
    And match names !contains 'x-dotcms-version'
    And match names !contains 'x-dotcms-min-sdk'
    And match names !contains 'x-dot-server'

  @positive
  Scenario: A request with a valid API token gets both version headers
    Given url baseUrl + '/api/v1/authentication/api-token'
    And header Content-Type = 'application/json'
    And request { user: 'admin@dotcms.com', password: 'admin', expirationDays: 1, label: 'response-meta-headers' }
    When method post
    Then status 200
    * def token = response.entity.token

    Given url baseUrl + '/api/v1/appconfiguration'
    And header Authorization = 'Bearer ' + token
    When method get
    Then status 200
    * def names = lowerCaseHeaderNames(responseHeaders)
    And match names contains 'x-dotcms-version'
    And match names contains 'x-dotcms-min-sdk'
