Feature: General Helpers and Functions

  Scenario: Define reusable functions for a global scope
    * def cleanUrl =
      """
      function(url) {
        if (!url) return url;

      // Split appart URL + params
        const parts = url.split('?');
        const baseUrl = parts[0];
        const queryParams = parts.length > 1 ? parts[1] : null;

      // Split apart URL + protocol
        let protocol = '';
        let restOfUrl = baseUrl;

        if (baseUrl.indexOf('http://') === 0) {
          protocol = 'http://';
          restOfUrl = baseUrl.substring(7);
        } else if (baseUrl.indexOf('https://') === 0) {
          protocol = 'https://';
          restOfUrl = baseUrl.substring(8);
        }

      // Replace // with / repeatedly
        while (restOfUrl.indexOf('//') >= 0) {
          restOfUrl = restOfUrl.replace("//", "/");
        }
      
      // Rebuild the full URL
        let cleanedUrl = protocol + restOfUrl;
        if (queryParams) {
          cleanedUrl = cleanedUrl + '?' + queryParams;
        }

        return cleanedUrl;
      }
      """

  @checkEndpointBlocked
  Scenario: Check if endpoint returns 404 with correct error message
    # Only execute when called with explicit arguments
    * if (!__arg || !__arg.url) karate.abort()
    * def requestUrl = __arg.url
    Given url requestUrl
    When method get
    Then status 404
    And match response contains 'Management endpoints are only available on the management port'

  @makeRequest
  Scenario: Make HTTP request and return response
    # Only execute when called with explicit arguments
    * if (!__arg || !__arg.url) karate.abort()
    * def requestUrl = __arg.url
    * def requestMethod = __arg.method || 'GET'
    * def requestHeaders = __arg.headers || {}

    Given url requestUrl
    And headers requestHeaders
    When method requestMethod
    * def result = { status: responseStatus, body: response }

  @waitForReady
  Scenario: Wait for application to be ready
    * def defaultManagementUrl = karate.properties['karate.management.url'] || 'http://localhost:8090'
    * def managementUrl = __arg && __arg.managementUrl ? __arg.managementUrl : defaultManagementUrl
    * def waitForReady = 
      """
      function() {
        var maxAttempts = 30; // 30 attempts = 5 minutes with 10 second intervals
        var attempt = 0;
        while (attempt < maxAttempts) {
          attempt++;
          karate.log('Checking readiness, attempt', attempt, 'of', maxAttempts);
          try {
            var callResult = karate.call('classpath:tests/management/readiness-check.feature', {
              readinessUrl: managementUrl + '/dotmgt/readyz'
            });
            karate.log('Readiness check response:', callResult.responseStatus, callResult.response);
            if (callResult.responseStatus == 200 && callResult.response == 'ready') {
              karate.log('Application is ready!');
              return true;
            }
          } catch (e) {
            karate.log('Readiness check failed:', e.message);
          }
          java.lang.Thread.sleep(10000); // Wait 10 seconds
        }
        karate.log('Application did not become ready after', maxAttempts, 'attempts');
        return false;
      }
      """
    * def isReady = waitForReady()
    * if (!isReady) karate.fail('Application failed to become ready within timeout')