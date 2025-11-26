Feature: Validate banner images

  Scenario: Validate banner image response headers
    Given url baseUrl + '/dA/' + bannerContentPieceOneId + '/image'
    When method GET
    Then status 200
    And match responseHeaders contains { 'Content-Type': '#notnull' }
    And match responseHeaders['Content-Type'][0] contains 'image/png'
    And match responseHeaders contains { 'Content-Length': '#notnull' }
    And match responseHeaders['Content-Length'][0] == contentLength
    And match responseHeaders contains { 'Content-Disposition': '#notnull' }
    And match responseHeaders['Content-Disposition'][0] contains 'inline'
    And match responseHeaders['Content-Disposition'][0] contains 'filename="' + imageName + '"'