Feature: Search tool pages output encoding

  Background:
    * def authUser = 'admin@dotcms.com'
    * def authPassword = 'admin'
    * def loginBody = { "userId": "#(authUser)", "password": "#(authPassword)" }
    * def uniqueSuffix = '' + java.lang.System.currentTimeMillis()
    # Markup, an entity and non-ASCII characters in the key/value "key", which becomes the contentlet title
    * def titleValue = '<b id="t1">Q&A ' + uniqueSuffix + ' é中</b>'
    * def searchBody = function(identifier) { return JSON.stringify({ query: { query_string: { query: '+identifier:' + identifier } }, aggs: { titles: { terms: { field: 'title_dotraw' } } } }); }

  Scenario Outline: <portlet> renders a title and aggregation key as text
    Given url baseUrl + '/api/v1/workflow/actions/default/fire/PUBLISH?indexPolicy=WAIT_FOR'
    And headers commonHeaders
    And request { "contentlet": { "stInode": "f4d7c1b8-2c88-4071-abf1-a5328977b07d", "languageId": 1, "key": "#(titleValue)", "value": "v" } }
    When method put
    Then status 200
    * def identifier = response.entity.identifier

    # the render route needs a portal session, not Basic auth
    Given url baseUrl + '/api/v1/authentication'
    And request loginBody
    When method post
    Then status 200

    Given url baseUrl + '/api/es/layout/<portlet>/render'
    And param query = searchBody(identifier)
    And retry until responseStatus == 200 && response.indexOf('<td>' + identifier + '</td>') > -1
    When method get
    Then status 200
    # the title is not written as markup in the title row, the aggregation list or the response block (JSON-encoded quotes there)
    And match response !contains '<b id="t1">'
    And match response !contains '<b id=\\"t1\\"'
    # the title row, the aggregation row (the engine lower-cases the key) and the response block show the markup as text
    And match response contains '&lt;b id=&#34;t1&#34;&gt;Q&amp;A ' + uniqueSuffix
    And match response contains '. &lt;b id=&#34;t1&#34;&gt;q&amp;a ' + uniqueSuffix
    And match response contains '&lt;b id=\\&#34;t1\\&#34;&gt;'
    # non-ASCII characters are kept
    And match response contains 'é中'

    Examples:
      | portlet          |
      | es-search-legacy |
      | es-search        |

  Scenario Outline: <portlet> prints the submitted query as text
    Given url baseUrl + '/api/v1/authentication'
    And request loginBody
    When method post
    Then status 200

    # a valid query whose text contains markup characters
    * def commentQuery = JSON.stringify({ query: { match: { title: '--><b id="q2">' } } })
    Given url baseUrl + '/api/es/layout/<portlet>/render'
    And param query = commentQuery
    When method get
    Then status 200
    And match response !contains '<b id="q2">'
    And match response !contains '<b id=\\"q2\\"'
    # the query keeps the backslashes of its JSON quotes
    And match response contains '&lt;b id=\\&#34;q2\\&#34;&gt;'

    Examples:
      | portlet          |
      | es-search-legacy |
      | es-search        |

  Scenario Outline: <portlet> shows an engine error message as text
    Given url baseUrl + '/api/v1/authentication'
    And request loginBody
    When method post
    Then status 200

    # the engine repeats the unknown query name in its error message
    * def errorQuery = JSON.stringify({ query: { '<i id="n1">': {} } })
    Given url baseUrl + '/api/es/layout/<portlet>/render'
    And param query = errorQuery
    When method get
    Then status 200
    And match response !contains '<i id="n1">'
    And match response contains 'unknown query [&lt;i id=&#34;n1&#34;&gt;]'

    Examples:
      | portlet          |
      | es-search-legacy |
      | es-search        |

  # guard, not part of the encoding change: the page is not served without a session
  Scenario: the result page is not served without a session
    Given url baseUrl + '/api/es/layout/es-search-legacy/render'
    And param query = '{"query":{"match_all":{}}}'
    When method get
    Then assert responseStatus == 401 || responseStatus == 403

  Scenario: the query tool page keeps paging and sort values inside their fields
    Given url baseUrl + '/api/v1/authentication'
    And request loginBody
    When method post
    Then status 200

    # the layout id of the Dev Tools menu, taken from one of its portlet urls
    Given url baseUrl + '/api/v1/menu'
    When method get
    Then status 200
    * def devTools = karate.filter(response.entity, function(m){ return m.name == 'Dev Tools' })[0]
    * def layoutUrl = karate.jsonPath(devTools, "$.menuItems[?(@.id=='graphql')].url")[0]
    * assert layoutUrl != null
    * def layoutId = layoutUrl.match(/p_l_id=([^&]+)/)[1]

    # values with markup characters stay inside their fields; a value that is not a number falls back to the default
    Given url baseUrl + '/c/portal/layout'
    And header Referer = baseUrl + '/dotAdmin/'
    And param p_l_id = layoutId
    And param p_p_id = 'query-tool-legacy'
    And param offset = '0" data-probe="1'
    And param limit = '20" data-probe="2'
    And param sort = 'title" data-probe="3'
    When method get
    Then status 200
    And match response !contains 'data-probe="'
    * assert /name="offset"[^>]*value="0"/.test(response)
    * assert /name="limit"[^>]*value="20"/.test(response)

    # legitimate values pass through unchanged
    Given url baseUrl + '/c/portal/layout'
    And header Referer = baseUrl + '/dotAdmin/'
    And param p_l_id = layoutId
    And param p_p_id = 'query-tool-legacy'
    And param offset = '40'
    And param limit = '10'
    And param sort = 'title desc'
    When method get
    Then status 200
    * assert /name="offset"[^>]*value="40"/.test(response)
    * assert /name="limit"[^>]*value="10"/.test(response)
    * assert /name="sort"[^>]*value="title desc"/.test(response)
