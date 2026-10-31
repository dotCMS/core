Feature: Users, role and layout for the Maintenance tool access tests

  # Called once by tests/defaults/MaintenanceToolsAccess.feature. As admin, it creates a role whose only layout
  # holds the Maintenance portlet, and three users: a back-end user with that role, a back-end user without it and
  # a front-end user. All names carry a random suffix. The last scenario of the caller deletes them.

  Scenario: Create the users, the role and the layout
    * def suffix = java.util.UUID.randomUUID().toString().replace('-', '').substring(0, 12)
    * def password = 'Maint-Pw-' + suffix + '-Aa1!'
    * def maintenanceEmail = 'maintenance' + suffix + '@dotcms.com'
    * def backendEmail = 'backend' + suffix + '@dotcms.com'
    * def frontendEmail = 'frontend' + suffix + '@dotcms.com'

    Given url baseUrl + '/api/v1/roles'
    And headers commonHeaders
    And request { roleName: '#("Maintenance only " + suffix)', roleKey: '#("maintenance-only-" + suffix)', canEditUsers: true, canEditPermissions: false, canEditLayouts: true, description: 'Role whose only layout holds the Maintenance portlet' }
    When method post
    Then status 200
    * def roleId = response.entity.id

    Given url baseUrl + '/api/v1/layouts'
    And headers commonHeaders
    And request { name: '#("Maintenance only " + suffix)', icon: 'settings' }
    When method post
    Then status 200
    * def layoutId = response.entity.id

    Given url baseUrl + '/api/v1/layouts/' + layoutId + '/portlets'
    And headers commonHeaders
    And request { portletIds: ['maintenance'] }
    When method put
    Then status 200

    Given url baseUrl + '/api/v1/roles/layouts'
    And headers commonHeaders
    And request { roleId: '#(roleId)', layoutIds: ['#(layoutId)'] }
    When method post
    Then status 200

    Given url baseUrl + '/api/v1/users'
    And headers commonHeaders
    And request { firstName: 'Maintenance', lastName: '#(suffix)', email: '#(maintenanceEmail)', password: '#(password)', active: true, roles: ['DOTCMS_BACK_END_USER'] }
    When method post
    Then status 200
    * def maintenanceUserId = response.entity.userID

    Given url baseUrl + '/api/v1/roles/' + roleId + '/users/' + maintenanceUserId
    And headers commonHeaders
    When method post
    Then status 200

    Given url baseUrl + '/api/v1/users'
    And headers commonHeaders
    And request { firstName: 'Backend', lastName: '#(suffix)', email: '#(backendEmail)', password: '#(password)', active: true, roles: ['DOTCMS_BACK_END_USER'] }
    When method post
    Then status 200
    * def backendUserId = response.entity.userID

    Given url baseUrl + '/api/v1/users'
    And headers commonHeaders
    And request { firstName: 'Frontend', lastName: '#(suffix)', email: '#(frontendEmail)', password: '#(password)', active: true, roles: ['DOTCMS_FRONT_END_USER'] }
    When method post
    Then status 200
    * def frontendUserId = response.entity.userID
