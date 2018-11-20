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
            parentContentType = ContentTypeBuilder.builder(SimpleContentType.class).folder(
                    FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("parentContentType" + time)
                    .owner(user.getUserId()).build();

            parentContentType = contentTypeAPI.save(parentContentType);

            childContentType = ContentTypeBuilder.builder(SimpleContentType.class).folder(
                    FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("childContentType" + time)
                    .owner(user.getUserId()).build();

            childContentType = contentTypeAPI.save(childContentType);

            Field field = FieldBuilder.builder(RelationshipField.class).name("newRel")
                    .contentTypeId(parentContentType.id())
                    .values(CARDINALITY).relationType(childContentType.variable()).build();

            //One side of the relationship is set parentContentType --> childContentType
            field = fieldAPI.save(field, user);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + field.variable();

            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            assertNotNull(relationship);

            assertNull(relationship.getParentRelationName());
            assertEquals(parentContentType.id(), relationship.getParentStructureInode());
            assertEquals(childContentType.id(), relationship.getChildStructureInode());
            assertEquals(field.variable(), relationship.getChildRelationName());
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
    public void testSaveRelationshipField_when_newFieldIsSetToAnExistingRelationship()
            throws DotSecurityException, DotDataException {
        final long time = System.currentTimeMillis();

        ContentType parentContentType = null;
        ContentType childContentType  = null;

        try {
            parentContentType = ContentTypeBuilder.builder(SimpleContentType.class).folder(
                    FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("parentContentType" + time)
                    .owner(user.getUserId()).build();

            parentContentType = contentTypeAPI.save(parentContentType);

            childContentType = ContentTypeBuilder.builder(SimpleContentType.class).folder(
                    FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("childContentType" + time)
                    .owner(user.getUserId()).build();

            childContentType = contentTypeAPI.save(childContentType);

            //Adding a RelationshipField to the parent
            Field field = FieldBuilder.builder(RelationshipField.class).name("newRel")
                    .contentTypeId(parentContentType.id())
                    .values(CARDINALITY).relationType(childContentType.variable()).build();

            //One side of the relationship is set parentContentType --> childContentType
            field = fieldAPI.save(field, user);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + field.variable();


            //Adding a RelationshipField to the child
            Field secondField = FieldBuilder.builder(RelationshipField.class).name("otherSideRel")
                    .contentTypeId(childContentType.id())
                    .values(CARDINALITY).relationType(fullFieldVar).build();

            //Setting the other side of the relationship childContentType --> parentContentType
            secondField = fieldAPI.save(secondField, user);

            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            assertNotNull(relationship);

            assertEquals(childContentType.id(), relationship.getChildStructureInode());
            assertEquals(field.variable(), relationship.getChildRelationName());
            assertEquals(parentContentType.id(), relationship.getParentStructureInode());
            assertEquals(secondField.variable(), relationship.getParentRelationName());
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

    @Test(expected = DotDataException.class)
    public void testSaveRelationshipField_when_replacingParentInABothSidedRelationship()
            throws DotSecurityException, DotDataException {
        final long time = System.currentTimeMillis();

        ContentType parentContentType    = null;
        ContentType childContentType     = null;
        ContentType newParentContentType = null;


        try {
            parentContentType = ContentTypeBuilder.builder(SimpleContentType.class).folder(
                    FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("parentContentType" + time)
                    .owner(user.getUserId()).build();

            parentContentType = contentTypeAPI.save(parentContentType);

            childContentType = ContentTypeBuilder.builder(SimpleContentType.class).folder(
                    FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("childContentType" + time)
                    .owner(user.getUserId()).build();

            childContentType = contentTypeAPI.save(childContentType);

            newParentContentType = ContentTypeBuilder.builder(SimpleContentType.class).folder(
                    FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                    .name("newParentContentType" + time)
                    .owner(user.getUserId()).build();

            newParentContentType = contentTypeAPI.save(newParentContentType);

            //Adding a RelationshipField to the parent
            Field field = FieldBuilder.builder(RelationshipField.class).name("newRel")
                    .contentTypeId(parentContentType.id())
                    .values(CARDINALITY).relationType(childContentType.variable()).build();

            //One side of the relationship is set parentContentType --> childContentType
            field = fieldAPI.save(field, user);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + field.variable();

            //Adding a RelationshipField to the child
            //Setting the other side of the relationship childContentType --> parentContentType
            Field secondField = FieldBuilder.builder(RelationshipField.class).name("otherSideRel")
                    .contentTypeId(childContentType.id())
                    .values(CARDINALITY).relationType(fullFieldVar).build();

            secondField = fieldAPI.save(secondField, user);

            //Setting a new relationship childContentType --> newParentContentType
            secondField = FieldBuilder.builder(secondField).relationType(newParentContentType.variable()).build();

            fieldAPI.save(secondField, user);
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
    public void testSaveRelationshipField_when_replacingChildInABothSidedRelationship()
            throws DotSecurityException, DotDataException {
        final long time = System.currentTimeMillis();

        ContentType parentContentType   = null;
        ContentType childContentType    = null;
        ContentType newChildContentType = null;

        try {
            parentContentType = ContentTypeBuilder.builder(SimpleContentType.class).folder(
                    FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("parentContentType" + time)
                    .owner(user.getUserId()).build();

            parentContentType = contentTypeAPI.save(parentContentType);

            childContentType = ContentTypeBuilder.builder(SimpleContentType.class).folder(
                    FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("childContentType" + time)
                    .owner(user.getUserId()).build();

            childContentType = contentTypeAPI.save(childContentType);

            newChildContentType = ContentTypeBuilder.builder(SimpleContentType.class).folder(
                    FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                    .name("newChildContentType" + time)
                    .owner(user.getUserId()).build();

            newChildContentType = contentTypeAPI.save(newChildContentType);

            //Adding a RelationshipField to the parent
            Field field = FieldBuilder.builder(RelationshipField.class).name("newRel")
                    .contentTypeId(parentContentType.id()).values(CARDINALITY)
                    .relationType(childContentType.variable()).build();

            //One side of the relationship is set parentContentType --> childContentType
            field = fieldAPI.save(field, user);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + field.variable();

            //Adding a RelationshipField to the child
            //Setting the other side of the relationship childContentType --> parentContentType
            Field secondField = FieldBuilder.builder(RelationshipField.class).name("otherSideRel")
                    .contentTypeId(childContentType.id()).values(CARDINALITY)
                    .relationType(fullFieldVar).build();

            fieldAPI.save(secondField, user);

            //Setting a new relationship parentContentType --> newChildContentType
            field = FieldBuilder.builder(field).relationType(newChildContentType.variable()).build();

            fieldAPI.save(field, user);

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
    public void testSaveBothSidedRelationshipFieldAndRemoveChildField()
            throws DotSecurityException, DotDataException {
        final long time = System.currentTimeMillis();

        ContentType parentContentType = null;
        ContentType childContentType  = null;

        try {
            parentContentType = ContentTypeBuilder.builder(SimpleContentType.class).folder(
                    FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("parentContentType" + time)
                    .owner(user.getUserId()).build();

            parentContentType = contentTypeAPI.save(parentContentType);

            childContentType = ContentTypeBuilder.builder(SimpleContentType.class).folder(
                    FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("childContentType" + time)
                    .owner(user.getUserId()).build();

            childContentType = contentTypeAPI.save(childContentType);

            //Adding a RelationshipField to the parent
            Field field = FieldBuilder.builder(RelationshipField.class).name("newRel")
                    .contentTypeId(parentContentType.id()).values(CARDINALITY)
                    .relationType(childContentType.variable()).build();

            //One side of the relationship is set parentContentType --> childContentType
            field = fieldAPI.save(field, user);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + field.variable();


            //Adding a RelationshipField to the child
            Field secondField = FieldBuilder.builder(RelationshipField.class).name("otherSideRel")
                    .contentTypeId(childContentType.id()).values(CARDINALITY)
                    .relationType(fullFieldVar).build();

            secondField = fieldAPI.save(secondField, user);

            //Removing child field
            fieldAPI.delete(secondField);

            //Getting the one-sided of the relationship parentContentType --> childContentType
            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            assertNotNull(relationship);

            assertEquals(childContentType.id(), relationship.getChildStructureInode());
            assertEquals(field.variable(), relationship.getChildRelationName());
            assertEquals(parentContentType.id(), relationship.getParentStructureInode());
            assertNull(relationship.getParentRelationName());
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
    public void testSaveBothSidedRelationshipFieldAndRemoveParentField()
            throws DotSecurityException, DotDataException {
        final long time = System.currentTimeMillis();

        ContentType parentContentType = null;
        ContentType childContentType  = null;

        try {
            parentContentType = ContentTypeBuilder.builder(SimpleContentType.class).folder(
                    FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("parentContentType" + time)
                    .owner(user.getUserId()).build();

            parentContentType = contentTypeAPI.save(parentContentType);

            childContentType = ContentTypeBuilder.builder(SimpleContentType.class).folder(
                    FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("childContentType" + time)
                    .owner(user.getUserId()).build();

            childContentType = contentTypeAPI.save(childContentType);

            //Adding a RelationshipField to the parent
            Field field = FieldBuilder.builder(RelationshipField.class).name("newRel")
                    .contentTypeId(parentContentType.id()).values(CARDINALITY)
                    .relationType(childContentType.variable()).build();

            //One side of the relationship is set parentContentType --> childContentType
            field = fieldAPI.save(field, user);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + field.variable();

            //Adding a RelationshipField to the child
            Field secondField = FieldBuilder.builder(RelationshipField.class).name("otherSideRel")
                    .contentTypeId(childContentType.id()).values(CARDINALITY)
                    .relationType(fullFieldVar).build();

            secondField = fieldAPI.save(secondField, user);

            //Removing parent field
            fieldAPI.delete(field);

            //Getting the one-sided relationship childContentType --> parentContentType
            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            assertNotNull(relationship);

            assertEquals(parentContentType.id(), relationship.getParentStructureInode());
            assertEquals(secondField.variable(), relationship.getParentRelationName());
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
    public void testOneSidedRelationshipFieldAndRemoveField()
            throws DotSecurityException, DotDataException {
        final long time = System.currentTimeMillis();

        ContentType parentContentType = null;
        ContentType childContentType  = null;

        try {
            parentContentType = ContentTypeBuilder.builder(SimpleContentType.class).folder(
                    FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("parentContentType" + time)
                    .owner(user.getUserId()).build();

            parentContentType = contentTypeAPI.save(parentContentType);

            childContentType = ContentTypeBuilder.builder(SimpleContentType.class).folder(
                    FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("childContentType" + time)
                    .owner(user.getUserId()).build();

            childContentType = contentTypeAPI.save(childContentType);

            //Adding a RelationshipField to the parent
            Field field = FieldBuilder.builder(RelationshipField.class).name("newRel")
                    .contentTypeId(parentContentType.id()).values(CARDINALITY)
                    .relationType(childContentType.variable()).build();

            //One side of the relationship is set parentContentType --> childContentType
            field = fieldAPI.save(field, user);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + field.variable();

            //Removing parent field
            fieldAPI.delete(field);

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
            parentContentType = ContentTypeBuilder.builder(SimpleContentType.class).folder(
                    FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("parentContentType" + time)
                    .owner(user.getUserId()).build();

            parentContentType = contentTypeAPI.save(parentContentType);

            childContentType = ContentTypeBuilder.builder(SimpleContentType.class).folder(
                    FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("childContentType" + time)
                    .owner(user.getUserId()).build();

            childContentType = contentTypeAPI.save(childContentType);

            //Adding a RelationshipField to the parent
            Field field = FieldBuilder.builder(RelationshipField.class).name("newRel")
                    .contentTypeId(parentContentType.id()).values("5")
                    .relationType(childContentType.variable()).build();

            //Trying to save relationship field with invalid input (cardinality=5)
            fieldAPI.save(field, user);

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
    public void testSaveRelationshipFieldBothSidesRequired()
            throws DotSecurityException, DotDataException {
        final long time = System.currentTimeMillis();

        ContentType parentContentType = null;
        ContentType childContentType  = null;
        Relationship relationship;

        try {
            parentContentType = ContentTypeBuilder.builder(SimpleContentType.class).folder(
                    FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("parentContentType" + time)
                    .owner(user.getUserId()).build();

            parentContentType = contentTypeAPI.save(parentContentType);

            childContentType = ContentTypeBuilder.builder(SimpleContentType.class).folder(
                    FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("childContentType" + time)
                    .owner(user.getUserId()).build();

            childContentType = contentTypeAPI.save(childContentType);

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
                    .contentTypeId(childContentType.id()).values(CARDINALITY)
                    .relationType(fullFieldVar).required(true).build();

            secondField = fieldAPI.save(secondField, user);

            field = fieldAPI.find(field.id());

            relationship = relationshipAPI.byTypeValue(fullFieldVar);

            assertNotNull(relationship);
            assertFalse(relationship.isChildRequired());
            assertTrue(relationship.isParentRequired());
            assertTrue(secondField.required());
            assertFalse(field.required());

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
    public void testSaveOneSidedRelationship_when_theChildContentTypeIsRelatedWithAnotherContentType()
            throws DotSecurityException, DotDataException {
        final long time = System.currentTimeMillis();
        ContentType parentContentType = null;
        final ContentType existingContentType = contentTypeAPI.find("Youtube");
        try {
            parentContentType = ContentTypeBuilder.builder(SimpleContentType.class).folder(
                    FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name("parentContentType" + time)
                    .owner(user.getUserId()).build();

            parentContentType = contentTypeAPI.save(parentContentType);

            Field field = FieldBuilder.builder(RelationshipField.class).name("newRel")
                    .contentTypeId(parentContentType.id()).values(CARDINALITY)
                    .relationType(existingContentType.variable()).build();

            //One side of the relationship is set parentContentType --> Youtube
            field = fieldAPI.save(field, user);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + field.variable();

            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            assertNotNull(relationship);
            assertNull(relationship.getParentRelationName());
            assertEquals(parentContentType.id(), relationship.getParentStructureInode());
            assertEquals(existingContentType.id(), relationship.getChildStructureInode());
            assertEquals(field.variable(), relationship.getChildRelationName());
            assertEquals(1, relationship.getCardinality());
            assertEquals(fullFieldVar, relationship.getRelationTypeValue());
        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }
        }
    }
}
