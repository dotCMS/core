Feature: Multiple Relationship Fields Preservation in Content Type Layouts

  Background:
    * url baseUrl
    * headers commonHeaders
    # Generate unique identifiers for this test run
    * def parentTypeVar = 'ParentType' + Math.floor(Math.random() * 1000000)
    * def childTypeVar1 = 'ChildType1_' + Math.floor(Math.random() * 1000000)
    * def childTypeVar2 = 'ChildType2_' + Math.floor(Math.random() * 1000000)
    * def textFieldVar = 'textField' + Math.floor(Math.random() * 1000000)
    * def relationshipFieldVar1 = 'relationField1_' + Math.floor(Math.random() * 1000000)
    * def relationshipFieldVar2 = 'relationField2_' + Math.floor(Math.random() * 1000000)
  
  Scenario: Create content types with multiple relationship fields and verify preservation
    
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
      "name": "Parent Content Type Multiple Rel Test",
      "variable": "#(parentTypeVar)"
    }
    """
    When method POST
    Then status 200
    And match response.entity[0].id != null
    
    # Store the parent content type ID
    * def parentTypeId = response.entity[0].id
    
    # 2. Create first child content type
    Given path '/api/v1/contenttype'
    And request
    """
    {
      "clazz": "com.dotcms.contenttype.model.type.ImmutableSimpleContentType",
      "defaultType": false,
      "fixed": false,
      "folder": "SYSTEM_FOLDER",
      "host": "SYSTEM_HOST",
      "name": "Child Content Type 1 Test",
      "variable": "#(childTypeVar1)"
    }
    """
    When method POST
    Then status 200
    And match response.entity[0].id != null
    
    # Store the first child content type ID
    * def childTypeId1 = response.entity[0].id
    
    # 3. Create second child content type
    Given path '/api/v1/contenttype'
    And request
    """
    {
      "clazz": "com.dotcms.contenttype.model.type.ImmutableSimpleContentType",
      "defaultType": false,
      "fixed": false,
      "folder": "SYSTEM_FOLDER",
      "host": "SYSTEM_HOST",
      "name": "Child Content Type 2 Test",
      "variable": "#(childTypeVar2)"
    }
    """
    When method POST
    Then status 200
    And match response.entity[0].id != null
    
    # Store the second child content type ID
    * def childTypeId2 = response.entity[0].id
    
    # 4. Add text field to parent content type
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
    
    # 5. Add first relationship field to parent content type and store ID
    Given path '/api/v1/contenttype/' + parentTypeId + '/fields'
    * def relationType1 = parentTypeVar + '-' + childTypeVar1
    * def relationRequest1 = 
    """
    {
      "clazz": "com.dotcms.contenttype.model.field.ImmutableRelationshipField",
      "contentTypeId": "#(parentTypeId)",
      "name": "First Relationship",
      "variable": "relationfield1960027",
      "required": false,
      "indexed": true,
      "listed": false,
      "searchable": true,
      "unique": false,
      "relationType": "#(relationType1)",
      "values": "1"
    }
    """
    And request relationRequest1
    When method POST
    Then status 200
    * def relationshipFieldId1 = response.entity.id
    
    # 6. Add second relationship field to parent content type and store ID
    Given path '/api/v1/contenttype/' + parentTypeId + '/fields'
    * def relationType2 = parentTypeVar + '-' + childTypeVar2
    * def relationRequest2 = 
    """
    {
      "clazz": "com.dotcms.contenttype.model.field.ImmutableRelationshipField",
      "contentTypeId": "#(parentTypeId)",
      "name": "Second Relationship",
      "variable": "relationfield2960027",
      "required": false,
      "indexed": true,
      "listed": false,
      "searchable": true,
      "unique": false,
      "relationType": "#(relationType2)",
      "values": "2"
    }
    """
    And request relationRequest2
    When method POST
    Then status 200
    * def relationshipFieldId2 = response.entity.id
    
    # 7. Get current layout
    Given path '/api/v3/contenttype/' + parentTypeId + '/fields'
    When method GET
    Then status 200
    
    # Store the layout structure but only including the text field (omitting relationship fields)
    * def currentLayout = response.entity
    * def textFieldOnly = []
    * def found = false
    
    # Loop through the layout to find the text field and omit the relationship fields
    * eval for(var i=0; i<currentLayout.length; i++) { var row = currentLayout[i]; if (row.columns && row.columns.length > 0) { for(var j=0; j<row.columns.length; j++) { var col = row.columns[j]; if (col.fields && col.fields.length > 0) { var newFields = []; for(var k=0; k<col.fields.length; k++) { if(col.fields[k].id == textFieldId) { newFields.push(col.fields[k]); found = true; } } col.fields = newFields; } } } if (found) { textFieldOnly.push(row); } }
    
    # 8. Update layout without the relationship fields
    Given path '/api/v3/contenttype/' + parentTypeId + '/fields/move'
    And request { layout: '#(textFieldOnly)' }
    When method PUT
    Then status 200
    
    # 9. Verify both relationship fields were preserved in the returned layout
    * def responseLayout = response.entity
    * def rel1Preserved = false
    * def rel2Preserved = false
    
    # Check if relationship fields exist in the response
    * eval for(var i=0; i<responseLayout.length; i++) { var row = responseLayout[i]; if (row.columns && row.columns.length > 0) { for(var j=0; j<row.columns.length; j++) { var col = row.columns[j]; if (col.fields && col.fields.length > 0) { for(var k=0; k<col.fields.length; k++) { if(col.fields[k].id == relationshipFieldId1) { rel1Preserved = true; } if(col.fields[k].id == relationshipFieldId2) { rel2Preserved = true; } } } } } }
    
    # Assert that both relationship fields were preserved
    * assert rel1Preserved == true
    * assert rel2Preserved == true
    
    # 10. Get the layout again to verify changes were saved
    Given path '/api/v3/contenttype/' + parentTypeId + '/fields'
    When method GET
    Then status 200
    
    # Check if relationship fields exist in the database layout
    * def dbLayout = response.entity
    * def rel1InDb = false
    * def rel2InDb = false
    
    # Check the saved layout for the relationship fields
    * eval for(var i=0; i<dbLayout.length; i++) { var row = dbLayout[i]; if (row.columns && row.columns.length > 0) { for(var j=0; j<row.columns.length; j++) { var col = row.columns[j]; if (col.fields && col.fields.length > 0) { for(var k=0; k<col.fields.length; k++) { if(col.fields[k].id == relationshipFieldId1) { rel1InDb = true; } if(col.fields[k].id == relationshipFieldId2) { rel2InDb = true; } } } } } }
    
    # Assert that both relationship fields were preserved in the database
    * assert rel1InDb == true
    * assert rel2InDb == true
    
    # 11. Verify relationships exist in database
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
    
    Given path '/api/v1/contenttype/' + childTypeId1
    When method DELETE
    * def statusCode = responseStatus
    * assert statusCode == 200 || statusCode == 404
    
    Given path '/api/v1/contenttype/' + childTypeId2
    When method DELETE
    * def statusCode = responseStatus
    * assert statusCode == 200 || statusCode == 404 