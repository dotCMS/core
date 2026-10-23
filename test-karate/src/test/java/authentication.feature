@ignore
Feature: Bootstrap an API JWT for Karate fixtures

  # Run once per JVM from karate-config.js via karate.callSingle. It performs the supported
  # JSON session login, uses the resulting JSESSIONID only to issue a real API token, and
  # returns the Bearer header. The session cookie is cleared before returning so it can never
  # authenticate an ordinary collection request or mask an authentication-negative scenario.
  # The deprecated POST /api/v1/authentication/api-token endpoint is intentionally not used:
  # its entity.token is a browser USER_TOKEN, not an API token that ordinary APIs accept.
  Scenario: Issue an admin API JWT through JSON login then token issuance
    * def baseUrl = karate.properties['karate.base.url'] || 'http://localhost:8080'
    * def loginEmail = 'admin@dotcms.com'
    * def password = 'admin'

    # 1. Designated JSON session login. The login action accepts the email login name.
    Given url baseUrl + '/api/v1/authentication'
    And header Content-Type = 'application/json'
    And request { userId: '#(loginEmail)', password: '#(password)', backEndLogin: true }
    When method post
    Then status 200

    # 2. Issue an API token using that isolated session. entity.jwt is the API JWT.
    # POST /api/v1/apitoken resolves userId as a canonical user id, not an email login name, so
    # the email must NOT be reused here. Omitting userId issues the token for the authenticated
    # session owner (the admin account logged in above).
    * def tokenBody = { expirationSeconds: 3600 }
    * assert tokenBody.userId == null
    Given url baseUrl + '/api/v1/apitoken'
    And header Content-Type = 'application/json'
    And request tokenBody
    When method post
    Then status 200
    * def jwt = response.entity.jwt
    * assert jwt != null
    # The issued token must belong to a canonical user id, never to the email login name.
    * assert response.entity.token.userId != null
    * assert response.entity.token.userId != loginEmail
    * def authHeader = 'Bearer ' + jwt

    # Drop the bootstrap session; only the Bearer credential is carried forward.
    * configure cookies = null
