package com.dotmarketing.portlets.workflows.business;

import com.dotcms.business.WrapInTransaction;
import com.dotcms.contenttype.business.ContentTypeAPI;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.AlreadyExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.ContentletDependencies;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.structure.model.ContentletRelationships;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentAsDraftActionlet;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

public class SaveContentDraftActionletTest extends BaseWorkflowIntegrationTest {

    private static CreateSchemeStepActionResult schemeStepActionResult = null;
    private static WorkflowAPI                  workflowAPI            = null;
    private static ContentletAPI                contentletAPI          = null;
    private static ContentType                  type                   = null;
    private static Contentlet                   contentlet             = null;
    private static Contentlet                   contentlet2             = null;
    private static Relationship                 relationship           = null;


    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment

        IntegrationTestInitService.getInstance().init();
        SaveContentDraftActionletTest.workflowAPI              = APILocator.getWorkflowAPI();
        final ContentTypeAPI contentTypeAPI                    = APILocator.getContentTypeAPI(APILocator.systemUser());
        SaveContentDraftActionletTest.contentletAPI            = APILocator.getContentletAPI();

        // creates the scheme and actions
        SaveContentDraftActionletTest.schemeStepActionResult   = SaveContentDraftActionletTest.createSchemeStepActionActionlet
                ("saveContentScheme" + UUIDGenerator.generateUuid(), "step1", "action1", SaveContentAsDraftActionlet.class);

        // creates the type to trigger the scheme
        SaveContentDraftActionletTest.createTestType(contentTypeAPI);

        // associated the scheme to the type
        SaveContentDraftActionletTest.workflowAPI.saveSchemesForStruct(new StructureTransformer(SaveContentDraftActionletTest.type).asStructure(),
                Arrays.asList(SaveContentDraftActionletTest.schemeStepActionResult.getScheme()));

