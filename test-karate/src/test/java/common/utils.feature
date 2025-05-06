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