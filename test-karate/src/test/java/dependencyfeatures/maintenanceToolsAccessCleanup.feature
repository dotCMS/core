Feature: Remove the data of the Maintenance tool access tests

  # Called by the afterFeature and afterScenario hooks of tests/defaults/MaintenanceToolsAccess.feature, as admin.
  # No step checks a status, so a part that is already gone or was never created does not stop the rest.

  @fixture
  Scenario: Delete the users, the role and the layout
    # Users first: deleting a user reads the role cache that deleting a role clears
    Given url baseUrl + '/api/v1/users/' + maintenanceUserId
    And headers commonHeaders
    When method delete

    Given url baseUrl + '/api/v1/users/' + backendUserId
    And headers commonHeaders
    When method delete

    Given url baseUrl + '/api/v1/users/' + frontendUserId
    And headers commonHeaders
    When method delete

    Given url baseUrl + '/api/v1/roles/layouts'
    And headers commonHeaders
    And request { roleId: '#(roleId)', layoutIds: ['#(layoutId)'] }
    When method delete

    Given url baseUrl + '/api/v1/layouts/' + layoutId
    And headers commonHeaders
    When method delete

    Given url baseUrl + '/api/v1/roles/' + roleId
    And headers commonHeaders
    When method delete

  @overrides
  Scenario: Delete the system-table overrides
    Given url baseUrl + '/api/v1/system-table/_delete'
    And headers commonHeaders
    And request { key: '#(passwordKey)' }
    When method delete

    Given url baseUrl + '/api/v1/system-table/_delete'
    And headers commonHeaders
    And request { key: '#(urlKey)' }
    When method delete
