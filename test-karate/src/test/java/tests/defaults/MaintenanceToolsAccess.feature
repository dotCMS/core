Feature: Maintenance tool routes, scripts and tabs

  # The cache, Redis, JVM information, session and thread routes of the Maintenance tool answer only CMS
  # Administrators. A user who holds the Maintenance portlet without being one, a back-end user, a front-end user and
  # an anonymous caller get 401. The two DWR scripts the Threads and Logged Users tabs used are no longer served, the
  # JVM information hides configuration values, the cache listing leaves out company keys and user passwords, and the
  # Maintenance page renders the System Info, Threads and Logged Users tabs only for a CMS Administrator.

  Background:
    * def fixture = callonce read('classpath:dependencyfeatures/maintenanceToolsAccessSetup.feature')
    # Removes the users, the role and the layout once the feature ends (parallel runner); the last scenario does it too
    * configure afterFeature = function() { karate.call('classpath:dependencyfeatures/maintenanceToolsAccessCleanup.feature@fixture', fixture) }
    * configure cookies = null
    * def encodedAuth = function(s) { return java.util.Base64.getEncoder().encodeToString(s.getBytes('UTF-8')); }
    * def basicHeaders = function(email) { return { 'Content-Type': 'application/json', 'Authorization': 'Basic ' + encodedAuth(email + ':' + fixture.password) } }
    * def callerHeaders = { anonymous: { 'Content-Type': 'application/json' }, frontend: '#(basicHeaders(fixture.frontendEmail))', backend: '#(basicHeaders(fixture.backendEmail))', maintenance: '#(basicHeaders(fixture.maintenanceEmail))' }
    * def routes =
      """
      [
        { method: 'get', path: '/api/v1/caches' },
        { method: 'get', path: '/api/v1/caches/provider/Caffeine%20Cache%20Provider/objects/userdotcmscache' },
        { method: 'delete', path: '/api/v1/caches/menucache' },
        { method: 'get', path: '/api/v1/redis/ping' },
        { method: 'get', path: '/api/v1/redis/sample-key' },
        { method: 'delete', path: '/api/v1/redis/sample-key' },
        { method: 'get', path: '/api/v1/jvm' },
        { method: 'get', path: '/api/v1/maintenance/_sessions' },
        { method: 'get', path: '/api/v1/maintenance/_threads?hideSystem=true' }
      ]
      """
    * def requestsFor = function(headers) { return karate.map(routes, function(route) { return { url: baseUrl + route.path, method: route.method, headers: headers } }) }
    * def pageUrl = function(url) { return (url.indexOf('http') == 0 ? url : baseUrl + url) + '&in_frame=true&frame=detailFrame&container=true&angularCurrentPortlet=maintenance' }
    * def plainCall =
      """
      function(scriptName, methodName, sessionId) {
        return ['callCount=1', 'windowName=', 'page=/dotAdmin/', 'httpSessionId=' + sessionId,
                'scriptSessionId=maintenance', 'instanceId=maintenance', 'c0-scriptName=' + scriptName,
                'c0-methodName=' + methodName, 'c0-id=0', 'batchId=0'].join('\n');
      }
      """

  @permissions @negative
  Scenario Outline: A <caller> caller gets 401 from every Maintenance tool route
    * def results = call read('classpath:common/utils.feature@makeRequest') requestsFor(callerHeaders['<caller>'])
    * def statuses = karate.map(results, function(call) { return call.responseStatus })
    * match each statuses == 401

    Examples:
      | caller      |
      | anonymous   |
      | frontend    |
      | backend     |
      | maintenance |

  @permissions @negative
  Scenario: A user who holds the Maintenance portlet without being a CMS Administrator is told why
    * def results = call read('classpath:common/utils.feature@makeRequest') requestsFor(callerHeaders.maintenance)
    * def bodies = karate.map(results, function(call) { return karate.toString(call.response) })
    * match each bodies contains 'is not a CMS Administrator'

  @permissions @positive
  Scenario: A CMS Administrator reads the Maintenance tool routes
    # The Redis key read and the cache object listing are checked with their values in the scenarios below
    * def reads = karate.filter(requestsFor(commonHeaders), function(r) { return r.method == 'get' && r.url.indexOf('/redis/sample-key') < 0 && r.url.indexOf('/objects/') < 0 })
    * def results = call read('classpath:common/utils.feature@makeRequest') reads
    * def statuses = karate.map(results, function(call) { return call.responseStatus })
    * match each statuses == 200

  @positive
  Scenario: The DWR scripts of the Threads and Logged Users tabs are no longer served
    Given url baseUrl + '/api/v1/authentication'
    And header Content-Type = 'application/json'
    And request { userId: 'admin@dotcms.com', password: 'admin' }
    When method post
    Then status 200
    * def sessionId = responseCookies.JSESSIONID.value

    Given url baseUrl + '/dwr/call/plaincall/ThreadMonitorTool.getSysProps.dwr'
    And header Content-Type = 'text/plain'
    And header Referer = baseUrl + '/dotAdmin/'
    And request plainCall('ThreadMonitorTool', 'getSysProps', sessionId)
    When method post
    Then status 200
    And match response contains 'java.lang.SecurityException'
    And match response !contains 'handleCallback'

    Given url baseUrl + '/dwr/call/plaincall/UserSessionAjax.getSessionList.dwr'
    And header Content-Type = 'text/plain'
    And header Referer = baseUrl + '/dotAdmin/'
    And request plainCall('UserSessionAjax', 'getSessionList', sessionId)
    When method post
    Then status 200
    And match response contains 'java.lang.SecurityException'
    And match response !contains 'handleCallback'

    Given url baseUrl + '/dwr/interface/ThreadMonitorTool.js'
    And header Referer = baseUrl + '/dotAdmin/'
    When method get
    Then status 501

    Given url baseUrl + '/dwr/interface/UserSessionAjax.js'
    And header Referer = baseUrl + '/dotAdmin/'
    When method get
    Then status 501

    # A Maintenance script that is still registered is served
    Given url baseUrl + '/dwr/interface/CMSMaintenanceAjax.js'
    And header Referer = baseUrl + '/dotAdmin/'
    When method get
    Then status 200

  @positive
  Scenario: The JVM information hides values by name and the user part of URLs
    * def passwordKey = 'SAMPLE_' + fixture.suffix + '_PWD'
    * def urlKey = 'SAMPLE_' + fixture.suffix + '_DB_URL'
    * def storedPassword = 'value-' + fixture.suffix
    * def storedUrlPassword = 'url-' + fixture.suffix
    # Removes both overrides when the scenario ends, also after a failed step
    * configure afterScenario = function() { karate.call('classpath:dependencyfeatures/maintenanceToolsAccessCleanup.feature@overrides', { passwordKey: passwordKey, urlKey: urlKey }) }

    Given url baseUrl + '/api/v1/system-table/'
    And headers commonHeaders
    And request { key: '#(passwordKey)', value: '#(storedPassword)' }
    When method post
    Then status 200

    Given url baseUrl + '/api/v1/system-table/'
    And headers commonHeaders
    And request { key: '#(urlKey)', value: '#("jdbc:postgresql://dbuser:" + storedUrlPassword + "@db:5432/dotcms")' }
    When method post
    Then status 200

    Given url baseUrl + '/api/v1/jvm'
    And headers commonHeaders
    When method get
    Then status 200
    * def body = karate.toString(response)
    * def shownPassword = response.configOverrides[passwordKey]
    * def shownUrl = response.configOverrides[urlKey]
    * def shownEnvironmentPassword = response.environment.JVM_ENDPOINT_TEST_PASS
    * match shownPassword == '*****'
    * match shownUrl == 'jdbc:postgresql://*****@db:5432/dotcms'
    * match shownEnvironmentPassword == '*****'
    * assert body.indexOf(storedPassword) < 0
    * assert body.indexOf(storedUrlPassword) < 0

  @positive
  Scenario: The cache object listing leaves out company keys and user passwords
    # A login reads the company, so the company cache holds it
    Given url baseUrl + '/api/v1/authentication'
    And header Content-Type = 'application/json'
    And request { userId: 'admin@dotcms.com', password: 'admin' }
    When method post
    Then status 200
    * configure cookies = null

    Given url baseUrl
    And path 'api/v1/caches/provider', 'Caffeine Cache Provider', 'objects', 'companypool'
    And headers commonHeaders
    When method get
    Then status 200
    * def companies = karate.filter(karate.valuesOf(response.entity), function(v) { return v != null && typeof v == 'object' })
    * match companies != []
    * match each companies contains { companyId: '#string' }
    * def companyKeys = karate.jsonPath(companies, '$[*].key')
    * match companyKeys == []
    * def companyId = companies[0].companyId

    # The single-object route returns the same company without its key
    Given url baseUrl
    And path 'api/v1/caches/provider', 'Caffeine Cache Provider', 'object', 'companypool', companyId
    And headers commonHeaders
    When method get
    Then status 200
    * def company = response.entity
    * match company contains { companyId: '#(companyId)' }
    * match company !contains { key: '#present' }

    Given url baseUrl
    And path 'api/v1/caches/provider', 'Caffeine Cache Provider', 'objects', 'userdotcmscache'
    And headers commonHeaders
    When method get
    Then status 200
    * def users = karate.filter(karate.valuesOf(response.entity), function(v) { return v != null && typeof v == 'object' })
    * match users != []
    * def userPasswords = karate.jsonPath(users, '$[*].password')
    * match userPasswords == []

  @permissions
  Scenario: The Maintenance page renders the System Info, Threads and Logged Users tabs only for a CMS Administrator
    # As admin, through a session as the back-end UI does
    Given url baseUrl + '/api/v1/authentication'
    And header Content-Type = 'application/json'
    And request { userId: 'admin@dotcms.com', password: 'admin' }
    When method post
    Then status 200

    Given url baseUrl + '/api/v1/menu'
    When method get
    Then status 200
    * def adminPage = karate.jsonPath(response, "$.entity[*].menuItems[?(@.id=='maintenance')].url")[0]

    Given url pageUrl(adminPage)
    And header Referer = baseUrl + '/dotAdmin/'
    When method get
    Then status 200
    And match response contains 'id="cache"'
    And match response contains 'id="systemProps"'
    And match response contains 'id="threads"'
    And match response contains 'id="loggedusers"'

    # As the user who holds only the Maintenance portlet
    * configure cookies = null
    Given url baseUrl + '/api/v1/authentication'
    And header Content-Type = 'application/json'
    And request { userId: '#(fixture.maintenanceEmail)', password: '#(fixture.password)' }
    When method post
    Then status 200

    Given url baseUrl + '/api/v1/menu'
    When method get
    Then status 200
    * def maintenancePage = karate.jsonPath(response, "$.entity[*].menuItems[?(@.id=='maintenance')].url")[0]

    Given url pageUrl(maintenancePage)
    And header Referer = baseUrl + '/dotAdmin/'
    When method get
    Then status 200
    And match response contains 'id="cache"'
    And match response !contains 'id="systemProps"'
    And match response !contains 'id="threads"'
    And match response !contains 'id="loggedusers"'

  @permissions
  Scenario: Remove the users, the role and the layout
    # The afterFeature hook does the same when the feature runs through the parallel runner, as in CI; a second
    # pass only finds nothing to delete
    * call read('classpath:dependencyfeatures/maintenanceToolsAccessCleanup.feature@fixture') fixture
