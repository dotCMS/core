Feature: Add pieces of content then Publish the Page
  Background:

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

   * def page_id = __arg.page_id
   * def content1_id = __arg.content1_id
   * def content2_id = __arg.content2_id
   * def container_id = __arg.container_id

  Scenario: Create a new version of a piece of content
    Given url baseUrl + '/api/v1/page/'+page_id+'/content'
    And headers commonHeaders
    And request
      """
      [
        {
          "contentletsId": ["#(content1_id)", "#(content2_id)"],
          "identifier": "#(container_id)",
          "uuid": "1"
        }
      ]
      """
    When method POST
    Then status 200
    * def errors = call validateNoErrors response
    * match errors == []