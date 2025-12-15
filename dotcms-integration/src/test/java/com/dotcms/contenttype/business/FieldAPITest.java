package com.dotcms.contenttype.business;

import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.BASE_TYPE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.CONTENT_TYPE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.IDENTIFIER;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.INODE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.LIVE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.MOD_DATE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.TITLE;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.URL_MAP;
import static com.dotcms.content.elasticsearch.constants.ESMappingConstants.WORKING;
import static com.dotcms.contenttype.business.ContentTypeAPIImpl.TYPES_AND_FIELDS_VALID_VARIABLE_REGEX;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.ARCHIVED_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.FOLDER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.HOST_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.LOCKED_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.MOD_USER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.OWNER_KEY;
import static com.dotmarketing.portlets.contentlet.model.Contentlet.TITLE_IMAGE_KEY;
import static com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY.MANY_TO_ONE;
import static com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY.ONE_TO_MANY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.content.elasticsearch.business.ESMappingAPIImpl;
import com.dotcms.content.elasticsearch.business.IndicesInfo;
import com.dotcms.contenttype.business.FieldAPITest.UniqueConstraintTestCase.DuplicateType;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableCategoryField;
import com.dotcms.contenttype.model.field.ImmutableColumnField;
import com.dotcms.contenttype.model.field.ImmutableDateField;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.field.ImmutableHostFolderField;
import com.dotcms.contenttype.model.field.ImmutableKeyValueField;
import com.dotcms.contenttype.model.field.ImmutableRowField;
import com.dotcms.contenttype.model.field.ImmutableStoryBlockField;
import com.dotcms.contenttype.model.field.ImmutableTabDividerField;
import com.dotcms.contenttype.model.field.ImmutableTagField;
import com.dotcms.contenttype.model.field.ImmutableTextAreaField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotValidationException;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotDataValidationException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import io.vavr.Tuple2;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
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
        Config.setProperty(FieldAPI.FULLSCREEN_FIELD_FEATURE_FLAG, true);




    }

    public static class TestCase {

        boolean indexed;
        boolean listed;
        String cardinality;

        public TestCase(final boolean indexed, final boolean listed, final String cardinality) {
            this.indexed = indexed;
            this.listed = listed;
            this.cardinality = cardinality;
        }
    }

    @DataProvider
    public static Object[] testCases(){
        return new TestCase[]{
                //invalid indexed
                new TestCase(false, false, CARDINALITY),
                //invalid listed
                new TestCase(true, true, CARDINALITY),
                //invalid cardinality
                new TestCase(true, false, "5")
        };
    }

    public static class UniqueConstraintTestCase {

        boolean selfRelated;
        enum DuplicateType {
            //The parent field exists as a parent field in another relationship
            PARENT_FIELD,
            //The child field exists as a child field in another relationship
            CHILD_FIELD,
            //Both means that the parent field exists as a child field in another relationship
            //Only applies for self joins
            MIX_FROM_PARENT,
            //Both means that the child field exists as a parent field in another relationship
            //Only applies for self joins
            MIX_FROM_CHILD
        }
        DuplicateType duplicateType;

        public UniqueConstraintTestCase(final boolean selfRelated, final DuplicateType duplicateType) {
            this.selfRelated = selfRelated;
            this.duplicateType = duplicateType;
        }
    }

    @DataProvider
    public static Object[] UniqueConstraintTestCase(){
        return new UniqueConstraintTestCase[]{
                new UniqueConstraintTestCase(true, DuplicateType.PARENT_FIELD),
                new UniqueConstraintTestCase(true, DuplicateType.CHILD_FIELD),
                new UniqueConstraintTestCase(true, DuplicateType.MIX_FROM_PARENT),
                new UniqueConstraintTestCase(true, DuplicateType.MIX_FROM_CHILD),
                new UniqueConstraintTestCase(false, DuplicateType.PARENT_FIELD),
                new UniqueConstraintTestCase(false, DuplicateType.CHILD_FIELD)
        };
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

            final Field parentTypeRelationshipField = createAndSaveRelationshipField("newRel",
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
    public void testSaveOneSidedManyToOneRelationshipField_returns_newRelationshipObject()
            throws DotSecurityException, DotDataException {
        final long time = System.currentTimeMillis();

        ContentType parentContentType = null;
        ContentType childContentType = null;
        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType" + time);
            childContentType = createAndSaveSimpleContentType("childContentType" + time);

            final Field parentTypeRelationshipField = createAndSaveRelationshipField(
                    "newRel",
                    parentContentType.id(), childContentType.variable(),
                    String.valueOf(MANY_TO_ONE.ordinal()));

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField
                            .variable();

            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            assertNotNull(relationship);

            assertNull(relationship.getChildRelationName());
            assertEquals(parentContentType.id(), relationship.getChildStructureInode());
            assertEquals(childContentType.id(), relationship.getParentStructureInode());
            assertEquals(parentTypeRelationshipField.variable(),
                    relationship.getParentRelationName());
            assertEquals(String.valueOf(ONE_TO_MANY.ordinal()),
                    Integer.toString(relationship.getCardinality()));
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
            final Field parentTypeRelationshipField = createAndSaveRelationshipField("newRel",
                    parentContentType.id(), childContentType.variable(), CARDINALITY);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField.variable();

            //Adding a RelationshipField to the child
            final Field childTypeRelationshipField = createAndSaveRelationshipField("otherSideRel",
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
            final Field parentTypeRelationshipField = createAndSaveRelationshipField("newRel",
                    parentContentType.id(), childContentType.variable(), CARDINALITY);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField.variable();

            //Adding a RelationshipField to the child
            //Setting the other side of the relationship childContentType --> parentContentType
            //Adding a RelationshipField to the child
            final Field childTypeRelationshipField = createAndSaveRelationshipField("otherSideRel",
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
            final Field parentTypeRelationshipField = createAndSaveRelationshipField("newRel",
                    parentContentType.id(), childContentType.variable(), CARDINALITY);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField.variable();

            //Adding a RelationshipField to the child
            //Setting the other side of the relationship childContentType --> parentContentType
            createAndSaveRelationshipField("otherSideRel",
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
            final Field parentTypeRelationshipField = createAndSaveRelationshipField("newRel",
                    parentContentType.id(), childContentType.variable(), CARDINALITY);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField.variable();

            //Adding a RelationshipField to the child
            final Field childTypeRelationshipField = createAndSaveRelationshipField("otherSideRel",
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
            final Field parentTypeRelationshipField = createAndSaveRelationshipField("newRel",
                    parentContentType.id(), childContentType.variable(), CARDINALITY);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField.variable();

            //Adding a RelationshipField to the child
            final Field childTypeRelationshipField = createAndSaveRelationshipField("otherSideRel",
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
            final Field parentTypeRelationshipField = createAndSaveRelationshipField("newRel",
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
            assertTrue(relationship.isChildRequired());
            assertFalse(relationship.isParentRequired());
            assertFalse(secondField.required());
            assertTrue(field.required());
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
        final ContentType existingContentType = TestDataUtils.getBlogLikeContentType(); // Any CT would do.
        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType" + time);

            //One side of the relationship is set parentContentType --> Youtube
            final Field parentTypeRelationshipField = createAndSaveRelationshipField("newRel",
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

            final Field parentTypeRelationshipField = createAndSaveRelationshipField("newRel",
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

            final Field parentTypeRelationshipField = createAndSaveRelationshipField("newRel",
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
            assertTrue(secondField.required());
            assertFalse(updatedField.required());
            assertFalse(relationship.isChildRequired());
            assertTrue(relationship.isParentRequired());
            assertEquals(newCardinality, updatedField.values());
            assertEquals(newCardinality, String.valueOf(relationship.getCardinality()));

            //new cardinality should have been updated on the child field and required field is set to false
            assertEquals(newCardinality, secondField.values());

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
    public void testUpdateSelfRelatedFields_shouldNotChangeRelationNames()
            throws DotDataException, DotSecurityException {

        ContentType parentContentType = null;

        final long time = System.currentTimeMillis();
        final String newCardinality = String.valueOf(RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal());

        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType" + time);

            final Field childField = createAndSaveRelationshipField("newRel",
                    parentContentType.id(), parentContentType.variable(), CARDINALITY);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + childField.variable();

            //Adding the other side of the relationship
            Field parentField = FieldBuilder.builder(RelationshipField.class).name("otherSideRel")
                    .contentTypeId(parentContentType.id()).values(CARDINALITY)
                    .relationType(fullFieldVar).required(true).build();

            parentField = fieldAPI.save(parentField, user);

            //Update parent field
            fieldAPI
                    .save(FieldBuilder.builder(parentField).values(newCardinality).build(), user);

            //Update child field
            fieldAPI
                    .save(FieldBuilder.builder(childField).values(newCardinality).build(), user);

            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            assertEquals(childField.variable(), relationship.getChildRelationName());
            assertEquals(parentField.variable(), relationship.getParentRelationName());

        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }
        }
    }

    @Test(expected = DotDataException.class)
    @UseDataProvider("testCases")
    public void testValidateRelationshipField_shouldThrowAnException(TestCase testCase)
            throws DotSecurityException, DotDataException {


        final long time = System.currentTimeMillis();
        final ContentType type = createAndSaveSimpleContentType("testContentType" + time);
        try {
            final Field field = FieldBuilder.builder(RelationshipField.class)
                    .name("testField" + time)
                    .contentTypeId(type.id()).values(testCase.cardinality).indexed(testCase.indexed)
                    .listed(testCase.listed).build();

            fieldAPI.save(field, user);
        }finally {
            contentTypeAPI.delete(type);
        }
    }

    @Test(expected = DotDataValidationException.class)
    public void testValidateRelationshipFieldWithDash_shouldThrowAnException()
            throws DotSecurityException, DotDataException {


        final long time = System.currentTimeMillis();
        final ContentType type = createAndSaveSimpleContentType("testContentType" + time);
        try {
            final Field field = FieldBuilder.builder(RelationshipField.class)
                    .name("testField" + time)
                    .contentTypeId(type.id()).values(CARDINALITY).indexed(false)
                    .listed(false).variable("testContentType-videos").build();

            fieldAPI.save(field, user);
        }finally {
            contentTypeAPI.delete(type);
        }
    }

    @UseDataProvider("UniqueConstraintTestCase")
    @Test(expected = DotValidationException.class)
    public void testSaveRelationshipWithDuplicateFieldsShouldThrowAnException(UniqueConstraintTestCase testCase)
            throws DotDataException, DotSecurityException {
        final long time = System.currentTimeMillis();

        ContentType parentContentType = null;
        ContentType childContentType  = null;
        Relationship secondRelationship = null;
        Field relationshipField;
        final String fieldName = "newRel";
        final String cardinality = (testCase.duplicateType == DuplicateType.CHILD_FIELD || testCase.duplicateType == DuplicateType.MIX_FROM_PARENT) ? String
                .valueOf(ONE_TO_MANY.ordinal()) : String.valueOf(MANY_TO_ONE.ordinal());

        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType" + time);
            childContentType = testCase.selfRelated? parentContentType: createAndSaveSimpleContentType("childContentType" + time);

            relationshipField = createAndSaveRelationshipField(fieldName, parentContentType.id(), childContentType.variable(), cardinality);

            switch(testCase.duplicateType){
                case CHILD_FIELD:
                case PARENT_FIELD:
                    secondRelationship = new Relationship(parentContentType, childContentType, relationshipField);
                    break;
                case MIX_FROM_CHILD:
                    relationshipField = FieldBuilder.builder(relationshipField).values(String
                            .valueOf(ONE_TO_MANY.ordinal())).build();
                    secondRelationship = new Relationship(parentContentType, childContentType, relationshipField);
                    break;
                case MIX_FROM_PARENT:
                    relationshipField = FieldBuilder.builder(relationshipField).values(String
                                    .valueOf(MANY_TO_ONE.ordinal())).build();
                    secondRelationship = new Relationship(parentContentType, childContentType, relationshipField);
                    break;
            }

            relationshipAPI.save(secondRelationship);

        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }

            if (!testCase.selfRelated && UtilMethods.isSet(childContentType) && UtilMethods
                    .isSet(childContentType.id())) {
                contentTypeAPI.delete(childContentType);
            }
        }


    }


    @Test(expected = DotDataValidationException.class)
    public void testSave_UpdateExistingFieldWithDifferentContentType_ShouldThrowException()
        throws DotSecurityException, DotDataException {


        final long time = System.currentTimeMillis();
        final ContentType type = createAndSaveSimpleContentType("testContentType" + time);
        try {
            final Field field = FieldBuilder.builder(BinaryField.class)
                .name("Photo")
                .id("07cfbc2c-47de-4c78-a411-176fe8bb24a5")
                .contentTypeId(type.id())
                .values(CARDINALITY)
                .indexed(false)
                .listed(false)
                .variable("photo")
                .fixed(true)
                .build();
            fieldAPI.save(field, user);
        }finally {
            contentTypeAPI.delete(type);
        }
    }

    @DataProvider
    public static Object[] dataProviderSaveInvalidVariable() {
        return new Tuple2[] {
                // actual, should fail
                new Tuple2<>("123", true),
                new Tuple2<>("_123", true),
                new Tuple2<>("_123a", true),
                new Tuple2<>("asd123asd", false),
                new Tuple2<>("Asfsdf", false),
                new Tuple2<>("aa123", false)
        };
    }

    @Test
    @UseDataProvider("dataProviderSaveInvalidVariable")
    public void testSave_InvalidVariable_ShouldThrowException(final Tuple2<String, Boolean> testCase)
            throws DotSecurityException, DotDataException {

        final String variableTestCase = testCase._1;
        final boolean shouldFail = testCase._2;

        final long time = System.currentTimeMillis();
        final ContentType type = createAndSaveSimpleContentType("testContentType" + time);
        try {
            final Field field = FieldBuilder.builder(TextField.class)
                    .name("testField")
                    .contentTypeId(type.id())
                    .indexed(false)
                    .listed(false)
                    .variable(variableTestCase)
                    .fixed(true)
                    .build();
            fieldAPI.save(field, user);
            assertFalse(shouldFail);
        } catch (DotDataValidationException e) {
            assertTrue(shouldFail);
        } finally {
            contentTypeAPI.delete(type);
        }
    }


    @DataProvider
    public static Object[] dataProviderTypeNames() {
        return new String[] {
                "123",
                "123abc",
                "_123a",
                "asd123asd",
                "Asfsdf",
                "aa123",
                "This is a field",
                "Field && ,,,..**==} name~~~__",
                "__"
        };
    }

    @Test
    @UseDataProvider("dataProviderTypeNames")
    public void testSave_InvalidName_ShouldThrowException(final String fieldName)
            throws DotSecurityException, DotDataException {

        final long time = System.currentTimeMillis();
        final ContentType type = createAndSaveSimpleContentType("testContentType" + time);
        try {
            Field field = FieldBuilder.builder(TextField.class)
                    .name(fieldName)
                    .contentTypeId(type.id())
                    .indexed(false)
                    .listed(false)
                    .fixed(true)
                    .build();
            field = fieldAPI.save(field, user);

            Assert.assertTrue(field.variable().matches(TYPES_AND_FIELDS_VALID_VARIABLE_REGEX));
        } finally {
            contentTypeAPI.delete(type);
        }
    }

    private ContentType createAndSaveSimpleContentType(final String name) throws DotSecurityException, DotDataException {
        return contentTypeAPI.save(ContentTypeBuilder.builder(SimpleContentType.class).folder(
                FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name(name)
                .owner(user.getUserId()).build());
    }


    private Field createAndSaveRelationshipField(final String fieldName, final String parentTypeId,
                                                 final String childTypeVar, final String cardinality)
            throws DotSecurityException, DotDataException {

        final Field field = FieldBuilder.builder(RelationshipField.class).name(fieldName)
                .contentTypeId(parentTypeId).values(cardinality)
                .relationType(childTypeVar).build();

        //One side of the relationship is set parentContentType --> childContentType
        return fieldAPI.save(field, user);
    }

    @DataProvider
    public static Object[] dataProviderFieldVariableKeys() {
        return new String[] {
                "__myKey",
                "dottype.",
                "dot:type",
                "dot-type",
                "*dottype*",
                "$my^^&key%",
        };
    }

    @Test
    @UseDataProvider("dataProviderFieldVariableKeys")
    public void testSaveFieldVariable_KeyWithSpecialChars_ShouldSucceed(final String fieldVarKey)
            throws DotSecurityException, DotDataException {

        final long time = System.currentTimeMillis();
        final ContentType type = new ContentTypeDataGen().nextPersisted();
        try {
            Field field = FieldBuilder.builder(TextField.class)
                    .name("field"+time)
                    .contentTypeId(type.id())
                    .indexed(false)
                    .listed(false)
                    .fixed(true)
                    .build();
            field = fieldAPI.save(field, user);

            final FieldVariable variable = ImmutableFieldVariable.builder().fieldId(field.inode())
                    .name(fieldVarKey).key(fieldVarKey).value("value").userId(user.getUserId())
                    .build();

            fieldAPI.save(variable, user);

            boolean anyMatch = field.fieldVariables().stream()
                    .anyMatch((var)->var.key().equals(fieldVarKey));

            Assert.assertTrue("Incorrect var key", anyMatch);
        } finally {
            contentTypeAPI.delete(type);
        }
    }


    @DataProvider(format = "%m: %p[0]")
    public static Object[] dataProviderGraphQLCompatibleFields() {
        final GraphQLFieldNameCompatibilityTestCase caseModDateCompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseModDateCompatible.fieldName = MOD_DATE;
        caseModDateCompatible.fieldType = ImmutableDateField.class;
        caseModDateCompatible.testCaseName = "caseModDateCompatible";

        final GraphQLFieldNameCompatibilityTestCase caseTitleCompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseTitleCompatible.fieldName = TITLE;
        caseTitleCompatible.fieldType = ImmutableTextField.class;
        caseTitleCompatible.testCaseName = "caseTitleCompatible";

        final GraphQLFieldNameCompatibilityTestCase caseTitleImageCompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseTitleImageCompatible.fieldName = TITLE_IMAGE_KEY;
        caseTitleImageCompatible.fieldType = ImmutableBinaryField.class;
        caseTitleImageCompatible.testCaseName = "caseTitleImageCompatible";

        final GraphQLFieldNameCompatibilityTestCase caseContentTypeCompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseContentTypeCompatible.fieldName = CONTENT_TYPE;
        caseContentTypeCompatible.fieldType = ImmutableTextField.class;
        caseContentTypeCompatible.testCaseName = "caseContentTypeCompatible";

        final GraphQLFieldNameCompatibilityTestCase caseBaseTypeCompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseBaseTypeCompatible.fieldName = BASE_TYPE;
        caseBaseTypeCompatible.fieldType = ImmutableTextField.class;
        caseBaseTypeCompatible.testCaseName = "caseBaseTypeCompatible";

        final GraphQLFieldNameCompatibilityTestCase caseLiveCompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseLiveCompatible.fieldName = LIVE;
        caseLiveCompatible.fieldType = ImmutableTextField.class;
        caseLiveCompatible.testCaseName = "caseLiveCompatible";

        final GraphQLFieldNameCompatibilityTestCase caseWorkingCompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseWorkingCompatible.fieldName = WORKING;
        caseWorkingCompatible.fieldType = ImmutableTextField.class;
        caseWorkingCompatible.testCaseName = "caseWorkingCompatible";

        final GraphQLFieldNameCompatibilityTestCase caseArchivedCompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseArchivedCompatible.fieldName = ARCHIVED_KEY;
        caseArchivedCompatible.fieldType = ImmutableTextField.class;
        caseArchivedCompatible.testCaseName = "caseArchivedCompatible";

        final GraphQLFieldNameCompatibilityTestCase caseLockedCompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseLockedCompatible.fieldName = LOCKED_KEY;
        caseLockedCompatible.fieldType = ImmutableTextField.class;
        caseLockedCompatible.testCaseName = "caseLockedCompatible";

        final GraphQLFieldNameCompatibilityTestCase caseIdentifierCompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseIdentifierCompatible.fieldName = IDENTIFIER;
        caseIdentifierCompatible.fieldType = ImmutableTextField.class;
        caseIdentifierCompatible.testCaseName = "caseIdentifierCompatible";

        final GraphQLFieldNameCompatibilityTestCase caseInodeCompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseInodeCompatible.fieldName = INODE;
        caseInodeCompatible.fieldType = ImmutableTextField.class;
        caseInodeCompatible.testCaseName = "caseInodeCompatible";

        final GraphQLFieldNameCompatibilityTestCase caseUrlMapCompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseUrlMapCompatible.fieldName = URL_MAP;
        caseUrlMapCompatible.fieldType = ImmutableTextField.class;
        caseUrlMapCompatible.testCaseName = "caseUrlMapCompatible";

        return new GraphQLFieldNameCompatibilityTestCase[] {
                caseModDateCompatible,
                caseTitleCompatible,
                caseTitleImageCompatible,
                caseContentTypeCompatible,
                caseBaseTypeCompatible,
                caseLiveCompatible,
                caseWorkingCompatible,
                caseArchivedCompatible,
                caseLockedCompatible,
                caseIdentifierCompatible,
                caseInodeCompatible,
                caseUrlMapCompatible,
        };
    }

    @DataProvider(format = "%m: %p[0]")
    public static Object[] dataProviderGraphQLIncompatibleFields() {

        final GraphQLFieldNameCompatibilityTestCase caseModDateIncompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseModDateIncompatible.fieldName = MOD_DATE;
        caseModDateIncompatible.fieldType = ImmutableBinaryField.class;
        caseModDateIncompatible.testCaseName = "caseModDateIncompatible";

        final GraphQLFieldNameCompatibilityTestCase caseTitleIncompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseTitleIncompatible.fieldName = TITLE;
        caseTitleIncompatible.fieldType = ImmutableCategoryField.class;
        caseTitleIncompatible.testCaseName = "caseTitleIncompatible";

        final GraphQLFieldNameCompatibilityTestCase caseTitleImageIncompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseTitleImageIncompatible.fieldName = TITLE_IMAGE_KEY;
        caseTitleImageIncompatible.fieldType = ImmutableHostFolderField.class;
        caseTitleImageIncompatible.testCaseName = "caseTitleImageIncompatible";

        final GraphQLFieldNameCompatibilityTestCase caseContentTypeIncompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseContentTypeIncompatible.fieldName = CONTENT_TYPE;
        caseContentTypeIncompatible.fieldType = ImmutableHostFolderField.class;
        caseContentTypeIncompatible.testCaseName = "caseContentTypeIncompatible";

        final GraphQLFieldNameCompatibilityTestCase caseBaseTypeIncompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseBaseTypeIncompatible.fieldName = BASE_TYPE;
        caseBaseTypeIncompatible.fieldType = ImmutableKeyValueField.class;
        caseBaseTypeIncompatible.testCaseName = "caseBaseTypeIncompatible";

        final GraphQLFieldNameCompatibilityTestCase caseLiveIncompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseLiveIncompatible.fieldName = LIVE;
        caseLiveIncompatible.fieldType = ImmutableCategoryField.class;
        caseLiveIncompatible.testCaseName = "caseLiveIncompatible";

        final GraphQLFieldNameCompatibilityTestCase caseWorkingIncompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseWorkingIncompatible.fieldName = WORKING;
        caseWorkingIncompatible.fieldType = ImmutableCategoryField.class;
        caseWorkingIncompatible.testCaseName = "caseWorkingIncompatible";

        final GraphQLFieldNameCompatibilityTestCase caseArchivedIncompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseArchivedIncompatible.fieldName = ARCHIVED_KEY;
        caseArchivedIncompatible.fieldType = ImmutableCategoryField.class;
        caseArchivedIncompatible.testCaseName = "caseArchivedIncompatible";

        final GraphQLFieldNameCompatibilityTestCase caseLockedIncompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseLockedIncompatible.fieldName = LOCKED_KEY;
        caseLockedIncompatible.fieldType = ImmutableCategoryField.class;
        caseLockedIncompatible.testCaseName = "caseLockedIncompatible";

        final GraphQLFieldNameCompatibilityTestCase caseConLanguageIncompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseConLanguageIncompatible.fieldName = "conLanguage";
        caseConLanguageIncompatible.fieldType = ImmutableCategoryField.class;
        caseConLanguageIncompatible.testCaseName = "caseConLanguageIncompatible";

        final GraphQLFieldNameCompatibilityTestCase caseConLanguageParentIncompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseConLanguageParentIncompatible.fieldName = "conLanguage";
        caseConLanguageParentIncompatible.fieldType = ImmutableTagField.class;
        caseConLanguageParentIncompatible.testCaseName = "caseConLanguageParentIncompatible";

        final GraphQLFieldNameCompatibilityTestCase caseIdentifierIncompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseIdentifierIncompatible.fieldName = IDENTIFIER;
        caseIdentifierIncompatible.fieldType = ImmutableCategoryField.class;
        caseIdentifierIncompatible.testCaseName = "caseIdentifierIncompatible";

        final GraphQLFieldNameCompatibilityTestCase caseInodeIncompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseInodeIncompatible.fieldName = INODE;
        caseInodeIncompatible.fieldType = ImmutableCategoryField.class;
        caseInodeIncompatible.testCaseName = "caseInodeIncompatible";

        final GraphQLFieldNameCompatibilityTestCase caseHostIncompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseHostIncompatible.fieldName = HOST_KEY;
        caseHostIncompatible.fieldType = ImmutableCategoryField.class;
        caseHostIncompatible.testCaseName = "caseHostIncompatible";

        final GraphQLFieldNameCompatibilityTestCase caseHostParentIncompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseHostParentIncompatible.fieldName = HOST_KEY;
        caseHostParentIncompatible.fieldType = ImmutableTagField.class;
        caseHostParentIncompatible.testCaseName = "caseHostParentIncompatible";

        final GraphQLFieldNameCompatibilityTestCase caseFolderIncompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseFolderIncompatible.fieldName = FOLDER_KEY;
        caseFolderIncompatible.fieldType = ImmutableCategoryField.class;
        caseFolderIncompatible.testCaseName = "caseFolderIncompatible";

        final GraphQLFieldNameCompatibilityTestCase caseFolder_ParentIncompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseFolder_ParentIncompatible.fieldName = "conLanguage";
        caseFolder_ParentIncompatible.fieldType = ImmutableTagField.class;
        caseFolder_ParentIncompatible.testCaseName = "caseFolder_ParentIncompatible";

        final GraphQLFieldNameCompatibilityTestCase caseUrlMapIncompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseUrlMapIncompatible.fieldName = URL_MAP;
        caseUrlMapIncompatible.fieldType = ImmutableCategoryField.class;
        caseUrlMapIncompatible.testCaseName = "caseUrlMapIncompatible";

        final GraphQLFieldNameCompatibilityTestCase caseOwnerIncompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseOwnerIncompatible.fieldName = OWNER_KEY;
        caseOwnerIncompatible.fieldType = ImmutableCategoryField.class;
        caseOwnerIncompatible.testCaseName = "caseOwnerIncompatible";

        final GraphQLFieldNameCompatibilityTestCase caseOwner_ParentIncompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseOwner_ParentIncompatible.fieldName = OWNER_KEY;
        caseOwner_ParentIncompatible.fieldType = ImmutableTagField.class;
        caseOwner_ParentIncompatible.testCaseName = "caseOwner_ParentIncompatible";

        final GraphQLFieldNameCompatibilityTestCase caseModUserIncompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseModUserIncompatible.fieldName = MOD_USER_KEY;
        caseModUserIncompatible.fieldType = ImmutableCategoryField.class;
        caseModUserIncompatible.testCaseName = "caseModUserIncompatible";

        final GraphQLFieldNameCompatibilityTestCase caseModUser_ParentIncompatible = new GraphQLFieldNameCompatibilityTestCase();
        caseModUser_ParentIncompatible.fieldName = MOD_USER_KEY;
        caseModUser_ParentIncompatible.fieldType = ImmutableTagField.class;
        caseModUser_ParentIncompatible.testCaseName = "caseModUser_ParentIncompatible";

        return new GraphQLFieldNameCompatibilityTestCase[] {
                caseModDateIncompatible,
                caseTitleIncompatible,
                caseTitleImageIncompatible,
                caseContentTypeIncompatible,
                caseConLanguageIncompatible,
                caseConLanguageParentIncompatible,
                caseBaseTypeIncompatible,
                caseLiveIncompatible,
                caseIdentifierIncompatible,
                caseInodeIncompatible,
                caseHostIncompatible,
                caseHostParentIncompatible,
                caseFolderIncompatible,
                caseFolder_ParentIncompatible,
                caseUrlMapIncompatible,
                caseOwnerIncompatible,
                caseOwner_ParentIncompatible,
                caseModUserIncompatible,
                caseModUser_ParentIncompatible
        };
    }

    /**
     * Method to test: {@link FieldAPIImpl#save(Field, User)}
     * <p>
     * Given scenario: A {@link ContentType} with a `name` that would normally result in a incompatible `variable` (generated by our API based on the name)
     * with the current GraphQL Schema.
     * <p>
     * Expected result: The generated (by our API) Content Type variable is different than the incompatible `variable`
     */

    @Test
    @UseDataProvider("dataProviderGraphQLIncompatibleFields")
    public void test_SaveField_GivenTypeName_GeneratedVariableIsDifferentThanProvidedOne(
            final GraphQLFieldNameCompatibilityTestCase testCase)
            throws DotSecurityException, DotDataException {

        final ContentType type = new ContentTypeDataGen().nextPersisted();
        try {
            final ContentType contentType = new ContentTypeDataGen().nextPersisted();
            final Field field = createField(contentType, testCase.fieldName, null,
                    testCase.fieldType);

            Assert.assertNotEquals(testCase.fieldName, field.variable());
        } finally {
            contentTypeAPI.delete(type);
        }
    }

    /**
     * Method to test: {@link FieldAPIImpl#save(Field, User)}
     * <p>
     * Given scenario: A {@link ContentType} with a `name` that would normally result in a Compatible `variable` (generated by our API based on the name)
     * with the current GraphQL Schema.
     * <p>
     * Expected result: The generated (by our API) Content Type variable is the same than the compatible `variable`
     */

    @Test
    @UseDataProvider("dataProviderGraphQLCompatibleFields")
    public void test_SaveField_GivenTypeName_GeneratedVariableIsSameAsProvidedOne(
            final GraphQLFieldNameCompatibilityTestCase testCase)
            throws DotSecurityException, DotDataException {

        final ContentType type = new ContentTypeDataGen().nextPersisted();
        try {
            final ContentType contentType = new ContentTypeDataGen().nextPersisted();
            final Field field = createField(contentType, testCase.fieldName, null,
                    testCase.fieldType);
            if (FieldFactoryImpl.RESERVED_FIELD_VARS.contains(testCase.fieldName.toLowerCase())){
                Assert.assertEquals(testCase.fieldName+1, field.variable());
            } else {
                Assert.assertEquals(testCase.fieldName, field.variable());
            }
        } finally {
            contentTypeAPI.delete(type);
        }
    }

    /**
     * Method to test: {@link FieldAPIImpl#save(Field, User)}
     * <p>
     * Given scenario: A {@link ContentType} with an incompatible `variable` with the current GraphQL Schema.
     * <p>
     * Expected result: {@link DotDataException} thrown
     */

    @UseDataProvider("dataProviderGraphQLIncompatibleFields")
    public void test_SaveField_GivenGraphQLIncompatibleVariable_ShouldSave(
            final GraphQLFieldNameCompatibilityTestCase testCase)
            throws DotSecurityException, DotDataException {

        final ContentType type = new ContentTypeDataGen().nextPersisted();
        try {
            final ContentType contentType = new ContentTypeDataGen().nextPersisted();
            // passing the invalid graphql variable as both name and variable of the field
            final Field field = createField(contentType, testCase.fieldName,
                    testCase.fieldName, testCase.fieldType);
            Assert.assertEquals(testCase.fieldName, field.variable());
        } finally {
            contentTypeAPI.delete(type);
        }
    }

    /**
     * Method to test: {@link FieldAPIImpl#save(Field, User)}
     * <p>
     * Given scenario: A {@link ContentType} with a compatible `variable` with the current GraphQL Schema.
     * <p>
     * Expected result: Field saved with given variable
     */

    @Test
    @UseDataProvider("dataProviderGraphQLCompatibleFields")
    public void test_SaveField_GivenGraphQLCompatibleVariable_ShouldSave(
            final GraphQLFieldNameCompatibilityTestCase testCase)
            throws DotSecurityException, DotDataException {

        final ContentType type = new ContentTypeDataGen().nextPersisted();
        try {
            final ContentType contentType = new ContentTypeDataGen().nextPersisted();
            // passing the invalid graphql variable as both name and variable of the field
            final Field field = createField(contentType, testCase.fieldName,
                    testCase.fieldName, testCase.fieldType);

            Assert.assertEquals(testCase.fieldName, field.variable());

        } finally {
            contentTypeAPI.delete(type);
        }
    }

    /**
     * <b>Method to test:</b> {@link FieldAPIImpl#save(Field, User)}
     * <p>
     * <b>Given scenario:</b> A new {@link Field} is saved and marked as `indexed=false`
     * <p>
     * <b>Expected result:</b> The field should be saved without ES mapping
     */
    @Test
    public void test_SaveNewNoIndexedField_ShouldNotAddESMapping()
            throws DotSecurityException, DotDataException, IOException {
        final long time = System.currentTimeMillis();
        final ContentType type = new ContentTypeDataGen().nextPersisted();
        try {
            Field field = FieldBuilder.builder(DateField.class)
                    .name("field" + time)
                    .variable("field" + time)
                    .contentTypeId(type.id())
                    .indexed(false)
                    .build();
            field = fieldAPI.save(field, user);

            final IndicesInfo legacyIndicesInfo = APILocator.getIndiciesAPI().loadLegacyIndices();
            final ESMappingAPIImpl mappingAPI = new ESMappingAPIImpl();

            //verify mapping on working index
            Map<String, Object> mapping = mappingAPI
                    .getFieldMappingAsMap(legacyIndicesInfo.getWorking(),
                            (type.variable() + StringPool.PERIOD + field.variable()));
            assertFalse(UtilMethods.isSet(mapping));

            //verify mapping on live index
            mapping = mappingAPI
                    .getFieldMappingAsMap(legacyIndicesInfo.getLive(),
                            (type.variable() + StringPool.PERIOD + field.variable()));
            assertFalse(UtilMethods.isSet(mapping));
        }finally{
            ContentTypeDataGen.remove(type);
        }
    }

    /**
     * <b>Method to test:</b> {@link FieldAPIImpl#save(Field, User)}
     * <p>
     * <b>Given scenario:</b> A new {@link DateField} is saved and marked as `indexed=true`
     * <p>
     * <b>Expected result:</b> The field should be saved and mapped in ES with `type=date`
     */
    @Test
    public void test_SaveNewIndexedField_ShouldAddESMapping()
            throws DotSecurityException, DotDataException, IOException {
        final long time = System.currentTimeMillis();
        final ContentType type = new ContentTypeDataGen().nextPersisted();
        try {
            Field field = FieldBuilder.builder(DateField.class)
                    .name("field" + time)
                    .variable("field" + time)
                    .contentTypeId(type.id())
                    .indexed(true)
                    .build();
            field = fieldAPI.save(field, user);

            final IndicesInfo legacyIndicesInfo = APILocator.getIndiciesAPI().loadLegacyIndices();
            final ESMappingAPIImpl mappingAPI = new ESMappingAPIImpl();

            //verify mapping on working index
            Map<String, String> mapping = (Map<String, String>) mappingAPI
                    .getFieldMappingAsMap(legacyIndicesInfo.getWorking(),
                            (type.variable() + StringPool.PERIOD + field.variable())
                                    .toLowerCase()).get(field.variable());
            assertTrue(UtilMethods.isSet(mapping.get("type")));
            assertEquals("date", mapping.get("type"));

            //verify mapping on live index
            mapping = (Map<String, String>) mappingAPI
                    .getFieldMappingAsMap(legacyIndicesInfo.getLive(),
                            (type.variable() + StringPool.PERIOD + field.variable())
                                    .toLowerCase()).get(field.variable());
            assertTrue(UtilMethods.isSet(mapping.get("type")));
            assertEquals("date", mapping.get("type"));

            //validate _dotraw fields
            mapping = (Map<String, String>) mappingAPI
                    .getFieldMappingAsMap(legacyIndicesInfo.getLive(),
                            (type.variable() + StringPool.PERIOD + field.variable() + "_dotraw")
                                    .toLowerCase()).get(field.variable() + "_dotraw");
            assertTrue(UtilMethods.isSet(mapping.get("type")));
            assertEquals("keyword", mapping.get("type"));

        }finally{
            ContentTypeDataGen.remove(type);
        }
    }

    /**
     * <b>Method to test:</b> {@link FieldAPIImpl#save(Field, User)}
     * <p>
     * <b>Given scenario:</b> A new {@link RelationshipField} is saved and marked as `indexed=true`
     * <p>
     * <b>Expected result:</b> The field should be saved and mapped in ES with `type=keyword`
     */
    @Test
    public void test_SaveNewRelationshipField_ShouldAddESMapping()
            throws DotSecurityException, DotDataException, IOException {
        final long time = System.currentTimeMillis();
        final ContentType type = createAndSaveSimpleContentType("contentType" + time);
        try {
            final Field field = createAndSaveRelationshipField("newRel",
                    type.id(), type.variable(), CARDINALITY);

            final IndicesInfo legacyIndicesInfo = APILocator.getIndiciesAPI().loadLegacyIndices();
            final ESMappingAPIImpl mappingAPI = new ESMappingAPIImpl();

            //verify mapping on working index
            Map<String, String> mapping = (Map<String, String>) mappingAPI
                    .getFieldMappingAsMap(legacyIndicesInfo.getWorking(),
                            (type.variable() + StringPool.PERIOD + field.variable())
                                    .toLowerCase()).get(field.variable().toLowerCase());
            assertTrue(UtilMethods.isSet(mapping.get("type")));
            assertEquals("keyword", mapping.get("type"));

            //verify mapping on live index
            mapping = (Map<String, String>) mappingAPI
                    .getFieldMappingAsMap(legacyIndicesInfo.getLive(),
                            (type.variable() + StringPool.PERIOD + field.variable())
                                    .toLowerCase()).get(field.variable().toLowerCase());
            assertTrue(UtilMethods.isSet(mapping.get("type")));
            assertEquals("keyword", mapping.get("type"));
        }finally{
            ContentTypeDataGen.remove(type);
        }
    }

    /**
     * Method to test: com.dotcms.contenttype.business.FieldAPI#save(com.dotcms.contenttype.model.field.Field, com.liferay.portal.model.User, boolean)
     * Given scenario: Recreate a deleted relationship field but choosing existing relationship, from parent to child
     * Expected result: Should succeed
     */

    @Test
    public void testSaveRelationshipField_GivenRecreatedDeletedRelationshipFieldChoosingExistingRel_SaveShouldSucceed()
            throws DotSecurityException, DotDataException {
        final long time = System.currentTimeMillis();

        ContentType parentContentType = null;
        ContentType childContentType  = null;

        try {
            parentContentType = createAndSaveSimpleContentType("parentContentType" + time);
            childContentType = createAndSaveSimpleContentType("childContentType" + time);

            //Adding a RelationshipField to the parent
            final Field parentTypeRelationshipField = createAndSaveRelationshipField("newRel",
                    parentContentType.id(), childContentType.variable(), CARDINALITY);

            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + parentTypeRelationshipField.variable();

            //Adding a RelationshipField to the child
            final Field childTypeRelationshipField = createAndSaveRelationshipField("otherSideRel",
                    childContentType.id(), fullFieldVar, CARDINALITY);

            final Relationship relationship = relationshipAPI.byTypeValue(fullFieldVar);

            assertNotNull(relationship);

            assertEquals(childContentType.id(), relationship.getChildStructureInode());
            assertEquals(parentTypeRelationshipField.variable(), relationship.getChildRelationName());
            assertEquals(parentContentType.id(), relationship.getParentStructureInode());
            assertEquals(childTypeRelationshipField.variable(), relationship.getParentRelationName());
            assertEquals(CARDINALITY, Integer.toString(relationship.getCardinality()));
            assertEquals(fullFieldVar, relationship.getRelationTypeValue());

            final String parentTypeRelationshipFieldVariable = parentTypeRelationshipField.variable();
            // let's delete the relationship field from the parent content type
            APILocator.getContentTypeFieldAPI().delete(parentTypeRelationshipField);

            final Relationship reFetchedRelationship = relationshipAPI.byTypeValue(fullFieldVar);
            assertNull(reFetchedRelationship.getChildRelationName());

            // let's attempt to re-add the field but choosing the existing relationship
            final Field recreatedParentTypeRelationshipField = createAndSaveRelationshipField("newRel",
                    parentContentType.id(), parentContentType.variable() + "." + parentTypeRelationshipFieldVariable
                    , CARDINALITY);

            assertEquals(recreatedParentTypeRelationshipField.variable(),
                    relationship.getChildRelationName());

        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }

            if (UtilMethods.isSet(childContentType) && UtilMethods.isSet(childContentType.id())) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }

    static class GraphQLFieldNameCompatibilityTestCase {
        String fieldName;
        private Class<? extends Field> fieldType;
        String testCaseName;

        @Override
        public String toString() {
            return testCaseName;
        }
    }

    public static Field createField(final ContentType contentType, final String fieldName,
            final String fieldVariable,
            final Class<? extends Field> fieldType)
            throws DotSecurityException, DotDataException {
        final FieldAPI fieldAPI = APILocator.getContentTypeFieldAPI();
        final FieldBuilder fieldBuilder = getFieldBuilder(fieldType);

        final Field field = fieldBuilder.contentTypeId(contentType.id())
                .name(fieldName)
                .variable(fieldVariable)
                .build();

        return fieldAPI.save(field, APILocator.systemUser());

    }

    private static FieldBuilder getFieldBuilder(final Class<? extends Field> fieldType) {
        try {
            return (FieldBuilder) fieldType.getMethod("builder").invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            Logger.error("Couldn't invoke builder", e);
        }

        return null;
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link FieldAPI#isFullScreenField(Field)}</li>
     *     <li><b>Given Scenario: </b>Create four fields: Tab, Row, Column, and Text Area, and
     *     ask the API if the Text Area field can be displayed in full screen.</li>
     *     <li><b>Expected Result: </b>The API returns {@code true} for the Text Area field.</li>
     * </ul>
     *
     * @throws DotSecurityException Failed to save the test Content Type.
     * @throws DotDataException     Failed to save the test Content Type.
     */
    @Test
    public void test_textarea_fullscreen_field()
            throws DotSecurityException, DotDataException {
        ContentType type = createAndSaveSimpleContentType("fieldTest" + UUIDGenerator.shorty());
        contentTypeAPI.save(type);
        List<Field> fields = new ArrayList<>(type.fields());
        final Field tab = ImmutableTabDividerField.builder().variable("tabField1").name("tabField1").contentTypeId(type.id()).build();
        final Field row = ImmutableRowField.builder().variable("rowField1").name("rowField1").contentTypeId(type.id()).build();
        final Field column = ImmutableColumnField.builder().variable("columnField1").name("columnField1").contentTypeId(type.id()).build();
        final Field textArea = ImmutableTextAreaField.builder().variable("testfield").name("testfield").contentTypeId(type.id()).build();

        fields.add(tab);
        fields.add(row);
        fields.add(column);
        fields.add(textArea);
        type = contentTypeAPI.save(type, fields);
        Field rowField = type.fieldMap().get("testfield");

        assertTrue(fieldAPI.isFullScreenField(rowField));
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link FieldAPI#isFullScreenField(Field)}</li>
     *     <li><b>Given Scenario: </b>Create four fields: Tab, Row, Column, and Story Block, and
     *     ask the API if the Story Block field can be displayed in full screen.</li>
     *     <li><b>Expected Result: </b>The API returns {@code true} for the Story Block field.</li>
     * </ul>
     *
     * @throws DotSecurityException Failed to save the test Content Type.
     * @throws DotDataException     Failed to save the test Content Type.
     */
    @Test
    public void test_block_fullscreen_field()
            throws DotSecurityException, DotDataException {
        ContentType type = createAndSaveSimpleContentType("fieldTest" + UUIDGenerator.shorty());
        contentTypeAPI.save(type);
        List<Field> fields = new ArrayList<>(type.fields());
        final Field tab = ImmutableTabDividerField.builder().variable("tabField1").name("tabField1").contentTypeId(type.id()).build();
        final Field row = ImmutableRowField.builder().variable("rowField1").name("rowField1").contentTypeId(type.id()).build();
        final Field column = ImmutableColumnField.builder().variable("columnField1").name("columnField1").contentTypeId(type.id()).build();
        final Field storyblock = ImmutableStoryBlockField.builder().variable("testfield").name("testfield").contentTypeId(type.id()).build();

        fields.add(tab);
        fields.add(row);
        fields.add(column);
        fields.add(storyblock);
        type = contentTypeAPI.save(type, fields);
        Field rowField = type.fieldMap().get("testfield");

        assertTrue(fieldAPI.isFullScreenField(rowField));
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link FieldAPI#isFullScreenField(Field)}</li>
     *     <li><b>Given Scenario: </b>Create five fields: Tab, Row, Column 1, Column 2, and Story
     *     Block, and ask the API if the Story Block field CANNOT be displayed in full screen in
     *     this layout as it's not the only field on its tab.</li>
     *     <li><b>Expected Result: </b>The API returns {@code false} for the Story Block field.</li>
     * </ul>
     *
     * @throws DotSecurityException Failed to save the test Content Type.
     * @throws DotDataException     Failed to save the test Content Type.
     */
    @Test
    public void test_multicolumn_row_is_not_full_screen_field()
            throws DotSecurityException, DotDataException {
        ContentType type = createAndSaveSimpleContentType("fieldTest" + UUIDGenerator.shorty());
        contentTypeAPI.save(type);
        List<Field> fields = new ArrayList<>(type.fields());
        final Field tab = ImmutableTabDividerField.builder().variable("tabField1").name("tabField1").contentTypeId(type.id()).build();
        final Field row = ImmutableRowField.builder().variable("rowField1").name("rowField1").contentTypeId(type.id()).build();

        final Field column = ImmutableColumnField.builder().variable("columnField1").name("columnField1").contentTypeId(type.id()).build();
        final Field column2 = ImmutableColumnField.builder().variable("columnField2").name("columnField2").contentTypeId(type.id()).build();

        final Field storyblock = ImmutableStoryBlockField.builder().variable("testfield").name("testfield").contentTypeId(type.id()).build();

        fields.add(tab);
        fields.add(row);
        fields.add(column);
        fields.add(column2);
        fields.add(storyblock);
        type = contentTypeAPI.save(type, fields);
        Field rowField = type.fieldMap().get("testfield");

        assertFalse(fieldAPI.isFullScreenField(rowField));
    }

    /**
     * <ul>
     *     <li><b>Method to test: </b>{@link FieldAPI#isFullScreenField(Field)}</li>
     *     <li><b>Given Scenario: </b>Create three fields: Row, Column, and Story Block, and ask
     *     the API if the Story Block field can be displayed in full screen.</li>
     *     <li><b>Expected Result: </b>The API returns {@code true} for the Story Block field.</li>
     * </ul>
     *
     * @throws DotSecurityException Failed to save the test Content Type.
     * @throws DotDataException     Failed to save the test Content Type.
     */
    @Test
    public void first_field_is_full_screen_field_if_no_tab()
            throws DotSecurityException, DotDataException {
        ContentType type = createAndSaveSimpleContentType("fieldTest" + UUIDGenerator.shorty());
        contentTypeAPI.save(type);
        List<Field> fields = new ArrayList<>(type.fields());
        final Field row = ImmutableRowField.builder().variable("rowField1").name("rowField1").contentTypeId(type.id()).build();
        final Field column = ImmutableColumnField.builder().variable("columnField1").name("columnField1").contentTypeId(type.id()).build();
        final Field storyblock = ImmutableStoryBlockField.builder().variable("testfield").name("testfield").contentTypeId(type.id()).build();

        fields.add(row);
        fields.add(column);
        fields.add(storyblock);
        type = contentTypeAPI.save(type, fields);
        Field rowField = type.fieldMap().get("testfield");

        assertTrue(fieldAPI.isFullScreenField(rowField));
    }

}
