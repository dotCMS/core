Feature: Relationship Fields Preservation in Content Type Layouts

  Background:
    * url baseUrl
    * headers commonHeaders
    # Generate unique identifiers for this test run
    * def parentTypeVar = 'ParentType' + Math.floor(Math.random() * 1000000)
    * def childTypeVar = 'ChildType' + Math.floor(Math.random() * 1000000)
    * def textFieldVar = 'textField' + Math.floor(Math.random() * 1000000)
    * def relationshipFieldVar = 'relationField' + Math.floor(Math.random() * 1000000)
  
  Scenario: Create parent and child content types and verify relationship field preservation
    
    # 1. Create parent content type
    Given path '/api/v1/contenttype'
    And request
    """
    {
      "clazz": "com.dotcms.contenttype.model.type.ImmutableSimpleContentType",
      "defaultType": false,
      "fixed": false,
      "folder": "SYSTEM_FOLDER",
      "host": "SYSTEM_HOST",
      "name": "Parent Content Type Test",
      "variable": "#(parentTypeVar)"
    }
    """
    When method POST
    Then status 200
    And match response.entity[0].id != null
    And match response.entity[0].variable == parentTypeVar
    
    # Store the parent content type ID
    * def parentTypeId = response.entity[0].id
    
    # 2. Create child content type
    Given path '/api/v1/contenttype'
    And request
    """
    {
      "clazz": "com.dotcms.contenttype.model.type.ImmutableSimpleContentType",
      "defaultType": false,
      "fixed": false,
      "folder": "SYSTEM_FOLDER",
      "host": "SYSTEM_HOST",
      "name": "Child Content Type Test",
      "variable": "#(childTypeVar)"
    }
    """
    When method POST
    Then status 200
    And match response.entity[0].id != null
    And match response.entity[0].variable == childTypeVar
    
    # Store the child content type ID
    * def childTypeId = response.entity[0].id
    
    # 3. Add text field to parent content type
    Given path '/api/v1/contenttype/' + parentTypeId + '/fields'
    And request
    """
    {
      "clazz": "com.dotcms.contenttype.model.field.ImmutableTextField",
      "contentTypeId": "#(parentTypeId)",
      "dataType": "TEXT",
      "name": "Test Text Field",
      "variable": "#(textFieldVar)",
      "required": false,
      "indexed": true,
      "listed": false,
      "searchable": true,
      "unique": false
    }
    """
    When method POST
    Then status 200
    And match response.entity.id != null
    
    # Store the text field ID
    * def textFieldId = response.entity.id
    
    # 4. Add relationship field to parent content type and store ID
    Given path '/api/v1/contenttype/' + parentTypeId + '/fields'
    And request
    """
    {
      "clazz": "com.dotcms.contenttype.model.field.ImmutableRelationshipField",
      "contentTypeId": "#(parentTypeId)",
      "name": "Test Relationship",
      "variable": "relationField",
      "required": false,
      "indexed": true,
      "listed": false,
      "searchable": true,
      "unique": false,
      "relationType": "#(parentTypeVar)-#(childTypeVar)",
      "values": "3"
    }
    """
    # Change the relationType to use the actual variables
    * def relationType = parentTypeVar + '-' + childTypeVar
    # Re-build the request with the actual relationType
    * def relationRequest = 
    """
    {
      "clazz": "com.dotcms.contenttype.model.field.ImmutableRelationshipField",
      "contentTypeId": "#(parentTypeId)",
      "name": "Test Relationship",
      "variable": "relationField",
      "required": false,
      "indexed": true,
      "listed": false,
      "searchable": true,
      "unique": false,
      "relationType": "#(relationType)",
      "values": "3"
    }
    """
    And request relationRequest
    When method POST
    Then status 200
    * def relationshipFieldId = response.entity.id
    
    # 5. Get current layout
    Given path '/api/v3/contenttype/' + parentTypeId + '/fields'
    When method GET
    Then status 200
    
    # Store the layout structure but only including the text field (omitting relationship)
    * def currentLayout = response.entity
    * def textFieldOnly = []
    * def found = false
    
    # Loop through the layout to find the text field and omit the relationship field
    * eval for(var i=0; i<currentLayout.length; i++) { var row = currentLayout[i]; if (row.columns && row.columns.length > 0) { for(var j=0; j<row.columns.length; j++) { var col = row.columns[j]; if (col.fields && col.fields.length > 0) { var newFields = []; for(var k=0; k<col.fields.length; k++) { if(col.fields[k].id == textFieldId) { newFields.push(col.fields[k]); found = true; } } col.fields = newFields; } } } if (found) { textFieldOnly.push(row); } }
    
    # 6. Update layout without the relationship field
    Given path '/api/v3/contenttype/' + parentTypeId + '/fields/move'
    And request { layout: '#(textFieldOnly)' }
    When method PUT
    Then status 200
    
    # 7. Verify relationship field was preserved in the returned layout
    * def responseLayout = response.entity
    * def relationshipPreserved = false
    
    # Check if relationship field exists in the response
    * eval for(var i=0; i<responseLayout.length; i++) { var row = responseLayout[i]; if (row.columns && row.columns.length > 0) { for(var j=0; j<row.columns.length; j++) { var col = row.columns[j]; if (col.fields && col.fields.length > 0) { for(var k=0; k<col.fields.length; k++) { if(col.fields[k].id == relationshipFieldId) { relationshipPreserved = true; } } } } } }
    
    # Assert that the relationship field was preserved
    * assert relationshipPreserved == true
    
    # 8. Get the layout again to verify changes were saved
    Given path '/api/v3/contenttype/' + parentTypeId + '/fields'
    When method GET
    Then status 200
    
    # Check if relationship field exists in the database layout
    * def dbLayout = response.entity
    * def relationshipInDb = false
    
    # Check the saved layout for the relationship field
    * eval for(var i=0; i<dbLayout.length; i++) { var row = dbLayout[i]; if (row.columns && row.columns.length > 0) { for(var j=0; j<row.columns.length; j++) { var col = row.columns[j]; if (col.fields && col.fields.length > 0) { for(var k=0; k<col.fields.length; k++) { if(col.fields[k].id == relationshipFieldId) { relationshipInDb = true; } } } } } }
    
    # Assert that the relationship field was preserved in the database
    * assert relationshipInDb == true
    
    # 9. Verify relationships exist in database
    Given path '/api/v1/relationships'
    And param contentTypeId = parentTypeId
    When method GET
    Then status 200
    And match response != null
    * def relationshipsExist = karate.sizeOf(response.entity) > 0
    * print "Found relationships:", relationshipsExist
    
    # Clean up: Delete content types
    Given path '/api/v1/contenttype/' + parentTypeId
    When method DELETE
    * def statusCode = responseStatus
    * assert statusCode == 200 || statusCode == 404
    
    Given path '/api/v1/contenttype/' + childTypeId
    When method DELETE
    * def statusCode = responseStatus
    * assert statusCode == 200 || statusCode == 404 