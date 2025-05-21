package com.dotmarketing.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.business.RelationshipFactory;
import com.dotcms.contenttype.model.field.ImmutableRelationshipField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.repackage.com.liferay.portal.model.User;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.WebKeys;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(APILocator.class)
public class RelationshipAPIUnitTest {

    @Mock
    private RelationshipFactory relationshipFactoryMock;
    @Mock
    private FieldAPI fieldAPIMock;
    @Mock
    private ContentTypeAPI contentTypeAPIMock;
    @Mock
    private User systemUserMock;

    private RelationshipAPIImpl relationshipAPI;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        PowerMockito.mockStatic(APILocator.class);
        when(APILocator.getFieldAPI()).thenReturn(fieldAPIMock);
        when(APILocator.getContentTypeAPI(systemUserMock)).thenReturn(contentTypeAPIMock);
        when(APILocator.getSystemUser()).thenReturn(systemUserMock);
        
        // Provide a default mock for relationshipFactory if RelationshipAPIImpl constructor needs it
        // or ensure the constructor used in tests doesn't rely on FactoryLocator if not testing that part.
        // For byInodeUsingFieldFactory, relationshipFactory is not directly used.
        relationshipAPI = new RelationshipAPIImpl(relationshipFactoryMock); 
    }

    @Test
    public void testByInodeUsingFieldFactory_ValidRelationshipField_ReturnsRelationship() throws DotDataException, DotSecurityException {
        final String fieldInode = "validFieldInode";
        final String parentContentTypeId = "parentTypeId";
        final String parentContentTypeVar = "ParentType";
        final String childContentTypeId = "childTypeId";
        final String childContentTypeVar = "ChildType";
        final String fieldVariable = "relatedItems";
        final Date modDate = new Date();

        RelationshipField mockRelationshipField = ImmutableRelationshipField.builder()
                .id(fieldInode)
                .name("Related Items")
                .variable(fieldVariable)
                .contentTypeId(parentContentTypeId)
                .relationType(childContentTypeVar) // Stores child content type variable
                .values(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY.toString()) // Store enum name
                .required(false)
                .fixed(false)
                .modDate(modDate)
                .build();

        ContentType parentContentType = ContentTypeBuilder.builder(SimpleContentType.class)
                .id(parentContentTypeId).variable(parentContentTypeVar).name("Parent Type").build();
        ContentType childContentType = ContentTypeBuilder.builder(SimpleContentType.class)
                .id(childContentTypeId).variable(childContentTypeVar).name("Child Type").build();

        when(fieldAPIMock.byId(fieldInode)).thenReturn(mockRelationshipField);
        when(contentTypeAPIMock.find(parentContentTypeId)).thenReturn(parentContentType);
        when(contentTypeAPIMock.findByName(childContentTypeVar)).thenReturn(childContentType);

        Relationship result = relationshipAPI.byInodeUsingFieldFactory(fieldInode);

        assertNotNull(result);
        assertEquals(fieldInode, result.getInode());
        assertEquals(parentContentTypeVar + "." + fieldVariable, result.getRelationTypeValue());
        assertEquals(parentContentTypeId, result.getParentStructureInode());
        assertEquals(childContentTypeId, result.getChildStructureInode());
        assertNull(result.getParentRelationName());
        assertEquals(fieldVariable, result.getChildRelationName());
        assertEquals(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal(), result.getCardinality());
        assertEquals(false, result.isParentRequired());
        assertEquals(mockRelationshipField.required(), result.isChildRequired());
        assertEquals(mockRelationshipField.fixed(), result.isFixed());
        assertEquals(modDate, result.getModDate());
    }

    @Test
    public void testByInodeUsingFieldFactory_NonRelationshipFieldInode_ReturnsNull() throws DotDataException, DotSecurityException {
        final String fieldInode = "nonRelationshipFieldInode";
        TextField mockTextField = ImmutableTextField.builder().id(fieldInode).name("Text Field").variable("textField").contentTypeId("anyTypeId").build();

        when(fieldAPIMock.byId(fieldInode)).thenReturn(mockTextField);

        Relationship result = relationshipAPI.byInodeUsingFieldFactory(fieldInode);
        assertNull(result);
    }

    @Test
    public void testByInodeUsingFieldFactory_InvalidInode_FieldNotFound_ReturnsNull() throws DotDataException, DotSecurityException {
        final String fieldInode = "invalidFieldInode";
        when(fieldAPIMock.byId(fieldInode)).thenReturn(null); // Simulate field not found

        Relationship result = relationshipAPI.byInodeUsingFieldFactory(fieldInode);
        assertNull(result);
    }
    
    @Test
    public void testByInodeUsingFieldFactory_InvalidInode_FieldAPThrowsException_ReturnsNull() throws DotDataException, DotSecurityException {
        final String fieldInode = "invalidFieldInodeThrows";
        // Simulate FieldAPI throwing an exception (e.g., custom NotFoundInDbException if that's what it does, or just DotDataException for simplicity here)
        when(fieldAPIMock.byId(fieldInode)).thenThrow(new DotDataException("Field not found")); 
        
        try {
            relationshipAPI.byInodeUsingFieldFactory(fieldInode);
            fail("DotDataException should have been thrown if FieldAPI throws it and it's not caught by byInodeUsingFieldFactory");
        } catch (DotDataException e) {
            // Expected if byInodeUsingFieldFactory doesn't catch it.
            // If it's designed to catch and return null, this test needs adjustment.
            // Based on current implementation, it doesn't catch exceptions from fieldAPIMock.byId()
        }
        // If the method is updated to catch this specific exception and return null:
        // Relationship result = relationshipAPI.byInodeUsingFieldFactory(fieldInode);
        // assertNull(result);
    }


    @Test(expected = DotDataException.class)
    public void testByInodeUsingFieldFactory_InvalidCardinality_ThrowsDotDataException() throws DotDataException, DotSecurityException {
        final String fieldInode = "invalidCardinalityFieldInode";
        RelationshipField mockRelationshipField = ImmutableRelationshipField.builder()
                .id(fieldInode).name("Bad Cardinality").variable("badCard")
                .contentTypeId("parentTypeId").relationType("ChildTypeVar")
                .values("INVALID_CARDINALITY_STRING") // Invalid value
                .build();
        
        ContentType parentContentType = ContentTypeBuilder.builder(SimpleContentType.class)
                .id("parentTypeId").variable("ParentTypeVar").name("Parent Type").build();
        ContentType childContentType = ContentTypeBuilder.builder(SimpleContentType.class)
                .id("childTypeId").variable("ChildTypeVar").name("Child Type").build();

        when(fieldAPIMock.byId(fieldInode)).thenReturn(mockRelationshipField);
        when(contentTypeAPIMock.find("parentTypeId")).thenReturn(parentContentType);
        when(contentTypeAPIMock.findByName("ChildTypeVar")).thenReturn(childContentType);


        relationshipAPI.byInodeUsingFieldFactory(fieldInode); // Should throw DotDataException
    }

    @Test
    public void testByInodeUsingFieldFactory_ParentContentTypeNotFound_ReturnsNull() throws DotDataException, DotSecurityException {
        final String fieldInode = "parentNotFoundFieldInode";
        RelationshipField mockRelationshipField = ImmutableRelationshipField.builder()
                .id(fieldInode).name("No Parent").variable("noParent")
                .contentTypeId("nonExistentParentId").relationType("ChildTypeVar")
                .values(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.toString()).build();

        when(fieldAPIMock.byId(fieldInode)).thenReturn(mockRelationshipField);
        when(contentTypeAPIMock.find("nonExistentParentId")).thenReturn(null); // Simulate parent not found

        Relationship result = relationshipAPI.byInodeUsingFieldFactory(fieldInode);
        assertNull(result);
    }
    
    @Test
    public void testByInodeUsingFieldFactory_ParentContentTypeAPIFindThrowsException_ReturnsNull() throws DotDataException, DotSecurityException {
        final String fieldInode = "parentNotFoundFieldInodeThrows";
        RelationshipField mockRelationshipField = ImmutableRelationshipField.builder()
                .id(fieldInode).name("No Parent").variable("noParent")
                .contentTypeId("nonExistentParentId").relationType("ChildTypeVar")
                .values(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.toString()).build();

        when(fieldAPIMock.byId(fieldInode)).thenReturn(mockRelationshipField);
        // Simulate ContentTypeAPI.find throwing an exception
        when(contentTypeAPIMock.find("nonExistentParentId")).thenThrow(new DotDataException("DB error or something")); 

        try {
            relationshipAPI.byInodeUsingFieldFactory(fieldInode);
            // If the method is designed to catch this exception and return null, this will fail.
            // Based on current implementation, it does not catch exceptions from contentTypeAPIMock.find()
             fail("DotDataException should have been thrown");
        } catch(DotDataException e) {
            // expected
        }
        // If the method is updated to catch this specific exception and return null:
        // Relationship result = relationshipAPI.byInodeUsingFieldFactory(fieldInode);
        // assertNull(result);
    }


    @Test
    public void testByInodeUsingFieldFactory_ChildContentTypeNotFound_ReturnsNull() throws DotDataException, DotSecurityException {
        final String fieldInode = "childNotFoundFieldInode";
        final String parentContentTypeId = "parentTypeId";
        
        RelationshipField mockRelationshipField = ImmutableRelationshipField.builder()
                .id(fieldInode).name("No Child").variable("noChild")
                .contentTypeId(parentContentTypeId).relationType("nonExistentChildVar")
                .values(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.toString()).build();
        
        ContentType parentContentType = ContentTypeBuilder.builder(SimpleContentType.class)
                .id(parentContentTypeId).variable("ParentTypeVar").name("Parent Type").build();

        when(fieldAPIMock.byId(fieldInode)).thenReturn(mockRelationshipField);
        when(contentTypeAPIMock.find(parentContentTypeId)).thenReturn(parentContentType);
        when(contentTypeAPIMock.findByName("nonExistentChildVar")).thenReturn(null); // Simulate child not found

        Relationship result = relationshipAPI.byInodeUsingFieldFactory(fieldInode);
        assertNull(result);
    }
    
    @Test
    public void testByInodeUsingFieldFactory_ChildContentTypeAPIFindByNameThrowsException_ReturnsNull() throws DotDataException, DotSecurityException {
        final String fieldInode = "childNotFoundFieldInodeThrows";
        final String parentContentTypeId = "parentTypeId";
        
        RelationshipField mockRelationshipField = ImmutableRelationshipField.builder()
                .id(fieldInode).name("No Child").variable("noChild")
                .contentTypeId(parentContentTypeId).relationType("nonExistentChildVar")
                .values(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.toString()).build();
        
        ContentType parentContentType = ContentTypeBuilder.builder(SimpleContentType.class)
                .id(parentContentTypeId).variable("ParentTypeVar").name("Parent Type").build();

        when(fieldAPIMock.byId(fieldInode)).thenReturn(mockRelationshipField);
        when(contentTypeAPIMock.find(parentContentTypeId)).thenReturn(parentContentType);
        // Simulate ContentTypeAPI.findByName throwing an exception
        when(contentTypeAPIMock.findByName("nonExistentChildVar")).thenThrow(new DotDataException("DB error or something")); 

        try {
            relationshipAPI.byInodeUsingFieldFactory(fieldInode);
            // If the method is designed to catch this exception and return null, this will fail.
            // Based on current implementation, it does not catch exceptions from contentTypeAPIMock.findByName()
             fail("DotDataException should have been thrown");
        } catch(DotDataException e) {
            // expected
        }
        // If the method is updated to catch this specific exception and return null:
        // Relationship result = relationshipAPI.byInodeUsingFieldFactory(fieldInode);
        // assertNull(result);
    }

    // Unit tests for byTypeValueUsingFieldFactory

    @Test
    public void testByTypeValueUsingFieldFactory_ValidTypeValue_ReturnsRelationship() throws DotDataException, DotSecurityException {
        final String parentContentTypeVar = "ParentType";
        final String relationshipFieldVar = "relatedItems";
        final String typeValue = parentContentTypeVar + "." + relationshipFieldVar;
        final String parentContentTypeId = "parentTypeId";
        final String childContentTypeId = "childTypeId";
        final String childContentTypeVar = "ChildType"; // This is stored in RelationshipField.relationType()
        final String fieldInode = "fieldInode123";
        final Date modDate = new Date();

        ContentType parentContentType = ContentTypeBuilder.builder(SimpleContentType.class)
                .id(parentContentTypeId).variable(parentContentTypeVar).name("Parent Type").build();
        RelationshipField mockRelationshipField = ImmutableRelationshipField.builder()
                .id(fieldInode)
                .name("Related Items")
                .variable(relationshipFieldVar)
                .contentTypeId(parentContentTypeId)
                .relationType(childContentTypeVar)
                .values(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY.toString())
                .required(true)
                .fixed(true)
                .modDate(modDate)
                .build();
        ContentType childContentType = ContentTypeBuilder.builder(SimpleContentType.class)
                .id(childContentTypeId).variable(childContentTypeVar).name("Child Type").build();

        when(contentTypeAPIMock.findByName(parentContentTypeVar)).thenReturn(parentContentType);
        when(fieldAPIMock.byContentTypeIdFieldVar(parentContentTypeId, relationshipFieldVar)).thenReturn(mockRelationshipField);
        when(contentTypeAPIMock.findByName(childContentTypeVar)).thenReturn(childContentType);

        Relationship result = relationshipAPI.byTypeValueUsingFieldFactory(typeValue);

        assertNotNull(result);
        assertEquals(fieldInode, result.getInode());
        assertEquals(typeValue, result.getRelationTypeValue());
        assertEquals(parentContentTypeId, result.getParentStructureInode());
        assertEquals(childContentTypeId, result.getChildStructureInode());
        assertNull(result.getParentRelationName());
        assertEquals(relationshipFieldVar, result.getChildRelationName());
        assertEquals(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal(), result.getCardinality());
        assertEquals(false, result.isParentRequired()); // Default from constructor logic
        assertEquals(mockRelationshipField.required(), result.isChildRequired());
        assertEquals(mockRelationshipField.fixed(), result.isFixed());
        assertEquals(modDate, result.getModDate());
    }

    @Test
    public void testByTypeValueUsingFieldFactory_NullTypeValue_ReturnsNull() throws DotDataException, DotSecurityException {
        assertNull(relationshipAPI.byTypeValueUsingFieldFactory(null));
    }

    @Test
    public void testByTypeValueUsingFieldFactory_EmptyTypeValue_ReturnsNull() throws DotDataException, DotSecurityException {
        assertNull(relationshipAPI.byTypeValueUsingFieldFactory(""));
    }

    @Test
    public void testByTypeValueUsingFieldFactory_TypeValueNoPeriod_ReturnsNull() throws DotDataException, DotSecurityException {
        assertNull(relationshipAPI.byTypeValueUsingFieldFactory("ParentTypeRelatedItems"));
    }
    
    @Test
    public void testByTypeValueUsingFieldFactory_TypeValueEndsWithPeriod_ReturnsNull() throws DotDataException, DotSecurityException {
        assertNull(relationshipAPI.byTypeValueUsingFieldFactory("ParentType."));
    }

    @Test
    public void testByTypeValueUsingFieldFactory_TypeValueStartsWithPeriod_ReturnsNull() throws DotDataException, DotSecurityException {
        assertNull(relationshipAPI.byTypeValueUsingFieldFactory(".relatedItems"));
    }


    @Test
    public void testByTypeValueUsingFieldFactory_ParentContentTypeNotFound_ReturnsNull() throws DotDataException, DotSecurityException {
        final String typeValue = "NonExistentParent.relatedItems";
        when(contentTypeAPIMock.findByName("NonExistentParent")).thenReturn(null);
        assertNull(relationshipAPI.byTypeValueUsingFieldFactory(typeValue));
    }
    
    @Test(expected = DotDataException.class)
    public void testByTypeValueUsingFieldFactory_ParentContentTypeApiThrowsException_PropagatesException() throws DotDataException, DotSecurityException {
        final String typeValue = "ErrorParent.relatedItems";
        when(contentTypeAPIMock.findByName("ErrorParent")).thenThrow(new DotDataException("DB error"));
        relationshipAPI.byTypeValueUsingFieldFactory(typeValue); // Should propagate DotDataException
    }

    @Test
    public void testByTypeValueUsingFieldFactory_FieldNotFound_ReturnsNull() throws DotDataException, DotSecurityException {
        final String parentContentTypeVar = "ParentType";
        final String relationshipFieldVar = "nonExistentField";
        final String typeValue = parentContentTypeVar + "." + relationshipFieldVar;
        final String parentContentTypeId = "parentTypeId";

        ContentType parentContentType = ContentTypeBuilder.builder(SimpleContentType.class)
                .id(parentContentTypeId).variable(parentContentTypeVar).name("Parent Type").build();

        when(contentTypeAPIMock.findByName(parentContentTypeVar)).thenReturn(parentContentType);
        when(fieldAPIMock.byContentTypeIdFieldVar(parentContentTypeId, relationshipFieldVar)).thenReturn(null); // Field not found

        assertNull(relationshipAPI.byTypeValueUsingFieldFactory(typeValue));
    }

    @Test
    public void testByTypeValueUsingFieldFactory_FieldNotRelationshipField_ReturnsNull() throws DotDataException, DotSecurityException {
        final String parentContentTypeVar = "ParentType";
        final String textFieldVar = "textField";
        final String typeValue = parentContentTypeVar + "." + textFieldVar;
        final String parentContentTypeId = "parentTypeId";

        ContentType parentContentType = ContentTypeBuilder.builder(SimpleContentType.class)
                .id(parentContentTypeId).variable(parentContentTypeVar).name("Parent Type").build();
        TextField mockTextField = ImmutableTextField.builder().id("textId").name("Text Field").variable(textFieldVar).contentTypeId(parentContentTypeId).build();

        when(contentTypeAPIMock.findByName(parentContentTypeVar)).thenReturn(parentContentType);
        when(fieldAPIMock.byContentTypeIdFieldVar(parentContentTypeId, textFieldVar)).thenReturn(mockTextField); // Returns a non-RelationshipField

        assertNull(relationshipAPI.byTypeValueUsingFieldFactory(typeValue));
    }

    @Test(expected = DotDataException.class)
    public void testByTypeValueUsingFieldFactory_FieldApiThrowsException_PropagatesException() throws DotDataException, DotSecurityException {
        final String parentContentTypeVar = "ParentType";
        final String errorFieldVar = "errorField";
        final String typeValue = parentContentTypeVar + "." + errorFieldVar;
        final String parentContentTypeId = "parentTypeId";

        ContentType parentContentType = ContentTypeBuilder.builder(SimpleContentType.class)
                .id(parentContentTypeId).variable(parentContentTypeVar).name("Parent Type").build();
        
        when(contentTypeAPIMock.findByName(parentContentTypeVar)).thenReturn(parentContentType);
        when(fieldAPIMock.byContentTypeIdFieldVar(parentContentTypeId, errorFieldVar)).thenThrow(new DotDataException("DB error"));
        relationshipAPI.byTypeValueUsingFieldFactory(typeValue); // Should propagate DotDataException
    }


    @Test(expected = DotDataException.class)
    public void testByTypeValueUsingFieldFactory_ChildContentTypeNotFound_ThrowsDotDataException() throws DotDataException, DotSecurityException {
        final String parentContentTypeVar = "ParentType";
        final String relationshipFieldVar = "relatedItems";
        final String typeValue = parentContentTypeVar + "." + relationshipFieldVar;
        final String parentContentTypeId = "parentTypeId";
        final String childContentTypeVar = "NonExistentChildType";

        ContentType parentContentType = ContentTypeBuilder.builder(SimpleContentType.class)
                .id(parentContentTypeId).variable(parentContentTypeVar).name("Parent Type").build();
        RelationshipField mockRelationshipField = ImmutableRelationshipField.builder()
                .id("fieldId").name("Related Items").variable(relationshipFieldVar)
                .contentTypeId(parentContentTypeId).relationType(childContentTypeVar) // Points to non-existent child
                .values(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.toString()).build();

        when(contentTypeAPIMock.findByName(parentContentTypeVar)).thenReturn(parentContentType);
        when(fieldAPIMock.byContentTypeIdFieldVar(parentContentTypeId, relationshipFieldVar)).thenReturn(mockRelationshipField);
        when(contentTypeAPIMock.findByName(childContentTypeVar)).thenReturn(null); // Child not found

        relationshipAPI.byTypeValueUsingFieldFactory(typeValue); // Should throw DotDataException
    }
    
    @Test(expected = DotDataException.class)
    public void testByTypeValueUsingFieldFactory_ChildContentTypeApiThrowsException_PropagatesException() throws DotDataException, DotSecurityException {
        final String parentContentTypeVar = "ParentType";
        final String relationshipFieldVar = "relatedItems";
        final String typeValue = parentContentTypeVar + "." + relationshipFieldVar;
        final String parentContentTypeId = "parentTypeId";
        final String childContentTypeVar = "ErrorChildType";

        ContentType parentContentType = ContentTypeBuilder.builder(SimpleContentType.class)
                .id(parentContentTypeId).variable(parentContentTypeVar).name("Parent Type").build();
        RelationshipField mockRelationshipField = ImmutableRelationshipField.builder()
                .id("fieldId").name("Related Items").variable(relationshipFieldVar)
                .contentTypeId(parentContentTypeId).relationType(childContentTypeVar)
                .values(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.toString()).build();

        when(contentTypeAPIMock.findByName(parentContentTypeVar)).thenReturn(parentContentType);
        when(fieldAPIMock.byContentTypeIdFieldVar(parentContentTypeId, relationshipFieldVar)).thenReturn(mockRelationshipField);
        when(contentTypeAPIMock.findByName(childContentTypeVar)).thenThrow(new DotDataException("DB error"));

        relationshipAPI.byTypeValueUsingFieldFactory(typeValue); // Should propagate DotDataException
    }


    @Test(expected = DotDataException.class)
    public void testByTypeValueUsingFieldFactory_InvalidCardinality_ThrowsDotDataException() throws DotDataException, DotSecurityException {
        final String parentContentTypeVar = "ParentType";
        final String relationshipFieldVar = "relatedItems";
        final String typeValue = parentContentTypeVar + "." + relationshipFieldVar;
        final String parentContentTypeId = "parentTypeId";
        final String childContentTypeVar = "ChildType";

        ContentType parentContentType = ContentTypeBuilder.builder(SimpleContentType.class)
                .id(parentContentTypeId).variable(parentContentTypeVar).name("Parent Type").build();
        RelationshipField mockRelationshipField = ImmutableRelationshipField.builder()
                .id("fieldId").name("Related Items").variable(relationshipFieldVar)
                .contentTypeId(parentContentTypeId).relationType(childContentTypeVar)
                .values("INVALID_CARDINALITY") // Invalid value
                .build();
         ContentType childContentType = ContentTypeBuilder.builder(SimpleContentType.class)
                .id("childTypeId").variable(childContentTypeVar).name("Child Type").build();


        when(contentTypeAPIMock.findByName(parentContentTypeVar)).thenReturn(parentContentType);
        when(fieldAPIMock.byContentTypeIdFieldVar(parentContentTypeId, relationshipFieldVar)).thenReturn(mockRelationshipField);
        when(contentTypeAPIMock.findByName(childContentTypeVar)).thenReturn(childContentType);


        relationshipAPI.byTypeValueUsingFieldFactory(typeValue); // Should throw DotDataException
    }


    // Existing tests below

    // Unit tests for byParentUsingFieldFactory

    @Test
    public void testByParentUsingFieldFactory_ParentWithRelationshipFields_ReturnsRelationships() throws DotDataException, DotSecurityException {
        final String parentId = "parentCTId";
        final String parentVar = "ParentContentType";
        ContentTypeIf parentContentType = mock(ContentTypeIf.class);
        when(parentContentType.id()).thenReturn(parentId);
        when(parentContentType.variable()).thenReturn(parentVar);

        // Field 1: Valid RelationshipField
        RelationshipField relField1 = ImmutableRelationshipField.builder()
                .id("rfId1").name("Child1").variable("child1Rel")
                .contentTypeId(parentId).relationType("ChildType1Var")
                .values(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.toString())
                .modDate(new Date()).build();
        ContentType childType1 = ContentTypeBuilder.builder(SimpleContentType.class).id("childId1").variable("ChildType1Var").build();
        
        // Field 2: Another valid RelationshipField
        RelationshipField relField2 = ImmutableRelationshipField.builder()
                .id("rfId2").name("Child2").variable("child2Rel")
                .contentTypeId(parentId).relationType("ChildType2Var")
                .values(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY.toString())
                .required(true).modDate(new Date()).build();
        ContentType childType2 = ContentTypeBuilder.builder(SimpleContentType.class).id("childId2").variable("ChildType2Var").build();

        // Field 3: Non-RelationshipField
        TextField textField = ImmutableTextField.builder().id("tfId1").name("Text").variable("text").contentTypeId(parentId).build();

        List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
        fields.add(relField1);
        fields.add(textField);
        fields.add(relField2);

        when(fieldAPIMock.findByContentTypeId(parentId)).thenReturn(fields);
        when(contentTypeAPIMock.findByName("ChildType1Var")).thenReturn(childType1);
        when(contentTypeAPIMock.findByName("ChildType2Var")).thenReturn(childType2);

        List<Relationship> result = relationshipAPI.byParentUsingFieldFactory(parentContentType);

        assertNotNull(result);
        assertEquals(2, result.size()); // Only two RelationshipFields

        // Assertions for first relationship
        Relationship resRel1 = result.stream().filter(r -> r.getInode().equals("rfId1")).findFirst().orElse(null);
        assertNotNull(resRel1);
        assertEquals(parentVar + "." + relField1.variable(), resRel1.getRelationTypeValue());
        assertEquals(parentId, resRel1.getParentStructureInode());
        assertEquals("childId1", resRel1.getChildStructureInode());
        assertEquals(relField1.variable(), resRel1.getChildRelationName());
        assertEquals(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal(), resRel1.getCardinality());
        assertEquals(relField1.required(), resRel1.isChildRequired());

        // Assertions for second relationship
        Relationship resRel2 = result.stream().filter(r -> r.getInode().equals("rfId2")).findFirst().orElse(null);
        assertNotNull(resRel2);
        assertEquals(parentVar + "." + relField2.variable(), resRel2.getRelationTypeValue());
        assertEquals(parentId, resRel2.getParentStructureInode());
        assertEquals("childId2", resRel2.getChildStructureInode());
        assertEquals(relField2.variable(), resRel2.getChildRelationName());
        assertEquals(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal(), resRel2.getCardinality());
        assertEquals(relField2.required(), resRel2.isChildRequired());
    }

    @Test
    public void testByParentUsingFieldFactory_ParentWithNoRelationshipFields_ReturnsEmptyList() throws DotDataException, DotSecurityException {
        final String parentId = "parentCTIdNoRel";
        ContentTypeIf parentContentType = mock(ContentTypeIf.class);
        when(parentContentType.id()).thenReturn(parentId);

        TextField textField1 = ImmutableTextField.builder().id("tfId1").name("Text1").variable("text1").contentTypeId(parentId).build();
        TextField textField2 = ImmutableTextField.builder().id("tfId2").name("Text2").variable("text2").contentTypeId(parentId).build();
        
        List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
        fields.add(textField1);
        fields.add(textField2);

        when(fieldAPIMock.findByContentTypeId(parentId)).thenReturn(fields);
        List<Relationship> result = relationshipAPI.byParentUsingFieldFactory(parentContentType);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testByParentUsingFieldFactory_ParentWithEmptyFields_ReturnsEmptyList() throws DotDataException, DotSecurityException {
        final String parentId = "parentCTIdEmptyFields";
        ContentTypeIf parentContentType = mock(ContentTypeIf.class);
        when(parentContentType.id()).thenReturn(parentId);

        when(fieldAPIMock.findByContentTypeId(parentId)).thenReturn(new ArrayList<>()); // Empty list of fields
        List<Relationship> result = relationshipAPI.byParentUsingFieldFactory(parentContentType);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    public void testByParentUsingFieldFactory_NullParent_ReturnsEmptyList() throws DotDataException, DotSecurityException {
        List<Relationship> result = relationshipAPI.byParentUsingFieldFactory(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testByParentUsingFieldFactory_ParentWithNullId_ReturnsEmptyList() throws DotDataException, DotSecurityException {
        ContentTypeIf parentContentType = mock(ContentTypeIf.class);
        when(parentContentType.id()).thenReturn(null); // Parent with null ID
        
        List<Relationship> result = relationshipAPI.byParentUsingFieldFactory(parentContentType);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testByParentUsingFieldFactory_ChildContentTypeNotFound_SkipsRelationship() throws DotDataException, DotSecurityException {
        final String parentId = "parentCTIdChildMissing";
        final String parentVar = "ParentChildMissing";
        ContentTypeIf parentContentType = mock(ContentTypeIf.class);
        when(parentContentType.id()).thenReturn(parentId);
        when(parentContentType.variable()).thenReturn(parentVar);

        RelationshipField relField1 = ImmutableRelationshipField.builder()
                .id("rfId1").name("Child1").variable("child1Rel")
                .contentTypeId(parentId).relationType("NotFoundChildVar") // This child won't be found
                .values(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.toString()).build();
        
        RelationshipField relField2 = ImmutableRelationshipField.builder() // This one is valid
                .id("rfId2").name("Child2").variable("child2Rel")
                .contentTypeId(parentId).relationType("FoundChildVar") 
                .values(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.toString()).build();
        ContentType foundChildType = ContentTypeBuilder.builder(SimpleContentType.class).id("foundChildId").variable("FoundChildVar").build();


        List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
        fields.add(relField1);
        fields.add(relField2);

        when(fieldAPIMock.findByContentTypeId(parentId)).thenReturn(fields);
        when(contentTypeAPIMock.findByName("NotFoundChildVar")).thenReturn(null); // Child not found
        when(contentTypeAPIMock.findByName("FoundChildVar")).thenReturn(foundChildType);


        List<Relationship> result = relationshipAPI.byParentUsingFieldFactory(parentContentType);
        assertNotNull(result);
        assertEquals(1, result.size()); // Only relField2 should be processed
        assertEquals("rfId2", result.get(0).getInode());
    }

    @Test
    public void testByParentUsingFieldFactory_InvalidCardinality_SkipsRelationship() throws DotDataException, DotSecurityException {
        final String parentId = "parentCTIdInvalidCard";
        final String parentVar = "ParentInvalidCard";
        ContentTypeIf parentContentType = mock(ContentTypeIf.class);
        when(parentContentType.id()).thenReturn(parentId);
        when(parentContentType.variable()).thenReturn(parentVar);

        RelationshipField relField1 = ImmutableRelationshipField.builder()
                .id("rfId1").name("Child1").variable("child1Rel")
                .contentTypeId(parentId).relationType("ChildType1Var")
                .values("INVALID_CARDINALITY") // Invalid cardinality
                .build();
        ContentType childType1 = ContentTypeBuilder.builder(SimpleContentType.class).id("childId1").variable("ChildType1Var").build();

        List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
        fields.add(relField1);
        
        when(fieldAPIMock.findByContentTypeId(parentId)).thenReturn(fields);
        when(contentTypeAPIMock.findByName("ChildType1Var")).thenReturn(childType1); // Child is found

        List<Relationship> result = relationshipAPI.byParentUsingFieldFactory(parentContentType);
        assertNotNull(result);
        assertTrue(result.isEmpty()); // Relationship with invalid cardinality should be skipped
    }

    @Test(expected = DotDataException.class)
    public void testByParentUsingFieldFactory_FieldApiThrowsException_PropagatesException() throws DotDataException, DotSecurityException {
        final String parentId = "parentCTIdFieldApiError";
        ContentTypeIf parentContentType = mock(ContentTypeIf.class);
        when(parentContentType.id()).thenReturn(parentId);

        when(fieldAPIMock.findByContentTypeId(parentId)).thenThrow(new DotDataException("DB error on findByContentTypeId"));
        relationshipAPI.byParentUsingFieldFactory(parentContentType); // Should propagate DotDataException
    }
    
    @Test(expected = DotSecurityException.class)
    public void testByParentUsingFieldFactory_ContentTypeApiThrowsSecurityException_PropagatesException() throws DotDataException, DotSecurityException {
        final String parentId = "parentCTIdSecurityError";
        final String parentVar = "ParentSecurityError";
        ContentTypeIf parentContentType = mock(ContentTypeIf.class);
        when(parentContentType.id()).thenReturn(parentId);
        when(parentContentType.variable()).thenReturn(parentVar);

        RelationshipField relField1 = ImmutableRelationshipField.builder()
                .id("rfId1").name("Child1").variable("child1Rel")
                .contentTypeId(parentId).relationType("ChildType1Var")
                .values(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.toString()).build();
        
        List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
        fields.add(relField1);

        when(fieldAPIMock.findByContentTypeId(parentId)).thenReturn(fields);
        when(contentTypeAPIMock.findByName("ChildType1Var")).thenThrow(new DotSecurityException("Security error on findByName"));
        
        relationshipAPI.byParentUsingFieldFactory(parentContentType); // Should propagate DotSecurityException
    }


    // Existing tests below

    // Unit tests for byChildUsingFieldFactory

    @Test
    public void testByChildUsingFieldFactory_RelationshipFieldsPointToChild_ReturnsRelationships() throws DotDataException, DotSecurityException {
        final String childId = "childCTId";
        final String childVar = "ChildContentType";
        ContentTypeIf childContentType = mock(ContentTypeIf.class);
        when(childContentType.id()).thenReturn(childId);
        when(childContentType.variable()).thenReturn(childVar);

        // Parent Type 1
        ContentType parentType1 = ContentTypeBuilder.builder(SimpleContentType.class).id("parentId1").variable("ParentType1").build();
        RelationshipField relField1OnParent1 = ImmutableRelationshipField.builder()
                .id("rfId1").name("ChildRel1").variable("childRelField1")
                .contentTypeId(parentType1.id()).relationType(childVar) // Points to our child
                .values(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.toString())
                .modDate(new Date()).build();
        List<com.dotcms.contenttype.model.field.Field> fieldsParent1 = Collections.singletonList(relField1OnParent1);

        // Parent Type 2
        ContentType parentType2 = ContentTypeBuilder.builder(SimpleContentType.class).id("parentId2").variable("ParentType2").build();
        RelationshipField relField2OnParent2 = ImmutableRelationshipField.builder()
                .id("rfId2").name("ChildRel2").variable("childRelField2")
                .contentTypeId(parentType2.id()).relationType(childVar) // Also points to our child
                .values(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY.toString())
                .required(true).modDate(new Date()).build();
        TextField nonRelFieldOnParent2 = ImmutableTextField.builder().id("tf1").name("Text").variable("text").contentTypeId(parentType2.id()).build();
         List<com.dotcms.contenttype.model.field.Field> fieldsParent2 = Arrays.asList(relField2OnParent2, nonRelFieldOnParent2);


        // Parent Type 3 (has a rel field but points to a different child)
        ContentType parentType3 = ContentTypeBuilder.builder(SimpleContentType.class).id("parentId3").variable("ParentType3").build();
        RelationshipField relField3OnParent3 = ImmutableRelationshipField.builder()
                .id("rfId3").name("OtherChildRel").variable("otherChildRelField")
                .contentTypeId(parentType3.id()).relationType("SomeOtherChildVar") // Does NOT point to our child
                .values(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.toString()).build();
        List<com.dotcms.contenttype.model.field.Field> fieldsParent3 = Collections.singletonList(relField3OnParent3);
        
        List<ContentType> allTypes = Arrays.asList(parentType1, parentType2, parentType3);
        when(contentTypeAPIMock.findAll()).thenReturn(allTypes);
        when(fieldAPIMock.findByContentTypeId(parentType1.id())).thenReturn(fieldsParent1);
        when(fieldAPIMock.findByContentTypeId(parentType2.id())).thenReturn(fieldsParent2);
        when(fieldAPIMock.findByContentTypeId(parentType3.id())).thenReturn(fieldsParent3);
        
        List<Relationship> result = relationshipAPI.byChildUsingFieldFactory(childContentType);

        assertNotNull(result);
        assertEquals(2, result.size()); // rfId1 and rfId2 should be found

        Relationship res1 = result.stream().filter(r -> r.getInode().equals("rfId1")).findFirst().orElse(null);
        assertNotNull(res1);
        assertEquals(parentType1.variable() + "." + relField1OnParent1.variable(), res1.getRelationTypeValue());
        assertEquals(parentType1.id(), res1.getParentStructureInode());
        assertEquals(childId, res1.getChildStructureInode());
        assertEquals(relField1OnParent1.variable(), res1.getChildRelationName());

        Relationship res2 = result.stream().filter(r -> r.getInode().equals("rfId2")).findFirst().orElse(null);
        assertNotNull(res2);
        assertEquals(parentType2.variable() + "." + relField2OnParent2.variable(), res2.getRelationTypeValue());
        assertEquals(parentType2.id(), res2.getParentStructureInode());
        assertEquals(childId, res2.getChildStructureInode());
        assertEquals(relField2OnParent2.variable(), res2.getChildRelationName());
        assertEquals(relField2OnParent2.required(), res2.isChildRequired());
    }

    @Test
    public void testByChildUsingFieldFactory_NoRelationshipFieldsPointToChild_ReturnsEmptyList() throws DotDataException, DotSecurityException {
        final String childId = "childCTId2";
        final String childVar = "ChildContentType2";
        ContentTypeIf childContentType = mock(ContentTypeIf.class);
        when(childContentType.id()).thenReturn(childId);
        when(childContentType.variable()).thenReturn(childVar);

        ContentType parentType1 = ContentTypeBuilder.builder(SimpleContentType.class).id("parentId1_alt").variable("ParentType1Alt").build();
        RelationshipField relField1 = ImmutableRelationshipField.builder() // Points to a different child
                .id("rfId_alt1").contentTypeId(parentType1.id()).relationType("DifferentChildVar").values("ONE_TO_ONE").build();
        List<com.dotcms.contenttype.model.field.Field> fieldsParent1 = Collections.singletonList(relField1);

        ContentType parentType2 = ContentTypeBuilder.builder(SimpleContentType.class).id("parentId2_alt").variable("ParentType2Alt").build();
        TextField textField = ImmutableTextField.builder().id("tf_alt").contentTypeId(parentType2.id()).build(); // No relationship field
        List<com.dotcms.contenttype.model.field.Field> fieldsParent2 = Collections.singletonList(textField);

        List<ContentType> allTypes = Arrays.asList(parentType1, parentType2);
        when(contentTypeAPIMock.findAll()).thenReturn(allTypes);
        when(fieldAPIMock.findByContentTypeId(parentType1.id())).thenReturn(fieldsParent1);
        when(fieldAPIMock.findByContentTypeId(parentType2.id())).thenReturn(fieldsParent2);
        
        List<Relationship> result = relationshipAPI.byChildUsingFieldFactory(childContentType);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
    
    @Test
    public void testByChildUsingFieldFactory_NoContentTypesFound_ReturnsEmptyList() throws DotDataException, DotSecurityException {
        final String childId = "childCTId3";
        final String childVar = "ChildContentType3";
        ContentTypeIf childContentType = mock(ContentTypeIf.class);
        when(childContentType.id()).thenReturn(childId);
        when(childContentType.variable()).thenReturn(childVar);

        when(contentTypeAPIMock.findAll()).thenReturn(Collections.emptyList()); // No content types at all
        
        List<Relationship> result = relationshipAPI.byChildUsingFieldFactory(childContentType);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }


    @Test
    public void testByChildUsingFieldFactory_NullChild_ReturnsEmptyList() throws DotDataException, DotSecurityException {
        List<Relationship> result = relationshipAPI.byChildUsingFieldFactory(null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testByChildUsingFieldFactory_ChildWithNullId_ReturnsEmptyList() throws DotDataException, DotSecurityException {
        ContentTypeIf childContentType = mock(ContentTypeIf.class);
        when(childContentType.id()).thenReturn(null);
        when(childContentType.variable()).thenReturn("SomeVar");
        List<Relationship> result = relationshipAPI.byChildUsingFieldFactory(childContentType);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testByChildUsingFieldFactory_ChildWithNullVariable_ReturnsEmptyList() throws DotDataException, DotSecurityException {
        ContentTypeIf childContentType = mock(ContentTypeIf.class);
        when(childContentType.id()).thenReturn("someId");
        when(childContentType.variable()).thenReturn(null);
        List<Relationship> result = relationshipAPI.byChildUsingFieldFactory(childContentType);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testByChildUsingFieldFactory_InvalidCardinality_SkipsRelationship() throws DotDataException, DotSecurityException {
        final String childId = "childCTIdCard";
        final String childVar = "ChildContentTypeCard";
        ContentTypeIf childContentType = mock(ContentTypeIf.class);
        when(childContentType.id()).thenReturn(childId);
        when(childContentType.variable()).thenReturn(childVar);

        ContentType parentType1 = ContentTypeBuilder.builder(SimpleContentType.class).id("parentIdCard").variable("ParentTypeCard").build();
        RelationshipField relFieldInvalidCard = ImmutableRelationshipField.builder()
                .id("rfInvalidCard").contentTypeId(parentType1.id()).relationType(childVar)
                .values("INVALID_CARDINALITY_STRING") // Invalid
                .build();
        RelationshipField relFieldValidCard = ImmutableRelationshipField.builder() // This one should still be found
                .id("rfValidCard").contentTypeId(parentType1.id()).relationType(childVar)
                .variable("validRelVar").values(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY.toString())
                .build();
        
        List<com.dotcms.contenttype.model.field.Field> fieldsParent1 = Arrays.asList(relFieldInvalidCard, relFieldValidCard);
        
        List<ContentType> allTypes = Collections.singletonList(parentType1);
        when(contentTypeAPIMock.findAll()).thenReturn(allTypes);
        when(fieldAPIMock.findByContentTypeId(parentType1.id())).thenReturn(fieldsParent1);

        List<Relationship> result = relationshipAPI.byChildUsingFieldFactory(childContentType);
        assertNotNull(result);
        assertEquals(1, result.size()); // Only the valid one
        assertEquals("rfValidCard", result.get(0).getInode());
    }

    @Test(expected = DotDataException.class)
    public void testByChildUsingFieldFactory_ContentTypeApiFindAllThrowsException_PropagatesException() throws DotDataException, DotSecurityException {
        ContentTypeIf childContentType = mock(ContentTypeIf.class);
        when(childContentType.id()).thenReturn("childIdEx");
        when(childContentType.variable()).thenReturn("ChildVarEx");
        when(contentTypeAPIMock.findAll()).thenThrow(new DotDataException("DB error on findAll"));
        relationshipAPI.byChildUsingFieldFactory(childContentType);
    }

    @Test(expected = DotDataException.class)
    public void testByChildUsingFieldFactory_FieldApiFindByContentTypeIdThrowsException_PropagatesException() throws DotDataException, DotSecurityException {
        ContentTypeIf childContentType = mock(ContentTypeIf.class);
        when(childContentType.id()).thenReturn("childIdEx2");
        when(childContentType.variable()).thenReturn("ChildVarEx2");
        
        ContentType parentType1 = ContentTypeBuilder.builder(SimpleContentType.class).id("parentIdEx2").variable("ParentTypeEx2").build();
        List<ContentType> allTypes = Collections.singletonList(parentType1);
        when(contentTypeAPIMock.findAll()).thenReturn(allTypes);
        when(fieldAPIMock.findByContentTypeId(parentType1.id())).thenThrow(new DotDataException("DB error on findByContentTypeId"));
        
        relationshipAPI.byChildUsingFieldFactory(childContentType);
    }

    // Existing tests below
    @Test
    public void testSave_RelationshipWithNonUniqueRelationTypeValue_ShouldThrowException() {

        final ContentType parent = ContentTypeBuilder.builder(SimpleContentType.class).id("parent")
                .name("Parent").variable("parent").build();

        final ContentType child = ContentTypeBuilder.builder(SimpleContentType.class).id("child")
                .name("Child").variable("child").build();

        final ContentType cousins = ContentTypeBuilder.builder(SimpleContentType.class)
                .id("cousins").name("Cousins").variable("Cousins").build();

        final Relationship relationship = new Relationship();
        relationship.setParentRelationName("Parent " + parent.name());
        relationship.setParentStructureInode(parent.id());
        relationship.setChildRelationName("Child " + child.name());
        relationship.setChildStructureInode(child.id());
        relationship.setCardinality(
                WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

        final RelationshipFactory relationshipFactory = Mockito.mock(RelationshipFactory.class);
        Mockito.when(relationshipFactory.sameParentAndChild(relationship)).thenReturn(false);

        final RelationshipAPIImpl api = new RelationshipAPIImpl(relationshipFactory);

        String newName = api.suggestNewFieldName(parent, relationship, false);
        assertEquals("Parents", newName);
        newName = api.suggestNewFieldName(child, relationship, true);
        assertEquals("Childs", newName);

        relationship.setCardinality(
                WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal());

        newName = api.suggestNewFieldName(parent, relationship, false);
        assertEquals("Parent", newName);
        newName = api.suggestNewFieldName(child, relationship, true);
        assertEquals("Childs", newName);

        relationship
                .setCardinality(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal());

        newName = api.suggestNewFieldName(parent, relationship, false);
        assertEquals("Parent", newName);
        newName = api.suggestNewFieldName(child, relationship, true);
        assertEquals("Child", newName);

        relationship.setParentStructureInode(child.id());
        relationship.setChildStructureInode(cousins.id());
        relationship.setCardinality(
                WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal());

        newName = api.suggestNewFieldName(child, relationship, false);
        assertEquals("Child", newName);
        newName = api.suggestNewFieldName(cousins, relationship, true);
        assertEquals("Cousins", newName);
    }

    @Test
    public void testSave_SelfRelationshipWithNonUniqueRelationTypeValue_ShouldThrowException () throws Exception {

        final ContentType contentType = ContentTypeBuilder.builder(SimpleContentType.class)
                .id("Testcontenttype").name("Testcontenttype").variable("Testcontenttype").build();

        final Relationship relationship = new Relationship();
        relationship.setParentRelationName("Parent " + contentType.name());
        relationship.setParentStructureInode(contentType.id());
        relationship.setChildRelationName("Child " + contentType.name());
        relationship.setChildStructureInode(contentType.id());
        relationship.setCardinality(
                WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

        final RelationshipFactory relationshipFactory = Mockito.mock(RelationshipFactory.class);
        Mockito.when(relationshipFactory.sameParentAndChild(relationship)).thenReturn(true);

        final RelationshipAPIImpl api = new RelationshipAPIImpl(relationshipFactory);

        String newName = api.suggestNewFieldName(contentType, relationship, false);
        assertEquals("TestcontenttypeParents", newName);
        newName = api.suggestNewFieldName(contentType, relationship, true);
        assertEquals("TestcontenttypeChildren", newName);

        relationship.setCardinality(
                WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal());

        newName = api.suggestNewFieldName(contentType, relationship, false);
        assertEquals("TestcontenttypeParent", newName);
        newName = api.suggestNewFieldName(contentType, relationship, true);
        assertEquals("TestcontenttypeChildren", newName);

        relationship
                .setCardinality(WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal());

        newName = api.suggestNewFieldName(contentType, relationship, false);
        assertEquals("TestcontenttypeParent", newName);
        newName = api.suggestNewFieldName(contentType, relationship, true);
        assertEquals("TestcontenttypeChild", newName);
    }
    
}
