package com.dotcms.contenttype.business;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.SimpleContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.util.CollectionsUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotValidationException;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.ContentletRelationships.ContentletRelationshipRecords;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.util.DateUtil;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.liferay.portal.model.User;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class RelationshipAPITest extends IntegrationTestBase {

    private static Relationship relationship = null;
    private static Structure structure = null;
    private static User user = null;
    private static RelationshipAPI relationshipAPI = null;
    private static ContentletAPI contentletAPI;
    private static ContentTypeAPI contentTypeAPI = null;
    private static FieldAPI contentTypeFieldAPI = null;

    @BeforeClass
    public static void prepare () throws Exception {

        //Setting web app environment
        IntegrationTestInitService.getInstance().init();

        user = APILocator.systemUser();
        relationshipAPI = APILocator.getRelationshipAPI();
        contentletAPI  = APILocator.getContentletAPI();
        contentTypeAPI = APILocator.getContentTypeAPI(user);
        contentTypeFieldAPI = APILocator.getContentTypeFieldAPI();

        structure = new Structure();
        structure.setFixed(false);
        structure.setVelocityVarName("ITStructure" + System.currentTimeMillis());
        structure.setName("IT-Structure-"+ System.currentTimeMillis());
        StructureFactory.saveStructure(structure);

        relationship = new Relationship();
        relationship.setParentRelationName("Parent");
        relationship.setChildRelationName("Child");
        relationship.setRelationTypeValue("IT-Parent-Child" + System.currentTimeMillis());
        relationship.setParentStructureInode(structure.getInode());
        relationship.setChildStructureInode(structure.getInode());
        relationshipAPI.create(relationship);
    }

    @AfterClass
    public static void cleanUp () throws DotDataException {
        //The current connection is useless after the Test that throws exceptions.
        //Need to close it, so the API opens a new one when cleaning up
        DbConnectionFactory.closeConnection();

        if (relationship != null) {
            relationshipAPI.delete(relationship);
        }
        //Clean up
        if (structure != null) {
            StructureFactory.deleteStructure(structure);
        }
    }

    /**
     * Test to Attempt to create a Duplicated Relationship (the system should prevent it)
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test (expected = DotDataException.class)
    public void testSave_RelationshipWithNonUniqueRelationTypeValue_ShouldThrowException () throws DotDataException, DotSecurityException {

        Relationship duplicated = new Relationship();
        duplicated.setParentRelationName("ParentDuplicated");
        duplicated.setChildRelationName("ChildDuplicated");
        duplicated.setParentStructureInode(structure.getInode());
        duplicated.setChildStructureInode(structure.getInode());
        duplicated.setRelationTypeValue(relationship.getRelationTypeValue()); //Use existing RelationTypeValue

        //An exception will be thrown because the relationship is duplicated
        //See method declaration, it is expecting this DotDataException
        relationshipAPI.create(duplicated);

    }

    /**
     * Test to Attempt to Update Relationship but modifying the RelationTypeValue (the system should prevent it)
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test (expected = DotValidationException.class)
    public void testSave_RelationshipWithModifiedRelationTypeValue_ShouldThrowException () throws DotDataException, DotSecurityException {

        Relationship modified = new Relationship();
        modified.setParentRelationName("ParentDuplicated");
        modified.setChildRelationName("ChildDuplicated");
        modified.setParentStructureInode(structure.getInode());
        modified.setChildStructureInode(structure.getInode());
        modified.setRelationTypeValue("Modified Relation Type Value");

        //An exception will be thrown because the relationship modified the relationTypeValue
        //See method declaration, it is expecting this DotValidationException
        relationshipAPI.save(modified, relationship.getInode());

    }

    @Test
    public void testconvertRelationshipToRelationshipField_Success() throws DotDataException, DotSecurityException {
        ContentType parentContentType = null;
        ContentType childContentType = null;
        try {
            //Create content types
            parentContentType = createContentType("parentContentType" + System.currentTimeMillis());
            childContentType = createContentType("childContentType" + System.currentTimeMillis());

            //Create Text Fields
            final String titleFieldString = "title";
            Field field = FieldBuilder.builder(TextField.class)
                    .name(titleFieldString)
                    .contentTypeId(parentContentType.id())
                    .build();
            contentTypeFieldAPI.save(field, user);

            field = FieldBuilder.builder(TextField.class)
                    .name(titleFieldString)
                    .contentTypeId(childContentType.id())
                    .build();
            contentTypeFieldAPI.save(field, user);

            //Create an old Relationship
            final Structure parentStructure = new StructureTransformer(parentContentType)
                    .asStructure();
            final Structure childStructure = new StructureTransformer(childContentType)
                    .asStructure();
            final Relationship relationship = new Relationship(parentStructure, childStructure,
                    parentContentType.name(),
                    childContentType.name(), 0, false, false);
            relationshipAPI.save(relationship);

            //Create Contentlet
            Contentlet contentletParent = new ContentletDataGen(parentContentType.id())
                    .setProperty(titleFieldString, "parent Contentlet").next();

            final Contentlet contentletChild = new ContentletDataGen(childContentType.id())
                    .setProperty(titleFieldString, "child Contentlet").nextPersisted();
            final Contentlet contentletChild2 = new ContentletDataGen(childContentType.id())
                    .setProperty(titleFieldString, "child Contentlet 2").nextPersisted();

            //Relate contentlets
            final List<Contentlet> relationshipList = CollectionsUtils
                    .list(contentletChild, contentletChild2);
            migrateRelationshipAndValidate(relationship, contentletParent, relationshipList);

        } finally {
            try {
                if (parentContentType != null) {
                    contentTypeAPI.delete(parentContentType);
                }
                if (childContentType != null) {
                    contentTypeAPI.delete(childContentType);
                }
            }catch (Exception e) {e.printStackTrace();}
        }
    }

    @Test
    public void testconvertRelationshipToRelationshipField_SelfRelated_Success() throws DotDataException, DotSecurityException {
        ContentType parentContentType = null;
        try {
            //Create content types
            parentContentType = createContentType("ContentType" + System.currentTimeMillis());

            //Create Text Fields
            final String titleFieldString = "title";
            final Field field = FieldBuilder.builder(TextField.class)
                    .name(titleFieldString)
                    .contentTypeId(parentContentType.id())
                    .build();
            contentTypeFieldAPI.save(field, user);

            //Create an old Relationship
            final Structure parentStructure = new StructureTransformer(parentContentType)
                    .asStructure();
            final Relationship relationship = new Relationship(parentStructure, parentStructure,
                    "parent" + parentContentType.name(),
                    "child" + parentContentType.name(), 0, false, false);
            relationshipAPI.save(relationship);

            //Create Contentlets
            Contentlet contentletParent = new ContentletDataGen(parentContentType.id())
                    .setProperty(titleFieldString, "parent Contentlet").next();
            final Contentlet contentletChild = new ContentletDataGen(parentContentType.id())
                    .setProperty(titleFieldString, "child Contentlet").nextPersisted();

            //Relate contentlets
            final List<Contentlet> relationshipList = CollectionsUtils.list(contentletChild);
            migrateRelationshipAndValidate(relationship, contentletParent, relationshipList);

        } finally {
            try {
                if (parentContentType != null) {
                    contentTypeAPI.delete(parentContentType);
                }
            }catch (Exception e) {e.printStackTrace();}
        }
    }

    /**
     * <b>Method to test:</b> {@link RelationshipAPI#dbRelatedContent(Relationship, Contentlet)} <p>
     * <b>Given Scenario:</b> A content is related to another (different content types) <p>
     * <b>ExpectedResult:</b> {@link RelationshipAPI#dbRelatedContent(Relationship, Contentlet)} should always return related content
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testDbRelatedContentWhenNonSelfJoinRelationship()
            throws DotDataException, DotSecurityException {
        final ContentType blogContentType = TestDataUtils.getBlogLikeContentType();
        final ContentType commentContentType = TestDataUtils.getCommentsLikeContentType();

        final Field field = FieldBuilder.builder(RelationshipField.class)
                .name(commentContentType.variable())
                .contentTypeId(blogContentType.id())
                .values(String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()))
                .relationType(commentContentType.variable()).required(false).build();

        contentTypeFieldAPI.save(field, user);

        //Creates parent content
        Contentlet blogContentParent = new ContentletDataGen(blogContentType.id())
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .setProperty("title", "blogContentParent")
                .setProperty("urlTitle", "blogContentParent")
                .setProperty("author", "systemUser")
                .setProperty("sysPublishDate", new Date())
                .setProperty("body", "blogBody").next();

        //Creates child content
        final Contentlet commentContentChild = new ContentletDataGen(commentContentType.id())
                .languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .setProperty("title", "commentContentChild")
                .setProperty("urlTitle", "commentContentChild")
                .setProperty("author", "systemUser")
                .setProperty("sysPublishDate", new Date())
                .setProperty("body", "blogBody").nextPersisted();

        //Adds a new relationship between both contentlets
        final Relationship relationship = APILocator.getRelationshipAPI()
                .byContentType(blogContentType).get(0);

        ContentletRelationships contentletRelationships = new ContentletRelationships(
                blogContentParent);
        ContentletRelationshipRecords records = contentletRelationships.new ContentletRelationshipRecords(
                relationship, true);
        records.setRecords(Lists.newArrayList(commentContentChild));
        contentletRelationships.getRelationshipsRecords().add(records);
        blogContentParent = contentletAPI.checkin(blogContentParent, contentletRelationships, null, null, user, false);

        //Validate results from parent side
        List<Contentlet> results = relationshipAPI.dbRelatedContent(relationship, blogContentParent);
        validateDbRelatedContentResults(commentContentChild, results);

        //Validate results from child side
        results = relationshipAPI.dbRelatedContent(relationship, commentContentChild);
        validateDbRelatedContentResults(blogContentParent, results);
    }

    /**
     * <b>Method to test:</b> {@link RelationshipAPI#dbRelatedContent(Relationship, Contentlet)} <p>
     * <b>Given Scenario:</b> A content is related to another (same content type/self-join relationship) <p>
     * <b>ExpectedResult:</b> {@link RelationshipAPI#dbRelatedContent(Relationship, Contentlet)} should always return related content
     * @throws DotDataException
     * @throws DotSecurityException
     */
    @Test
    public void testDbRelatedContentWhenSelfJoinRelationship()
            throws DotDataException, DotSecurityException {
        final ContentType contentType = TestDataUtils.getBlogLikeContentType();
        final Field field = FieldBuilder.builder(RelationshipField.class).name(contentType.variable())
                .contentTypeId(contentType.id())
                .values(String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal()))
                .relationType(contentType.variable()).required(false).build();

        contentTypeFieldAPI.save(field, user);
        final ContentletDataGen dataGen = new ContentletDataGen(contentType.id());

        //Creates parent content
        Contentlet blogContentParent = dataGen.languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .setProperty("title", "blogContentParent")
                .setProperty("urlTitle", "blogContentParent")
                .setProperty("author", "systemUser")
                .setProperty("sysPublishDate", new Date())
                .setProperty("body", "blogBody").next();

        //Creates child content
        final Contentlet blogContentChild = dataGen.languageId(APILocator.getLanguageAPI().getDefaultLanguage().getId())
                .setProperty("title", "blogContentChild")
                .setProperty("urlTitle", "blogContentChild")
                .setProperty("author", "systemUser")
                .setProperty("sysPublishDate", new Date())
                .setProperty("body", "blogBody").nextPersisted();

        //Adds a new relationship between both contentlets
        final Relationship relationship = APILocator.getRelationshipAPI().byContentType(contentType).get(0);

        ContentletRelationships contentletRelationships = new ContentletRelationships(
                blogContentParent);
        ContentletRelationshipRecords records = contentletRelationships.new ContentletRelationshipRecords(
                relationship, true);
        records.setRecords(Lists.newArrayList(blogContentChild));
        contentletRelationships.getRelationshipsRecords().add(records);
        blogContentParent = contentletAPI.checkin(blogContentParent, contentletRelationships, null, null, user, false);

        //Validate results from parent side
        List<Contentlet> results = relationshipAPI.dbRelatedContent(relationship, blogContentParent);
        validateDbRelatedContentResults(blogContentChild, results);

        //Validate results from child side
        results = relationshipAPI.dbRelatedContent(relationship, blogContentChild);
        validateDbRelatedContentResults(blogContentParent, results);
    }

    /**
     * Verifies the result list contains the content provided
     * @param contentlet
     * @param results
     */
    private void validateDbRelatedContentResults(final Contentlet contentlet, final List<Contentlet> results) {
        assertTrue(UtilMethods.isSet(results));
        assertEquals(1, results.size());
        assertEquals(contentlet.getIdentifier(), results.get(0).getIdentifier());
    }

    private void migrateRelationshipAndValidate(final Relationship relationship,
            Contentlet contentletParent, final List<Contentlet> relationshipList)
            throws DotDataException, DotSecurityException {

        final Map<Relationship, List<Contentlet>> relationshipListMap = Maps.newHashMap();
        relationshipListMap.put(relationship, relationshipList);
        contentletParent.setIndexPolicy(IndexPolicy.WAIT_FOR);
        contentletParent.setIndexPolicyDependencies(IndexPolicy.WAIT_FOR);
        //Checkin of the parent to validate Relationships
        contentletParent = APILocator
                .getContentletAPI().checkin(contentletParent,relationshipListMap,user,false);

        //List Related Contentlets
        List<Contentlet> relatedContent = APILocator.getContentletAPI()
                .getRelatedContent(contentletParent, relationship, user, false);
        assertNotNull(relatedContent);
        assertEquals(relationshipList.size(), relatedContent.size());

        for (int i = 0; i< relationshipList.size(); i++){
            assertEquals(relationshipList.get(i).getIdentifier(),relatedContent.get(i).getIdentifier());
        }

        //Get versionTs before migration
        final StringBuilder versionTsQuery = new StringBuilder(
                "select identifier, version_ts from contentlet_version_info where identifier=?");

        final DotConnect dcBefore = new DotConnect();

        relationshipList.forEach(elem -> versionTsQuery.append(" or identifier=?"));
        dcBefore.setSQL(versionTsQuery.toString());

        dcBefore.addParam(contentletParent.getIdentifier());
        relationshipList.forEach(elem -> dcBefore.addParam(elem.getIdentifier()));

        final Map versionTsBefore = dcBefore.loadObjectResults().stream()
                .collect(Collectors
                        .toMap(elem -> elem.get("identifier"), elem -> elem.get("version_ts")));

        DateUtil.sleep(2000); //mySQL is extremely fast we must allow sometime to ensure the new version info will be saved in different instant.

        // Migrate Relationship
        relationshipAPI.convertRelationshipToRelationshipField(relationship);

        //Verify the relationship still exists
        assertNotNull(relationshipAPI.byInode(relationship.getInode()));

        //Check Content is still related
        relatedContent = APILocator.getContentletAPI()
                .getRelatedContent(contentletParent, relationship, user, false);
        assertNotNull(relatedContent);
        assertEquals(relationshipList.size(), relatedContent.size());

        for (int i = 0; i < relationshipList.size(); i++) {
            assertEquals(relationshipList.get(i).getIdentifier(),
                    relatedContent.get(i).getIdentifier());
        }

        //Get versionTs after migration
        final DotConnect dcAfter = new DotConnect();

        dcAfter.setSQL(versionTsQuery.toString());
        dcAfter.addParam(contentletParent.getIdentifier());
        relationshipList.forEach(elem -> dcAfter.addParam(elem.getIdentifier()));

        final Map versionTsAfter = dcAfter.loadObjectResults().stream()
                .collect(Collectors
                        .toMap(elem -> elem.get("identifier"), elem -> elem.get("version_ts")));

        //Verify versionTs has been updated after migration
        assertTrue(((Timestamp)versionTsBefore.get(contentletParent.getIdentifier())).before(
                (Timestamp) versionTsAfter.get(contentletParent.getIdentifier())));

        for (int i = 0; i < relationshipList.size(); i++) {
            assertTrue(((Timestamp) versionTsBefore.get(relationshipList.get(i).getIdentifier())).before(
                    (Timestamp) versionTsAfter.get(relationshipList.get(i).getIdentifier())));
        }
    }

    private ContentType createContentType(final String name) throws DotSecurityException, DotDataException {
        return contentTypeAPI.save(ContentTypeBuilder.builder(SimpleContentType.class).folder(
                FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST).name(name)
                .owner(user.getUserId()).build());
    }
}
