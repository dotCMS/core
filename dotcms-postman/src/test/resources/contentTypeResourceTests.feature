Feature: ContentType Resource API Tests

  Background:
    * url 'http://localhost:8080'
    * configure headers = { 'Content-Type': 'application/json', 'Authorization': 'Basic YWRtaW5AZG90Y21zLmNvbTphZG1pbg==' }
    * def now = function(){ return java.lang.System.currentTimeMillis() }
    * def name = 't' + now()

  Scenario: Create, get, update, delete ContentType
    Given path '/api/v1/contenttype'
    And request
      """
      {
        "clazz": "com.dotcms.contenttype.model.type.SimpleContentType",
        "description": "My Structure",
        "defaultType": false,
        "system": false,
        "folder": "SYSTEM_FOLDER",
        "name": "My Custom Structure",
        "variable": "#(name)",
        "host": "SYSTEM_HOST",
        "fixed": false,
        "icon": "testIcon",
        "sortOrder": 3,
        "fields": [
          {
            "clazz": "com.dotcms.contenttype.model.field.TextField",
            "indexed": true,
            "dataType": "TEXT",
            "readOnly": false,
            "required": true,
            "searchable": true,
            "listed": true,
            "sortOrder": 2,
            "unique": false,
            "name": "Name",
            "variable": "name",
            "fixed": true
          }
        ],
        "workflow": ["d61a59e1-a49c-46f2-a929-db2b4bfa88b2"]
      }
      """
    When method post
    Then status 200
    And match response.entity[0].icon == 'testIcon'
    And match response.entity[0].sortOrder == 3
    * def contentTypeID = response.entity[0].id
    * def contentTypeVariable = response.entity[0].variable
    * def contentTypeFieldID = response.entity[0].fields[0].id
    Given path 'api/v1/contenttype/id/' + contentTypeID
    When method get
    Then status 200
    And match response.entity.icon == 'testIcon'
    And match response.entity.sortOrder == 3
    Given path 'api/v1/contenttype/id/' + contentTypeID
    And request
      """
      {
        "clazz": "com.dotcms.contenttype.model.type.SimpleContentType",
        "description": "My Structure",
        "defaultType": false,
        "system": false,
        "folder": "SYSTEM_FOLDER",
        "host": "SYSTEM_HOST",
        "name": "My Custom Structure",
        "variable": "#(contentTypeVariable)",
        "fixed": false,
        "id": "#(contentTypeID)",
        "fields": [
          {
            "clazz": "com.dotcms.contenttype.model.field.ImmutableTextField",
            "contentTypeId": "#(contentTypeID)",
            "dataType": "TEXT",
            "fieldType": "Text",
            "fieldTypeLabel": "Text",
            "fieldVariables": [],
            "fixed": true,
            "iDate": 1631719532000,
            "id": "#(contentTypeFieldID)",
            "indexed": true,
            "listed": true,
            "modDate": 1631719532000,
            "name": "Name",
            "readOnly": false,
            "required": true,
            "searchable": true,
            "sortOrder": 2,
            "unique": false,
            "variable": "name"
          }
        ],
        "workflow": ["d61a59e1-a49c-46f2-a929-db2b4bfa88b2"],
        "icon": "icon2",
        "sortOrder": 2
      }
      """
    When method put
    Then status 200
    And match response.entity.icon == 'icon2'
    And match response.entity.sortOrder == 2
    Given path 'api/v1/contenttype/id/' + contentTypeID
    When method delete
    Then status 200