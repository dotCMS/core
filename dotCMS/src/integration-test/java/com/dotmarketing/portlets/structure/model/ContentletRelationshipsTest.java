package com.dotmarketing.portlets.structure.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.model.User;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author nollymar
 */
@RunWith(DataProviderRunner.class)
public class ContentletRelationshipsTest extends IntegrationTestBase {

    private static ContentletAPI contentletAPI;
    private static ContentTypeAPI contentTypeAPI;
    private static FieldAPI fieldAPI;
    private static RelationshipAPI relationshipAPI;
    private static User user;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        fieldAPI = APILocator.getContentTypeFieldAPI();
        user = APILocator.getUserAPI().getSystemUser();

        contentletAPI = APILocator.getContentletAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(user);
        relationshipAPI = APILocator.getRelationshipAPI();
    }

    public static class TestCase {

        boolean bothSided;

        public TestCase(final boolean bothSided) {
            this.bothSided = bothSided;
        }
    }

    @DataProvider
    public static Object[] testCases() {
        return new TestCase[]{
                //invalid indexed
                new TestCase(false),
                //invalid listed
                new TestCase(true)
        };
    }

    @Test
    @UseDataProvider("testCases")
    public void testGetRelationshipRecords(TestCase testCase)
            throws DotSecurityException, DotDataException {

        final long time = System.currentTimeMillis();

        final String cardinality = String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());

        ContentType childContentType = null;
        ContentType parentContentType = null;

        try {
            parentContentType = contentTypeAPI.save(
                    ContentTypeBuilder.builder(SimpleContentType.class).folder(
                            FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                            .name("parentContentType" + time)
                            .owner(user.getUserId()).build());

            childContentType = contentTypeAPI.save(
                    ContentTypeBuilder.builder(SimpleContentType.class).folder(
                            FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                            .name("childContentType" + time)
                            .owner(user.getUserId()).build());

            Field firstField = FieldBuilder.builder(RelationshipField.class)
                    .name("firstField").variable("firstField")
                    .contentTypeId(parentContentType.id()).values(cardinality)
                    .relationType(parentContentType.variable()).build();

            firstField = fieldAPI.save(firstField, user);

            Field secondField = FieldBuilder.builder(RelationshipField.class)
                    .name("secondField").variable("secondField")
                    .contentTypeId(parentContentType.id()).values(cardinality)
                    .relationType(
                            testCase.bothSided ? parentContentType.variable() + StringPool.PERIOD
                                    + firstField.variable() : childContentType.id()).build();

            secondField = fieldAPI.save(secondField, user);

            final Relationship selfRelationship = relationshipAPI
                    .getRelationshipFromField(firstField, user);

            final Relationship secondRelationship = relationshipAPI
                    .getRelationshipFromField(secondField, user);

            //add legacy contentlet relationship records
            final Relationship legacyRelationship = new Relationship(
                    new StructureTransformer(parentContentType).asStructure(),
                    new StructureTransformer(childContentType).asStructure(),
                    parentContentType.variable(), childContentType.variable(),
                    RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal(), false, false);

            relationshipAPI.save(legacyRelationship);

            final ContentletDataGen contentletDataGen = new ContentletDataGen(
                    parentContentType.id());
            final Contentlet contentlet = contentletDataGen.nextPersisted();

            final ContentletRelationships contentletRelationships = contentletAPI
                    .getAllRelationships(contentlet);

            assertEquals(testCase.bothSided ? 3 : 4,
                    contentletRelationships.getRelationshipsRecords().size());

            final List<ContentletRelationships.ContentletRelationshipRecords> legacyRelationshipRecords = contentletRelationships
                    .getLegacyRelationshipsRecords();
            assertNotNull(legacyRelationshipRecords);
            assertEquals(testCase.bothSided ? 1 : 2, legacyRelationshipRecords.size());

            assertTrue(legacyRelationshipRecords.stream().allMatch(
                    record -> record.getRelationship().equals(legacyRelationship) ? record
                            .isHasParent()
                            : record.getRelationship().equals(selfRelationship) && !record
                                    .isHasParent()));

            //validate field relationships
            assertTrue(contentletRelationships.getRelationshipsRecordsByField(firstField).get(0)
                    .isHasParent());
            assertEquals(selfRelationship,
                    contentletRelationships.getRelationshipsRecordsByField(firstField).get(0)
                            .getRelationship());

            if (testCase.bothSided) {
                assertFalse(
                        contentletRelationships.getRelationshipsRecordsByField(secondField).get(0)
                                .isHasParent());
            } else {
                assertTrue(
                        contentletRelationships.getRelationshipsRecordsByField(secondField).get(0)
                                .isHasParent());
            }

            assertEquals(secondRelationship,
                    contentletRelationships.getRelationshipsRecordsByField(secondField).get(0)
                            .getRelationship());


        } finally {
            if (UtilMethods.isSet(parentContentType) && UtilMethods.isSet(parentContentType.id())) {
                contentTypeAPI.delete(parentContentType);
            }

            if (UtilMethods.isSet(childContentType) && UtilMethods.isSet(childContentType.id())) {
                contentTypeAPI.delete(childContentType);
            }
        }
    }
}
