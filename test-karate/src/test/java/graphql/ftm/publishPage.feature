Feature: Add pieces of content then Publish the Page
  Background:

   * def page_id = __arg.page_id
   * def content_ids = __arg.content_ids
   * def container_id = __arg.container_id

  Scenario: Create a new version of a piece of content
    Given url baseUrl + '/api/v1/page/'+page_id+'/content'
    And headers commonHeaders
    And request
      """
      [
        {
          "contentletsId": "#(banner_content_ids)",
          "identifier": "#(container_id)",
          "uuid": "1"
        },
        {
          "contentletsId": "#(content_ids)",
          "identifier": "#(container_id)",
          "uuid": "1"
        }
      ]
      """
    When method POST
    Then status 200
    * def errors = call validateNoErrors response
    * match errors == []