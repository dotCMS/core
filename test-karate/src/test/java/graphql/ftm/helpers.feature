Feature: Reusable Functions and Helpers

  Scenario: Define reusable functions

    ## General error free validation
    * def validateNoErrors =
      """
      function (response) {
        const errors = response.errors;
        if (errors) {
          return errors;
        }
        return [];
      }
      """

    ## Builds a payload for creating a new content version
    * def buildContentRequestPayload =
      """
      function(contentType, title, publishDate, expiresOn, identifier, urlTitle, imageId) {
        let payload = {
          "contentlets": [
            {
              "contentType": contentType,
              "title": title,
              "host":"8a7d5e23-da1e-420a-b4f0-471e7da8ea2d"
            }
          ]
        };
        if (publishDate) payload.contentlets[0].publishDate = publishDate;
        if (expiresOn) payload.contentlets[0].expiresOn = expiresOn;
        if (identifier) payload.contentlets[0].identifier = identifier;
        if (urlTitle) payload.contentlets[0].urlTitle = urlTitle;
        if (imageId) payload.contentlets[0].image = imageId;
        return payload;
      }
      """
    ## Extracts all errors from a response
    * def extractErrors =
      """
      function(response) {
        let errors = [];
        let results = response.entity.results;
        if (results && results.length > 0) {
          for (let i = 0; i < results.length; i++) {
            let result = results[i];
            // Handle both nested error messages and direct error messages
            for (let key in result) {
              if (result[key] && result[key].errorMessage) {
                errors.push(result[key].errorMessage);
              }
            }
          }
        }
        return errors;
      }
      """

    ## Extracts all contentlets from a response
    * def extractContentlets =
      """
      function(response) {
        let containers = response.entity.containers;
        let allContentlets = [];
        for (let key in containers) {
          if (containers[key].contentlets) {
            for (let contentletKey in containers[key].contentlets) {
              allContentlets = allContentlets.concat(containers[key].contentlets[contentletKey]);
            }
          }
        }
        return allContentlets;
      }
      """

    ## Generates a random suffix for test data
    * def testSuffix =
      """
      function() {
        if (!karate.get('testSuffix')) {
          let prefix = '__' + Math.floor(Math.random() * 100000);
          karate.set('testSuffix', prefix);
        }
        return karate.get('testSuffix');
      }
      """

    ## Extracts a specific object from a JSON array by UUID
    * def getContentletByUUID =
      """
      function(jsonArray, uuid) {
        for (let i = 0; i < jsonArray.length; i++) {
          let keys = Object.keys(jsonArray[i]);
          if (keys.includes(uuid)) {
            return jsonArray[i][uuid];
          }
        }
        return null; // Return null if not found
      }
      """

    ## Builds a payload for creating a new GraphQL request
    * def buildGraphQLRequestPayload =
      """
      function(pageUri, publishDate) {
        if (!pageUri.startsWith('/')) {
          pageUri = '/' + pageUri;
        }
        var query = 'query Page { page(url: "' + pageUri + '"';
        if (publishDate) {
          query += ' publishDate: "' + publishDate + '"';
        }
        query += ') { containers { containerContentlets { contentlets { title } } } } }';
        return { query: query };
      }
      """

    ## Extracts all contentlet titles from a GraphQL response
    * def contentletsFromGraphQlResponse =
    """
    function(response) {
      let containers = response.data.page.containers;
      let allTitles = [];
      containers.forEach(container => {
        container.containerContentlets.forEach(cc => {
          cc.contentlets.forEach(contentlet => {
            allTitles.push(contentlet.title);
          });
        });
      });
      return allTitles;
    }
    """
    ##