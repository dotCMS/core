package com.dotcms.contenttype.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import java.util.Date;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

public class FieldAPITest extends IntegrationTestBase {

    private static FieldAPI fieldAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static RelationshipAPI relationshipAPI;
    private static User user;
    private final static String CARDINALITY = String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

    @BeforeClass
    public static void prepare() throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        user      = APILocator.getUserAPI().getSystemUser();
        fieldAPI  = APILocator.getContentTypeFieldAPI();

        contentTypeAPI  = APILocator.getContentTypeAPI(user);
        relationshipAPI = APILocator.getRelationshipAPI();
    }

    @Test
    public void getFieldVariablesForField() throws Exception {
        // make sure its cached. see https://github.com/dotCMS/dotCMS/issues/2465
        ContentType contentType = null;
        try {

            final String uuid = UUIDGenerator.generateUuid();
            contentType = ContentTypeBuilder.builder(SimpleContentType.class).folder(
                    FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("FieldAPITest_" + uuid)
                    .owner(user.getUserId()).build();

            contentType = contentTypeAPI.save(contentType);

            Field field = FieldBuilder.builder(TextField.class).name("title")
                    .contentTypeId(contentType.id()).required(true).listed(true)
                    .indexed(true).sortOrder(1).readOnly(false).fixed(false).searchable(true)
                    .build();

            field = fieldAPI.save(field, user);
            final FieldVariable fv = ImmutableFieldVariable.builder().fieldId(field.inode())
                    .name("variable1").key("variable1").value("value").userId(user.getUserId())
                    .modDate(new Date()).build();
            fieldAPI.save(fv, user);

            // this should make it live in cache
            List<FieldVariable> list = fieldAPI.find(field.inode()).fieldVariables();
            assertEquals(1, list.size());
            assertEquals(list.get(0).key(), fv.key());
            assertEquals(list.get(0).value(), fv.value());

            FieldVariable fg = ImmutableFieldVariable.builder().fieldId(field.inode())
                    .name("variable2").key("variable2").value("value").userId(user.getUserId())
                    .modDate(new Date()).build();
            fieldAPI.save(fg, user);

            list = fieldAPI.find(field.inode()).fieldVariables();
            assertEquals(2, list.size());
            assertEquals(list.get(0).key(), fv.key());
            assertEquals(list.get(0).value(), fv.value());
            assertEquals(list.get(1).key(), fg.key());
            assertEquals(list.get(1).value(), fg.value());

        } finally {

            if (UtilMethods.isSet(contentType) && UtilMethods.isSet(contentType.id())) {
                contentTypeAPI.delete(contentType);
            }
        }
    }

    @Test
    public void testSaveOneSidedRelationshipField_returns_newRelationshipObject()
            throws DotSecurityException, DotDataException {
        final long time = System.currentTimeMillis();
        ContentType parentContentType = null;
        ContentType childContentType  = null;
        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType" + time);
            childContentType = createAndSaveSimpleContentType("childContentType" + time);

            final Field parentTypeRelationshipField = createAndSaveManyToManyRelationshipField("newRel",
                    parentContentType.id(), childContentType.variable(), CARDINALITY);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField.variable();

            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            assertNotNull(relationship);

            assertNull(relationship.getParentRelationName());
            assertEquals(parentContentType.id(), relationship.getParentStructureInode());
            assertEquals(childContentType.id(), relationship.getChildStructureInode());
            assertEquals(parentTypeRelationshipField.variable(), relationship.getChildRelationName());
            assertEquals(CARDINALITY, Integer.toString(relationship.getCardinality()));
            assertEquals(fullFieldVar, relationship.getRelationTypeValue());
        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }

            if (UtilMethods.isSet(childContentType) && UtilMethods.isSet(childContentType.id())) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    @Test
    public void testSaveRelationshipField_when_newFieldIsSetToAnExistingRelationship_SaveShouldSucceed()
            throws DotSecurityException, DotDataException {
        final long time = System.currentTimeMillis();

        ContentType parentContentType = null;
        ContentType childContentType  = null;

        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType" + time);
            childContentType = createAndSaveSimpleContentType("childContentType" + time);

            //Adding a RelationshipField to the parent
            final Field parentTypeRelationshipField = createAndSaveManyToManyRelationshipField("newRel",
                    parentContentType.id(), childContentType.variable(), CARDINALITY);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField.variable();

            //Adding a RelationshipField to the child
            final Field childTypeRelationshipField = createAndSaveManyToManyRelationshipField("otherSideRel",
                    childContentType.id(), fullFieldVar, CARDINALITY);

            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            assertNotNull(relationship);

            assertEquals(childContentType.id(), relationship.getChildStructureInode());
            assertEquals(parentTypeRelationshipField.variable(), relationship.getChildRelationName());
            assertEquals(parentContentType.id(), relationship.getParentStructureInode());
            assertEquals(childTypeRelationshipField.variable(), relationship.getParentRelationName());
            assertEquals(CARDINALITY, Integer.toString(relationship.getCardinality()));
            assertEquals(fullFieldVar, relationship.getRelationTypeValue());

        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }

            if (UtilMethods.isSet(childContentType) && UtilMethods.isSet(childContentType.id())) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    @Test(expected = DotDataException.class)
    public void testSaveRelationshipField_when_replacingParentInABothSidedRelationship_ShouldThrowException()
            throws DotSecurityException, DotDataException {
        final long time = System.currentTimeMillis();

        ContentType parentContentType    = null;
        ContentType childContentType     = null;
        ContentType newParentContentType = null;


        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType" + time);
            childContentType = createAndSaveSimpleContentType("childContentType" + time);
            newParentContentType = createAndSaveSimpleContentType("newParentContentType" + time);

            //Adding a RelationshipField to the parent
            //One side of the relationship is set parentContentType --> childContentType
            final Field parentTypeRelationshipField = createAndSaveManyToManyRelationshipField("newRel",
                    parentContentType.id(), childContentType.variable(), CARDINALITY);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField.variable();

            //Adding a RelationshipField to the child
            //Setting the other side of the relationship childContentType --> parentContentType
            //Adding a RelationshipField to the child
            final Field childTypeRelationshipField = createAndSaveManyToManyRelationshipField("otherSideRel",
                    childContentType.id(), fullFieldVar, CARDINALITY);

            //Setting a new relationship childContentType --> newParentContentType
            final Field mutatedChildTypeRelationshipField = FieldBuilder.builder(childTypeRelationshipField)
                    .relationType(newParentContentType.variable()).build();

            fieldAPI.save(mutatedChildTypeRelationshipField, user);
        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }

            if (UtilMethods.isSet(childContentType) && UtilMethods.isSet(childContentType.id())) {
                contentTypeAPI.delete(childContentType);
            }

            if (UtilMethods.isSet(newParentContentType) && UtilMethods.isSet(newParentContentType.id())) {
                contentTypeAPI.delete(newParentContentType);
            }
        }
    }

    @Test(expected = DotDataException.class)
    public void testSaveRelationshipField_when_replacingChildInABothSidedRelationship_ShouldThrowException()
            throws DotSecurityException, DotDataException {
        final long time = System.currentTimeMillis();

        ContentType parentContentType   = null;
        ContentType childContentType    = null;
        ContentType newChildContentType = null;

        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType" + time);
            childContentType = createAndSaveSimpleContentType("childContentType" + time);
            newChildContentType = createAndSaveSimpleContentType("newChildContentType" + time);

            //Adding a RelationshipField to the parent
            final Field parentTypeRelationshipField = createAndSaveManyToManyRelationshipField("newRel",
                    parentContentType.id(), childContentType.variable(), CARDINALITY);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField.variable();

            //Adding a RelationshipField to the child
            //Setting the other side of the relationship childContentType --> parentContentType
            createAndSaveManyToManyRelationshipField("otherSideRel",
                    childContentType.id(), fullFieldVar, CARDINALITY);

            //Setting a new relationship parentContentType --> newChildContentType
            final Field mutatedParentTypeRelationshipField = FieldBuilder.builder(parentTypeRelationshipField)
                    .relationType(newChildContentType.variable()).build();

            fieldAPI.save(mutatedParentTypeRelationshipField, user);

        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }

            if (UtilMethods.isSet(childContentType) && UtilMethods.isSet(childContentType.id())) {
                contentTypeAPI.delete(childContentType);
            }

            if (UtilMethods.isSet(newChildContentType) && UtilMethods.isSet(newChildContentType.id())) {
                contentTypeAPI.delete(newChildContentType);
            }
        }
    }


    @Test
    public void test_GivenBothSidedRelationshipField_WhenRemovingChildField_RelationshipShouldBeGettable()
            throws DotSecurityException, DotDataException {
        final long time = System.currentTimeMillis();

        ContentType parentContentType = null;
        ContentType childContentType  = null;

        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType" + time);
            childContentType = createAndSaveSimpleContentType("childContentType" + time);

            //Adding a RelationshipField to the parent
            final Field parentTypeRelationshipField = createAndSaveManyToManyRelationshipField("newRel",
                    parentContentType.id(), childContentType.variable(), CARDINALITY);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField.variable();

            //Adding a RelationshipField to the child
            final Field childTypeRelationshipField = createAndSaveManyToManyRelationshipField("otherSideRel",
                    childContentType.id(), fullFieldVar, CARDINALITY);

            //Removing child field
            fieldAPI.delete(childTypeRelationshipField);

            //Getting the one-sided of the relationship parentContentType --> childContentType
            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            assertNotNull(relationship);

            assertEquals(childContentType.id(), relationship.getChildStructureInode());
            assertEquals(parentTypeRelationshipField.variable(), relationship.getChildRelationName());
            assertEquals(parentContentType.id(), relationship.getParentStructureInode());
            assertNull(relationship.getParentRelationName());
            assertEquals(CARDINALITY, Integer.toString(relationship.getCardinality()));
            assertEquals(fullFieldVar, relationship.getRelationTypeValue());

        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }

            if (UtilMethods.isSet(childContentType) && UtilMethods.isSet(childContentType.id())) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    @Test
    public void test_GivenBothSidedRelationshipField_WhenRemovingParentField_RelationshipShouldBeGettable()
            throws DotSecurityException, DotDataException {
        final long time = System.currentTimeMillis();

        ContentType parentContentType = null;
        ContentType childContentType  = null;

        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType" + time);
            childContentType = createAndSaveSimpleContentType("childContentType" + time);

            //Adding a RelationshipField to the parent
            final Field parentTypeRelationshipField = createAndSaveManyToManyRelationshipField("newRel",
                    parentContentType.id(), childContentType.variable(), CARDINALITY);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField.variable();

            //Adding a RelationshipField to the child
            final Field childTypeRelationshipField = createAndSaveManyToManyRelationshipField("otherSideRel",
                    childContentType.id(), fullFieldVar, CARDINALITY);

            //Removing parent field
            fieldAPI.delete(parentTypeRelationshipField);

            //Getting the one-sided relationship childContentType --> parentContentType
            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            assertNotNull(relationship);

            assertEquals(parentContentType.id(), relationship.getParentStructureInode());
            assertEquals(childTypeRelationshipField.variable(), relationship.getParentRelationName());
            assertEquals(childContentType.id(), relationship.getChildStructureInode());
            assertNull(relationship.getChildRelationName());
            assertEquals(1, relationship.getCardinality());
            assertEquals(fullFieldVar, relationship.getRelationTypeValue());

        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }

            if (UtilMethods.isSet(childContentType) && UtilMethods.isSet(childContentType.id())) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    @Test
    public void test_GivenOneSidedRelationshipField_WhenRemovingField_RelationshipShouldBeWiped()
            throws DotSecurityException, DotDataException {
        final long time = System.currentTimeMillis();

        ContentType parentContentType = null;
        ContentType childContentType  = null;

        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType" + time);
            childContentType = createAndSaveSimpleContentType("childContentType" + time);

            //Adding a RelationshipField to the parent
            final Field parentTypeRelationshipField = createAndSaveManyToManyRelationshipField("newRel",
                    parentContentType.id(), childContentType.variable(), CARDINALITY);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField.variable();

            //Removing parent field
            fieldAPI.delete(parentTypeRelationshipField);

            final Relationship result = relationshipAPI.byTypeValue(fullFieldVar);

            //the relationship shouldn't exist
            assertTrue(!UtilMethods.isSet(result));

        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }

            if (UtilMethods.isSet(childContentType) && UtilMethods.isSet(childContentType.id())) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    @Test(expected = DotDataException.class)
    public void testSavedRelationshipFieldWithInvalidCardinality()
            throws DotSecurityException, DotDataException {
        final long time = System.currentTimeMillis();

        ContentType parentContentType = null;
        ContentType childContentType  = null;

        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType" + time);
            childContentType = createAndSaveSimpleContentType("childContentType" + time);

            //Adding a RelationshipField to the parent
            createAndSaveManyToManyRelationshipField("newRel",
                    parentContentType.id(), childContentType.variable(), "5");

        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }

            if (UtilMethods.isSet(childContentType) && UtilMethods.isSet(childContentType.id())) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }


    @Test
    public void testSaveRelationshipFieldBothSidesRequired_ShouldSucceed()
            throws DotSecurityException, DotDataException {
        final long time = System.currentTimeMillis();
        final String newCardinality = String.valueOf(RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal());

        ContentType parentContentType = null;
        ContentType childContentType  = null;
        Relationship relationship;

        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType" + time);
            childContentType = createAndSaveSimpleContentType("childContentType" + time);

            //Adding a RelationshipField to the parent
            Field field = FieldBuilder.builder(RelationshipField.class).name("newRel")
                    .contentTypeId(parentContentType.id()).values(CARDINALITY)
                    .relationType(childContentType.variable()).required(true).build();


            field = fieldAPI.save(field, user);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + field.variable();

            relationship = relationshipAPI.byTypeValue(fullFieldVar);

            assertNotNull(relationship);
            assertTrue(relationship.isChildRequired());
            assertFalse(relationship.isParentRequired());
            assertTrue(field.required());

            //Adding a RelationshipField to the child
            Field secondField = FieldBuilder.builder(RelationshipField.class).name("otherSideRel")
                    .contentTypeId(childContentType.id()).values(newCardinality)
                    .relationType(fullFieldVar).required(true).build();

            secondField = fieldAPI.save(secondField, user);

            field = fieldAPI.find(field.id());

            relationship = relationshipAPI.byTypeValue(fullFieldVar);

            assertNotNull(relationship);
            assertFalse(relationship.isChildRequired());
            assertTrue(relationship.isParentRequired());
            assertTrue(secondField.required());
            assertFalse(field.required());
            assertEquals(newCardinality, secondField.values());
            assertEquals(newCardinality, field.values());
            assertEquals(newCardinality, String.valueOf(relationship.getCardinality()));


        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }

            if (UtilMethods.isSet(childContentType) && UtilMethods.isSet(childContentType.id())) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    @Test
    public void testSaveOneSidedRelationship_when_theChildContentTypeIsRelatedWithAnotherContentType_ShouldSucceed()
            throws DotSecurityException, DotDataException {
        final long time = System.currentTimeMillis();
        ContentType parentContentType = null;
        final ContentType existingContentType = contentTypeAPI.find("Youtube");
        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType" + time);

            //One side of the relationship is set parentContentType --> Youtube
            final Field parentTypeRelationshipField = createAndSaveManyToManyRelationshipField("newRel",
                    parentContentType.id(), existingContentType.variable(), CARDINALITY);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField.variable();

            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            assertNotNull(relationship);
            assertNull(relationship.getParentRelationName());
            assertEquals(parentContentType.id(), relationship.getParentStructureInode());
            assertEquals(existingContentType.id(), relationship.getChildStructureInode());
            assertEquals(parentTypeRelationshipField.variable(), relationship.getChildRelationName());
            assertEquals(CARDINALITY, Integer.toString(relationship.getCardinality()));
            assertEquals(fullFieldVar, relationship.getRelationTypeValue());
        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }
        }
    }

    @Test
    public void testUpdateOneSidedRelationshipField_shouldSucceed()
            throws DotDataException, DotSecurityException {

        ContentType parentContentType = null;
        ContentType childContentType  = null;

        final long time = System.currentTimeMillis();
        final String newCardinality = String.valueOf(RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal());

        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType" + time);
            childContentType = createAndSaveSimpleContentType("childContentType" + time);

            final Field parentTypeRelationshipField = createAndSaveManyToManyRelationshipField("newRel",
                    parentContentType.id(), childContentType.variable(), CARDINALITY);

            final Field updatedField = fieldAPI
                    .save(FieldBuilder.builder(parentTypeRelationshipField).values(newCardinality)
                            .required(true).build(), user);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField.variable();

            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            assertNotNull(updatedField);
            assertEquals(parentTypeRelationshipField.id(), updatedField.id());
            assertEquals(newCardinality, updatedField.values());
            assertEquals(newCardinality, String.valueOf(relationship.getCardinality()));
            assertTrue(updatedField.required());
            assertTrue(relationship.isChildRequired());
            assertFalse(relationship.isParentRequired());
        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }

            if (UtilMethods.isSet(childContentType) && UtilMethods.isSet(childContentType.id())) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    @Test
    public void testUpdateParentFieldInARelationship_shouldSucceed()
            throws DotDataException, DotSecurityException {

        ContentType parentContentType = null;
        ContentType childContentType  = null;

        final long time = System.currentTimeMillis();
        final String newCardinality = String.valueOf(RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal());

        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType" + time);
            childContentType = createAndSaveSimpleContentType("childContentType" + time);

            final Field parentTypeRelationshipField = createAndSaveManyToManyRelationshipField("newRel",
                    parentContentType.id(), childContentType.variable(), CARDINALITY);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField.variable();

            //Adding a RelationshipField to the child
            Field secondField = FieldBuilder.builder(RelationshipField.class).name("otherSideRel")
                    .contentTypeId(childContentType.id()).values(CARDINALITY)
                    .relationType(fullFieldVar).required(true).build();

            secondField = fieldAPI.save(secondField, user);

            //Update fields (cardinality and required) on the parent field
            final Field updatedField = fieldAPI
                    .save(FieldBuilder.builder(parentTypeRelationshipField).values(newCardinality)
                            .required(true).build(), user);

            secondField = fieldAPI.find(secondField.id());

            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            assertNotNull(updatedField);
            assertEquals(parentTypeRelationshipField.id(), updatedField.id());
            assertTrue(updatedField.required());
            assertTrue(relationship.isChildRequired());
            assertFalse(relationship.isParentRequired());
            assertEquals(newCardinality, updatedField.values());
            assertEquals(newCardinality, String.valueOf(relationship.getCardinality()));

            //new cardinality should have been updated on the child field and required field is set to false
            assertEquals(newCardinality, secondField.values());
            assertFalse(secondField.required());

        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }

            if (UtilMethods.isSet(childContentType) && UtilMethods.isSet(childContentType.id())) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    private ContentType createAndSaveSimpleContentType(final String name) throws DotSecurityException, DotDataException {
        return contentTypeAPI.save(ContentTypeBuilder.builder(SimpleContentType.class).folder(
                FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name(name)
                .owner(user.getUserId()).build());
    }


    private Field createAndSaveManyToManyRelationshipField(final String relationshipName, final String parentTypeId,
                                                 final String childTypeVar, final String cardinality)
            throws DotSecurityException, DotDataException {

        final Field field = FieldBuilder.builder(RelationshipField.class).name(relationshipName)
                .contentTypeId(parentTypeId).values(cardinality)
                .relationType(childTypeVar).build();

        //One side of the relationship is set parentContentType --> childContentType
        return fieldAPI.save(field, user);
    }
}