        relationship = new Relationship();
        relationship.setParentRelationName("Parent");
        relationship.setChildRelationName("Child");
        relationship.setRelationTypeValue("IT-Parent-Child" + System.currentTimeMillis());
        relationship.setParentStructureInode(SaveContentDraftActionletTest.type.inode());
        relationship.setChildStructureInode(SaveContentDraftActionletTest.type.inode());
        APILocator.getRelationshipAPI().create(relationship);
    }

    private static void createTestType(final ContentTypeAPI contentTypeAPI) throws DotDataException, DotSecurityException {

        SaveContentDraftActionletTest.type = contentTypeAPI.save(
                ContentTypeBuilder.builder(BaseContentType.CONTENT.immutableClass())
                .expireDateVar(null).folder(FolderAPI.SYSTEM_FOLDER).host(Host.SYSTEM_HOST)
                .description("SaveContentAsDraftActionletTest...")
                .name("SaveContentDraftAsActionletTest").owner(APILocator.systemUser().toString())
                .variable("SaveContentAsDraftActionletTest"+ System.currentTimeMillis()).build());

        final List<Field> fields = new ArrayList<>(SaveContentDraftActionletTest.type.fields());

        fields.add(FieldBuilder.builder(TextField.class).name("title").variable("title")
                .contentTypeId(SaveContentDraftActionletTest.type.id()).dataType(DataTypes.TEXT).indexed(true).build());
        fields.add(FieldBuilder.builder(TextField.class).name("txt").variable("txt")
                .contentTypeId(SaveContentDraftActionletTest.type.id()).dataType(DataTypes.TEXT).indexed(true).build());

        SaveContentDraftActionletTest.type = contentTypeAPI.save(SaveContentDraftActionletTest.type, fields);
    }

    /**
     * Remove the content type and workflows created
     */
    @AfterClass
    public static void cleanup() throws DotDataException, DotSecurityException, AlreadyExistException {

        try {

            if (null != SaveContentDraftActionletTest.contentlet) {

                SaveContentDraftActionletTest.contentletAPI.destroy(SaveContentDraftActionletTest.contentlet, APILocator.systemUser(), false);
            }

            if (null != SaveContentDraftActionletTest.contentlet2) {

                SaveContentDraftActionletTest.contentletAPI.destroy(SaveContentDraftActionletTest.contentlet2, APILocator.systemUser(), false);
            }
        } finally {

            try {

                if (null != SaveContentDraftActionletTest.schemeStepActionResult) {

                    deleteWorkflowTaskByStep (SaveContentDraftActionletTest.schemeStepActionResult.getStep());
                    SaveContentDraftActionletTest.cleanScheme(SaveContentDraftActionletTest.schemeStepActionResult.getScheme());
                }
            } finally {

                if (relationship != null) {
                    APILocator.getRelationshipAPI().delete(relationship);
                }

                if (null != SaveContentDraftActionletTest.type) {

                    ContentTypeAPI contentTypeAPI = APILocator.getContentTypeAPI(APILocator.systemUser());
                    contentTypeAPI.delete(SaveContentDraftActionletTest.type);
                }
            }
        }
    } // cleanup

    @WrapInTransaction
    private static void deleteWorkflowTaskByStep(final WorkflowStep step) {

        try {
            new DotConnect().setSQL("delete from workflow_task where status = ?")
                    .addParam(step.getId()).loadResult();
        } catch (DotDataException e) {
            Logger.error(SaveContentDraftActionletTest.class, e.getMessage(), e);
        }
    }

    @Test
    public void saveContentDraftTest() throws DotDataException, DotSecurityException {

        final long       languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final Contentlet contentlet = new Contentlet();
        final User       user       = APILocator.systemUser();
        contentlet.setContentTypeId(SaveContentDraftActionletTest.type.id());
        contentlet.setOwner(user.toString());
        contentlet.setModUser(user.getUserId());
        contentlet.setModDate(new Date());
        contentlet.setLanguageId(languageId);
        contentlet.setStringProperty("title", "Test Save");
        contentlet.setStringProperty("txt",   "Test Save Text");
        contentlet.setHost(Host.SYSTEM_HOST);
        contentlet.setFolder(FolderAPI.SYSTEM_FOLDER);


        // first save
        contentlet.setIndexPolicy(IndexPolicy.FORCE);
        contentlet.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        final Contentlet contentlet1 = SaveContentDraftActionletTest.contentletAPI.checkin(contentlet, user, false);
        SaveContentDraftActionletTest.contentlet = contentlet1;

        contentlet1.setStringProperty("title", "Test Save 1");
        contentlet1.setStringProperty("txt", "Test Save Text 1");

        contentlet1.setIndexPolicy(IndexPolicy.FORCE);
        contentlet1.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        final Contentlet contentlet2 = SaveContentDraftActionletTest.contentletAPI.checkout(contentlet1.getInode(), user, false);

        contentlet2.setStringProperty("title", "Test Save 2");
        contentlet2.setStringProperty("txt", "Test Save Text 2");

        contentlet2.setIndexPolicy(IndexPolicy.FORCE);
        contentlet2.setBoolProperty(Contentlet.DISABLE_WORKFLOW, true);
        final Contentlet contentlet3 = SaveContentDraftActionletTest.contentletAPI.checkin(contentlet2, user, false);

        final Contentlet contentlet4 = SaveContentDraftActionletTest.contentletAPI.
                find(contentlet3.getInode(), user, false);
        // triggering the save content action

        contentlet4.setActionId(
                this.schemeStepActionResult.getAction().getId());
        contentlet4.setStringProperty("title", "Test Save 3");
        contentlet4.setStringProperty("txt", "Test Save Text 3");

        if (SaveContentDraftActionletTest.contentletAPI.canLock(contentlet4, user) && contentlet4.isLocked()) {
            SaveContentDraftActionletTest.contentletAPI.unlock(contentlet4, user, false);
        }

        contentlet4.setIndexPolicy(IndexPolicy.FORCE);
        SaveContentDraftActionletTest.workflowAPI.fireWorkflowNoCheckin(contentlet4, user);

        final Contentlet contentlet5 = SaveContentDraftActionletTest.contentletAPI.findContentletByIdentifier
                (SaveContentDraftActionletTest.contentlet.getIdentifier(),
                        false, languageId, user, false);

        // the contentlet save by the action must be not null, should has the same version.
        Assert.assertNotNull(contentlet5);
        Assert.assertNotNull(contentlet5.getInode());
        Assert.assertTrue(contentlet5.getInode().equals(contentlet3.getInode()));
        Assert.assertEquals("Test Save 3", contentlet5.getStringProperty("title"));
        Assert.assertEquals("Test Save Text 3", contentlet5.getStringProperty("txt"));
    }

    @Test
    public void save_content_draft_test_with_categories_success() throws DotDataException, DotSecurityException {

        final long       languageId = APILocator.getLanguageAPI().getDefaultLanguage().getId();
        final User       user       = APILocator.systemUser();
        // content to related
        Contentlet contentletRelated = new Contentlet();
        contentletRelated.setContentTypeId(SaveContentDraftActionletTest.type.id());
        contentletRelated.setOwner(user.toString());
        contentletRelated.setModUser(user.getUserId());
        contentletRelated.setModDate(new Date());
        contentletRelated.setLanguageId(languageId);
        contentletRelated.setStringProperty("title", "Test Saves Related");
        contentletRelated.setStringProperty("txt",   "Test Saves Related Text");
        contentletRelated.setHost(Host.SYSTEM_HOST);
        contentletRelated.setFolder(FolderAPI.SYSTEM_FOLDER);
        contentletRelated = SaveContentDraftActionletTest.contentletAPI.checkin(contentletRelated, user, false);

        // begin test
        final Contentlet contentlet = new Contentlet();
        contentlet.setContentTypeId(SaveContentDraftActionletTest.type.id());
        contentlet.setOwner(user.toString());
        contentlet.setModUser(user.getUserId());
        contentlet.setModDate(new Date());
        contentlet.setLanguageId(languageId);
        contentlet.setStringProperty("title", "Test Saves");
        contentlet.setStringProperty("txt",   "Test Saves Text");
        contentlet.setHost(Host.SYSTEM_HOST);
        contentlet.setFolder(FolderAPI.SYSTEM_FOLDER);
        contentlet.setIndexPolicy(IndexPolicy.FORCE);
        // first save
        final Contentlet contentlet1 = SaveContentDraftActionletTest.contentletAPI.checkin(contentlet, user, false);
        SaveContentDraftActionletTest.contentlet2 = contentlet1;

        contentlet1.setStringProperty("title", "Test Saves 2");
        contentlet1.setStringProperty("txt", "Test Saves Text 2");


        final Contentlet contentlet2 = SaveContentDraftActionletTest.contentletAPI.checkout(contentlet1.getInode(), user, false);

        contentlet2.setStringProperty("title", "Test Saves 2");
        contentlet2.setStringProperty("txt", "Test Saves Text 2");

        contentlet2.setIndexPolicy(IndexPolicy.FORCE);
        final Contentlet contentlet3 = SaveContentDraftActionletTest.contentletAPI.checkin(contentlet2, user, false);

        final Contentlet contentlet4 = SaveContentDraftActionletTest.contentletAPI.
                find(contentlet3.getInode(), user, false);
        // triggering the save content action

        contentlet4.setActionId(
                this.schemeStepActionResult.getAction().getId());
        contentlet4.setStringProperty("title", "Test Saves 3");
        contentlet4.setStringProperty("txt", "Test Saves Text 3");

        if (SaveContentDraftActionletTest.contentletAPI.canLock(contentlet4, user) && contentlet4.isLocked()) {
            SaveContentDraftActionletTest.contentletAPI.unlock(contentlet4, user, false);
        }

        final List<Category> categories    =  APILocator.getCategoryAPI().findAll(user, false);
        final Relationship relationship    = SaveContentDraftActionletTest.relationship;

        APILocator.getRelationshipAPI().addRelationship(contentlet4.getInode(), contentletRelated.getInode(), relationship.getParentRelationName());
        final List<Contentlet> contentlets = APILocator.getRelationshipAPI().dbRelatedContent(relationship, contentlet4);
        final Map<Relationship, List<Contentlet>> contentRelationshipsMap = new HashMap<>();
        contentRelationshipsMap.put(relationship, contentlets);
        final ContentletRelationships contentletRelationships = getContentletRelationshipsFromMap(contentlet4, contentRelationshipsMap);

        SaveContentDraftActionletTest.workflowAPI.fireContentWorkflow(contentlet4,
                new ContentletDependencies.Builder().categories(categories).
                relationships(contentletRelationships).modUser(user)
                .respectAnonymousPermissions(false).generateSystemEvent(false).build());

        final Contentlet contentlet5 = SaveContentDraftActionletTest.contentletAPI.findContentletByIdentifier
                (SaveContentDraftActionletTest.contentlet2.getIdentifier(),
                        false, languageId, user, false);

        final ContentletRelationships contentRelationship = APILocator.getContentletAPI().getAllRelationships(contentlet5);

        // the contentlet save by the action must be not null, should has the same version.
        Assert.assertNotNull(contentlet5);
        Assert.assertNotNull(contentlet5.getInode());
        Assert.assertNotNull(contentRelationship);
        Assert.assertTrue(contentRelationship.getRelationshipsRecords().size() > 0);
        Assert.assertTrue(contentlet5.getInode().equals(contentlet3.getInode()));
        Assert.assertEquals("Test Saves 3", contentlet5.getStringProperty("title"));
        Assert.assertEquals("Test Saves Text 3", contentlet5.getStringProperty("txt"));
    }

    private static ContentletRelationships getContentletRelationshipsFromMap(Contentlet contentlet,
                                                                      Map<Relationship, List<Contentlet>> contentRelationships) {

        if(contentRelationships == null) {
            return null;
        }

        Structure st = CacheLocator.getContentTypeCache().getStructureByInode(contentlet.getStructureInode());
        ContentletRelationships relationshipsData = new ContentletRelationships(contentlet);
        List<ContentletRelationships.ContentletRelationshipRecords> relationshipsRecords = new ArrayList<ContentletRelationships.ContentletRelationshipRecords>();
        relationshipsData.setRelationshipsRecords(relationshipsRecords);
        for(Map.Entry<Relationship, List<Contentlet>> relEntry : contentRelationships.entrySet()) {
            Relationship relationship = relEntry.getKey();
            boolean hasParent = FactoryLocator.getRelationshipFactory().isParent(relationship, st);
            boolean hasChildren = FactoryLocator.getRelationshipFactory().isChild(relationship, st);

            // self-join (same CT for parent and child) relationships return true to both, so since we can't
            // determine if it's parent or child we always assume child (e.g. Coming from the Content REST API)
            if (hasParent && hasChildren) {
                hasParent = false;
            }
            ContentletRelationships.ContentletRelationshipRecords
                    records = relationshipsData.new ContentletRelationshipRecords(relationship, hasParent);
            records.setRecords(relEntry.getValue());
            relationshipsRecords.add(records);
        }
        return relationshipsData;
    }
}