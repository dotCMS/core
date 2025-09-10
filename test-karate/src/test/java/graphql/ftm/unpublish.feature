Feature: Unpublish content using workflow action
  Background:

    * def contentlet_id = __arg.contentlet_id
    * def contentlet_inode = __arg.contentlet_inode

  Scenario: Unpublish a piece of content using workflow action
    Given url baseUrl + '/api/v1/workflow/actions/fire'
    And headers commonHeaders
    And request
      """
      {
        "actionName": "Unpublish",
        "contentlet" : {
          "identifier": "#(contentlet_id)",
          "inode": "#(contentlet_inode)"
        }
      }
      """
    When method PUT
    Then status 200
    * def errors = call validateNoErrors response
    * match errors == []