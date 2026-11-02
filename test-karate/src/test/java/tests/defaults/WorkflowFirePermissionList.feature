Feature: Workflow fire with a permission list

  # A back-end user with READ and WRITE on a contentlet, and no permission to edit its permissions,
  # fires the System Workflow Save action. Without a permission list the save succeeds; with a list
  # that differs from the contentlet's current permissions the endpoint answers 403 and the
  # contentlet's permission rows stay as they were.

  Background:
    * def suffix = java.util.UUID.randomUUID().toString().replace('-', '').substring(0, 12)
    * def saveActionId = 'ceca71a0-deee-4999-bd47-b01baa1bcfc8'
    * def systemWorkflowId = 'd61a59e1-a49c-46f2-a929-db2b4bfa88b2'
    * def encodedAuth = function(s) { return java.util.Base64.getEncoder().encodeToString(s.getBytes('UTF-8')); }
    * def asRows = function(rows) { return karate.sort(karate.map(rows, function(row) { return { roleId: row.roleId, permission: row.permission, type: row.type } }), function(row) { return row.roleId + ':' + row.permission + ':' + row.type }) }
    * def editorEmail = 'editor' + suffix + '@dotcms.com'
    * def editorPassword = 'Pw' + suffix
    * def editorHeaders = { 'Content-Type': 'application/json', 'Authorization': '#("Basic " + encodedAuth(editorEmail + ":" + editorPassword))' }

  @permissions
  Scenario: A changed permission list from a user who may not edit permissions answers 403 and leaves the rows unchanged

    # Setup, as admin: a site, a role that is not on the contentlet, and a back-end user
    Given url baseUrl + '/api/v1/site'
    And headers commonHeaders
    And request { siteName: '#("permission-list-" + suffix + ".dotcms.com")' }
    When method post
    Then status 200
    * def siteId = response.entity.identifier

    Given url baseUrl + '/api/v1/roles'
    And headers commonHeaders
    And request { roleName: '#("Extra role " + suffix)', roleKey: '#("extra-role-" + suffix)', canEditUsers: false, canEditPermissions: true, canEditLayouts: false, description: 'Role added by the permission list' }
    When method post
    Then status 200
    * def extraRoleId = response.entity.id

    Given url baseUrl + '/api/v1/users'
    And headers commonHeaders
    And request { firstName: 'Editor', lastName: '#(suffix)', email: '#(editorEmail)', password: '#(editorPassword)', active: true, roles: ['DOTCMS_BACK_END_USER'] }
    When method post
    Then status 200
    * def editorUserId = response.entity.userID
    * def editorRoleId = response.entity.roleId

    # The editor may read the site and the content types created on it
    Given url baseUrl + '/api/v1/permissions/' + siteId
    And headers commonHeaders
    And request { permissions: [ { roleId: '#(editorRoleId)', individual: ['READ'], inheritable: { CONTENT_TYPE: ['READ'] } } ] }
    When method put
    Then status 200

    Given url baseUrl + '/api/v1/contenttype'
    And headers commonHeaders
    And request
      """
      {
        "clazz": "com.dotcms.contenttype.model.type.ImmutableSimpleContentType",
        "name": "#('PermissionList' + suffix)",
        "variable": "#('PermissionList' + suffix)",
        "host": "#(siteId)",
        "workflow": ["#(systemWorkflowId)"],
        "fields": [
          {
            "clazz": "com.dotcms.contenttype.model.field.ImmutableTextField",
            "dataType": "TEXT",
            "name": "title",
            "variable": "title",
            "required": true,
            "indexed": true,
            "searchable": true
          },
          {
            "clazz": "com.dotcms.contenttype.model.field.ImmutableHostFolderField",
            "dataType": "SYSTEM",
            "name": "Site or Folder",
            "variable": "hostFolder"
          }
        ]
      }
      """
    When method post
    Then status 200
    * def contentTypeId = response.entity[0].id
    * def contentTypeVariable = response.entity[0].variable

    # Created with Save, so the contentlet stays in the step where Save is available
    Given url baseUrl + '/api/v1/workflow/actions/' + saveActionId + '/fire?indexPolicy=WAIT_FOR'
    And headers commonHeaders
    And request { contentlet: { contentType: '#(contentTypeVariable)', title: 'Permission list test', hostFolder: '#(siteId)' } }
    When method put
    Then status 200
    * def contentletId = response.entity.identifier

    # The contentlet gets its own rows: READ and WRITE for the editor
    Given url baseUrl + '/api/v1/permissions/' + contentletId
    And headers commonHeaders
    And request { permissions: [ { roleId: '#(editorRoleId)', individual: ['READ', 'WRITE'] } ] }
    When method put
    Then status 200

    Given url baseUrl + '/api/v1/permissions/_bycontent'
    And param contentletId = contentletId
    And param type = 'ALL'
    And headers commonHeaders
    When method get
    Then status 200
    * def rowsBefore = asRows(response.entity)
    And match rowsBefore[*].roleId contains editorRoleId

    # The editor may save the contentlet when no permission list is sent. Cookies are cleared on
    # every change of user so the admin session does not authenticate the editor's calls.
    * configure cookies = null
    Given url baseUrl + '/api/v1/workflow/actions/' + saveActionId + '/fire?indexPolicy=WAIT_FOR'
    And headers editorHeaders
    And request { contentlet: { identifier: '#(contentletId)', languageId: 1, title: 'Saved by the editor' } }
    When method put
    Then status 200

    # That save keeps the contentlet's permission rows
    * configure cookies = null
    Given url baseUrl + '/api/v1/permissions/_bycontent'
    And param contentletId = contentletId
    And param type = 'ALL'
    And headers commonHeaders
    When method get
    Then status 200
    And match asRows(response.entity) == rowsBefore

    # The same save with a permission list that adds a role is answered with 403
    * configure cookies = null
    Given url baseUrl + '/api/v1/workflow/actions/' + saveActionId + '/fire?indexPolicy=WAIT_FOR'
    And headers editorHeaders
    And request
      """
      {
        "contentlet": { "identifier": "#(contentletId)", "languageId": 1, "title": "Saved with a permission list" },
        "individualPermissions": { "READ": ["#(editorRoleId)", "#(extraRoleId)"], "WRITE": ["#(editorRoleId)"] }
      }
      """
    When method put
    Then status 403

    # The contentlet's permission rows are the ones it had before
    * configure cookies = null
    Given url baseUrl + '/api/v1/permissions/_bycontent'
    And param contentletId = contentletId
    And param type = 'ALL'
    And headers commonHeaders
    When method get
    Then status 200
    And match asRows(response.entity) == rowsBefore

    # Cleanup, as admin
    Given url baseUrl + '/api/v1/contenttype/id/' + contentTypeId
    And headers commonHeaders
    When method delete
    Then status 200

    Given url baseUrl + '/api/v1/users/' + editorUserId
    And headers commonHeaders
    When method delete
    Then status 200

    Given url baseUrl + '/api/v1/roles/' + extraRoleId
    And headers commonHeaders
    When method delete
    Then status 200

    Given url baseUrl + '/api/v1/site/' + siteId + '/_archive'
    And headers commonHeaders
    When method put
    Then status 200

    Given url baseUrl + '/api/v1/site/' + siteId
    And headers commonHeaders
    When method delete
    Then status 200
