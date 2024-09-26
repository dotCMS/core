package com.dotcms.util;

import com.dotcms.LicenseTestUtil;
import com.dotcms.contenttype.business.ContentTypeAPIImpl;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.ImmutableTextAreaField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.SiteDataGen;
import com.dotcms.datagen.TemplateDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.mock.request.MockAttributeRequest;
import com.dotcms.mock.request.MockHeaderRequest;
import com.dotcms.mock.request.MockHttpRequestIntegrationTest;
import com.dotcms.mock.request.MockSessionRequest;
import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.db.LocalTransaction;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.categories.model.Category;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.contentlet.model.IndexPolicy;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Relationship;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.portlets.workflows.actionlet.PublishContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentAsDraftActionlet;
import com.dotmarketing.portlets.workflows.actionlet.UnpublishContentActionlet;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.ImportUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.liferay.portal.model.User;
import com.liferay.portal.util.WebKeys;
import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.apache.commons.io.FileUtils;
import org.glassfish.jersey.internal.util.Base64;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the Content Importer/Exporter feature is working as expected.
 * Users can import and export contents from the Content Search page.
 * 
 * @author Jonathan Gamba Date: 3/10/14
 */

@RunWith(DataProviderRunner.class)
public class ImportUtilTest extends BaseWorkflowIntegrationTest {

    private static final String BINARY_FIELD_NAME = "binaryField";
    private static User user;
    private static Host defaultSite;
    private static Language defaultLanguage;
    private static ContentTypeAPIImpl contentTypeApi;
    private static FieldAPI fieldAPI;
    private static BaseWorkflowIntegrationTest.CreateSchemeStepActionResult schemeStepActionResult1 = null;
    private static BaseWorkflowIntegrationTest.CreateSchemeStepActionResult schemeStepActionResult2 = null;
    private static ContentletAPI contentletAPI;
    private static RelationshipAPI relationshipAPI;
    private static ShortyIdAPI shortyIdAPI;
    private static WorkflowAPI workflowAPI;
    private static WorkflowAction saveAction;
    private static WorkflowAction saveAndPublishAction;
    private static WorkflowAction saveAsDraftAction;
    private static WorkflowAction unpublishAction;
    private static WorkflowAction publishAction;
    private static WorkflowAction publish2Action;
    private static PermissionAPI permissionAPI;
    private static final String TITLE_FIELD_NAME = "testTitle";
    private static final String BODY_FIELD_NAME = "testBody";
    private static final String SITE_FIELD_NAME = "testHost";
    private static final String STEP_BY_USING_ACTION1 = "StepByUsingAction1";
    private static final String STEP_BY_USING_ACTION2 = "StepByUsingAction2";
    private static final String STEP_BY_USING_ACTION3 = "StepByUsingAction3";

    private static final int EDIT_PERMISSION =
            PermissionAPI.PERMISSION_READ + PermissionAPI.PERMISSION_EDIT;
    private static final int PUBLISH_PERMISSION = EDIT_PERMISSION + PermissionAPI.PERMISSION_PUBLISH;

    private static User joeContributor;
    private static User janeReviewer;
    private static User chrisPublisher;

    private static Role reviewerRole;
    private static Role contributorRole;
    private static Role publisherRole;

    private static WorkflowStep initialStep;
    private static WorkflowStep step1;
    private static WorkflowStep step2;
    private static WorkflowStep step3;

    private static final String TEST_WITHOUT_WF_ACTION_ON_CSV = ", Test without WF Action ID set on CSV, ";
    private static final String TEST_WITH_WF_ACTION_ON_CSV = ", Test with WF Action ID set on CSV, ";
    private static final String TEST_WITH_WF_ACTION_ON_CSV_BUT_NO_PERMISSIONS = ", Test with WF Action ID set on CSV but no permissions, ";
    private static final String TEST_WITH_WF_ACTION_ON_DROPDOWN_BUT_NO_PERMISSIONS = ", Test with WF Action ID set on dropdown but not permission, ";
    private static final String TEST_WITH_WF_ACTION_ON_CSV_BUT_NO_PERMISSIONS_AND_USING_DROPDOWN_ACTION = ", Test with WF Action ID set on CSV (but no permission) and using dropdown action, ";

    public static class RelationshipTestCase {

        boolean useLucene;
        boolean legacyRelationship;

        public RelationshipTestCase(final boolean useLucene, final boolean legacyRelationship) {
            this.useLucene = useLucene;
            this.legacyRelationship = legacyRelationship;
        }
    }

    @DataProvider
    public static Object[] relationshipTestCases(){
        return new RelationshipTestCase[]{
                //Importing legacy relationships using a lucene query
                new RelationshipTestCase(true, true),
                //Importing legacy relationships using identifiers
                new RelationshipTestCase(false, true),
                //Importing relationships 2.0 using a lucene query
                new RelationshipTestCase(true, false),
                //Importing relationships 2.0 using identifiers
                new RelationshipTestCase(false, false)
        };
    }

    @DataProvider
    public static String[] testCasesUniqueTextField() {
        return new String[]{"UniqueTitle","A+ Student",
                "aaa-bbb-ccc","valid - field","with \" quotes",
                "with special characters + [ ] { } * ( ) : && ! | ^ ~ ?",
                "CASEINSENSITIVE","with chinese characters 好 心 面"};
    }

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        user = APILocator.getUserAPI().getSystemUser();
        user.setLocale(Locale.US);
        defaultSite = APILocator.systemHost();  //getHostAPI().findDefaultHost(user, false);
        defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        contentTypeApi = (ContentTypeAPIImpl) APILocator.getContentTypeAPI(user);
        fieldAPI = APILocator.getContentTypeFieldAPI();
        workflowAPI = APILocator.getWorkflowAPI();
        permissionAPI = APILocator.getPermissionAPI();
        final RoleAPI roleAPI = APILocator.getRoleAPI();
        contentletAPI = APILocator.getContentletAPI();
        shortyIdAPI = APILocator.getShortyAPI();
        relationshipAPI = APILocator.getRelationshipAPI();

        // creates the scheme, step1 and action1
        schemeStepActionResult1 = createSchemeStepActionActionlet
                ("ImportUtilScheme" + UUIDGenerator.generateUuid(), "step1", "action1",
                        SaveContentActionlet.class);

        //Second Workflow
        final Role anyWhoCanEditRole = roleAPI.loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_EDIT_ROLE_KEY);
        final Role anyWhoCanPublishRole = roleAPI.loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_PUBLISH_ROLE_KEY);
        publisherRole = TestUserUtils.getOrCreatePublisherRole();
        reviewerRole =  TestUserUtils.getOrCreateReviewerRole();
        contributorRole = TestUserUtils.getOrCreateContributorRole();

        schemeStepActionResult2 = createSchemeStepActionActionlet(
                "ImportUtilScheme_2_" + UUIDGenerator.generateUuid(), "initialStep", "Save",
                SaveContentActionlet.class);

        initialStep = schemeStepActionResult2.getStep();
        //Step for after saveAction
        step1 = createNewWorkflowStep(STEP_BY_USING_ACTION1,
                schemeStepActionResult2.getScheme().getId());
        step1.setMyOrder(1);
        workflowAPI.saveStep(step1, user);

        //Step for after saveAndPublishAction
        step2 = createNewWorkflowStep(STEP_BY_USING_ACTION2,
                schemeStepActionResult2.getScheme().getId());
        step2.setMyOrder(2);
        workflowAPI.saveStep(step2, user);

        //Step for after saveAsDraft
        step3 = createNewWorkflowStep(STEP_BY_USING_ACTION3,
                schemeStepActionResult2.getScheme().getId());
        step3.setMyOrder(3);
        workflowAPI.saveStep(step3, user);

        final List<String> rolesIds = new ArrayList<>();

        //Initial step. Setting saveAction configuration
        saveAction = schemeStepActionResult2.getAction();
        rolesIds.add(anyWhoCanEditRole.getId());
        saveAction.setNextStep(step1.getId());
        saveAction.setShowOn(WorkflowState.LOCKED, WorkflowState.UNLOCKED, WorkflowState.NEW,
                WorkflowState.PUBLISHED, WorkflowState.UNPUBLISHED, WorkflowState.EDITING);
        addWhoCanUseToAction(saveAction, rolesIds);
        rolesIds.remove(anyWhoCanEditRole.getId());
        workflowAPI.saveAction(saveAction.getId(),
                step2.getId(), APILocator.systemUser());
        workflowAPI.saveAction(saveAction.getId(),
                step3.getId(), APILocator.systemUser());

        //Initial step. Setting saveAndPublishAction configuration
        BaseWorkflowIntegrationTest.CreateSchemeStepActionResult schemeResultTemp = createActionActionlet(
                schemeStepActionResult2.getScheme().getId(),
                schemeStepActionResult2.getStep().getId(), "Save & Publish",
                SaveContentActionlet.class);
        saveAndPublishAction = schemeResultTemp.getAction();
        addActionletToAction(saveAndPublishAction.getId(), PublishContentActionlet.class, 1);

        saveAndPublishAction.setNextStep(step2.getId());
        rolesIds.add(anyWhoCanPublishRole.getId());
        saveAndPublishAction.setShowOn(WorkflowState.LOCKED, WorkflowState.UNLOCKED, WorkflowState.NEW,
                WorkflowState.PUBLISHED, WorkflowState.UNPUBLISHED, WorkflowState.EDITING);
        addWhoCanUseToAction(saveAndPublishAction, rolesIds);
        rolesIds.remove(anyWhoCanPublishRole.getId());



        //Initial step. Setting saveAsDraft configuration
        schemeResultTemp = createActionActionlet(schemeStepActionResult2.getScheme().getId(),
                schemeStepActionResult2.getStep().getId(), "Save as Draft",
                SaveContentAsDraftActionlet.class);
        saveAsDraftAction = schemeResultTemp.getAction();
        saveAsDraftAction.setNextStep(step3.getId());
        rolesIds.add(reviewerRole.getId());
        saveAsDraftAction.setShowOn(WorkflowState.LOCKED, WorkflowState.UNLOCKED, WorkflowState.NEW,
                WorkflowState.PUBLISHED, WorkflowState.UNPUBLISHED, WorkflowState.EDITING);
        addWhoCanUseToAction(saveAsDraftAction, rolesIds);
        rolesIds.remove(reviewerRole.getId());



        //step2 UnpublishAction configuration
        schemeResultTemp = createActionActionlet(schemeStepActionResult2.getScheme().getId(),
                step2.getId(), "Unpublish", UnpublishContentActionlet.class);
        unpublishAction = schemeResultTemp.getAction();
        rolesIds.add(publisherRole.getId());
        unpublishAction
                .setShowOn(WorkflowState.LOCKED, WorkflowState.UNLOCKED, WorkflowState.PUBLISHED, WorkflowState.EDITING);
        addWhoCanUseToAction(unpublishAction, rolesIds);
        rolesIds.remove(publisherRole.getId());
        workflowAPI.saveAction(unpublishAction.getId(), initialStep.getId(), APILocator.systemUser());
        workflowAPI.saveAction(unpublishAction.getId(), step1.getId(), APILocator.systemUser());
        workflowAPI.saveAction(unpublishAction.getId(), step3.getId(), APILocator.systemUser());

        //step1 publishAction configuration
        schemeResultTemp = createActionActionlet(schemeStepActionResult2.getScheme().getId(),
                step1.getId(), "Publish", PublishContentActionlet.class);
        publishAction = schemeResultTemp.getAction();
        rolesIds.add(publisherRole.getId());
        publishAction
                .setShowOn(WorkflowState.LOCKED, WorkflowState.UNLOCKED, WorkflowState.UNPUBLISHED, WorkflowState.EDITING);
        addWhoCanUseToAction(publishAction, rolesIds);
        rolesIds.remove(publisherRole.getId());
        workflowAPI.saveAction(publishAction.getId(), initialStep.getId(), APILocator.systemUser());
        workflowAPI.saveAction(publishAction.getId(), step2.getId(), APILocator.systemUser());
        workflowAPI.saveAction(publishAction.getId(), step3.getId(), APILocator.systemUser());

        //Step3 publish2Action configuration
        schemeResultTemp = createActionActionlet(schemeStepActionResult2.getScheme().getId(),
                step3.getId(), "Publish2", PublishContentActionlet.class);
        publish2Action = schemeResultTemp.getAction();
        rolesIds.add(publisherRole.getId());
        publish2Action
                .setShowOn(WorkflowState.LOCKED, WorkflowState.UNLOCKED, WorkflowState.UNPUBLISHED, WorkflowState.EDITING);
        addWhoCanUseToAction(publish2Action, rolesIds);
        rolesIds.remove(publisherRole.getId());
        workflowAPI.saveAction(publish2Action.getId(), initialStep.getId(), APILocator.systemUser());
        workflowAPI.saveAction(publish2Action.getId(), step1.getId(), APILocator.systemUser());
        workflowAPI.saveAction(publish2Action.getId(), step2.getId(), APILocator.systemUser());


        //Special Users
        joeContributor = TestUserUtils.getJoeContributorUser(); //APILocator.getUserAPI().loadUserById("dotcms.org.2789");
        janeReviewer = TestUserUtils.getJaneReviewerUser(); //APILocator.getUserAPI().loadUserById("dotcms.org.2787");
        chrisPublisher = TestUserUtils.getChrisPublisherUser();  //APILocator.getUserAPI().loadUserById("dotcms.org.2795");

        // Make sure the spanish language exist
        TestDataUtils.getSpanishLanguage();

    }

    /**
     * Testing the {@link ImportUtil#importFile(Long, String, String, String[], boolean, boolean,
     * com.liferay.portal.model.User, long, String[], com.dotcms.repackage.com.csvreader.CsvReader,
     * int, int, java.io.Reader, String, HttpServletRequest)} method
     */
    @Test
    @Ignore("Temporarily disabled")
    public void importFile()
            throws DotDataException, DotSecurityException, IOException, InterruptedException {

        ContentletAPI contentletAPI = APILocator.getContentletAPI();

        //Create a test Content Type
        final String contentTypeSuffix = String.valueOf(new Date().getTime());
        Structure contentType = new Structure();
        contentType.setName("Import Test " + contentTypeSuffix);
        contentType.setVelocityVarName("ImportTest_" + contentTypeSuffix);
        contentType.setDescription("Testing import of csv files");

        StructureFactory.saveStructure(contentType);

        //Create test fields
        Field textField = new Field("Title", Field.FieldType.TEXT, Field.DataType.TEXT, contentType,
                true, true, true, 1, false, false, true);
        textField = FieldFactory.saveField(textField);
        final String textFieldVarName = textField.getVelocityVarName();

        Field siteField = new Field("Host", Field.FieldType.HOST_OR_FOLDER, Field.DataType.TEXT,
                contentType, true, true, true, 2, false, false, true);
        siteField = FieldFactory.saveField(siteField);
        final String siteFieldVarName = siteField.getVelocityVarName();
        workflowAPI.saveSchemesForStruct(contentType,
                Arrays.asList(schemeStepActionResult1.getScheme()));

        try {

            //----------------PREVIEW = TRUE------------------------------------------
            //------------------------------------------------------------------------
            //Create the csv file to import
            Reader reader = createTempFile(textFieldVarName + ", " + siteFieldVarName + "\r\n" +
                    "Test1, " + defaultSite.getIdentifier() + "\r\n" +
                    "Test2, " + defaultSite.getIdentifier() + "\r\n" +
                    "Test3, " + defaultSite.getIdentifier() + "\r\n" +
                    "Test4, " + defaultSite.getIdentifier() + "\r\n");
            CsvReader csvreader = new CsvReader(reader);
            csvreader.setSafetySwitch(false);
            String[] csvHeaders = csvreader.getHeaders();

            //Preview=true
            HashMap<String, List<String>> results = ImportUtil
                    .importFile(0L, defaultSite.getInode(), contentType.getInode(), new String[]{},
                            true, false, user, defaultLanguage.getId(), csvHeaders, csvreader, -1,
                            -1, reader, schemeStepActionResult1.getAction().getId(),getHttpRequest());
            //Validations
            validate(results, true, false, true);

            //As it was a preview nothing should be saved
            List<Contentlet> savedData = contentletAPI
                    .findByStructure(contentType.getInode(), user, false, 0, 0);
            //Validations
            assertNotNull(savedData);
            assertEquals(savedData.size(), 0);

            //----------------PREVIEW = FALSE-----------------------------------------
            //------------------------------------------------------------------------
            //Create the csv file to import
            reader = createTempFile(textFieldVarName + ", " + siteFieldVarName + "\r\n" +
                    "Test1, " + defaultSite.getIdentifier() + "\r\n" +
                    "Test2, " + defaultSite.getIdentifier() + "\r\n" +
                    "Test3, " + defaultSite.getIdentifier() + "\r\n" +
                    "Test4, " + defaultSite.getIdentifier() + "\r\n");
            csvreader = new CsvReader(reader);
            csvreader.setSafetySwitch(false);
            csvHeaders = csvreader.getHeaders();

            //Preview=false
            results = ImportUtil
                    .importFile(0L, defaultSite.getInode(), contentType.getInode(), new String[]{},
                            false, false, user, defaultLanguage.getId(), csvHeaders, csvreader, -1,
                            -1, reader, schemeStepActionResult1.getAction().getId(),getHttpRequest());
            //Validations
            validate(results, false, false, true);

            //Now we should have saved data
            savedData = contentletAPI.findByStructure(contentType.getInode(), user, false, 0, 0);
            //Validations
            assertNotNull(savedData);
            assertEquals(savedData.size(), 4);

            Logger.info(this, "Test1 Content. IsInodeIndexed:" + contentletAPI
                    .isInodeIndexed(savedData.get(0).getInode()));
            Logger.info(this, "Test2 Content. IsInodeIndexed:" + contentletAPI
                    .isInodeIndexed(savedData.get(1).getInode()));
            Logger.info(this, "Test3 Content. IsInodeIndexed:" + contentletAPI
                    .isInodeIndexed(savedData.get(2).getInode()));
            Logger.info(this, "Test4 Content. IsInodeIndexed:" + contentletAPI
                    .isInodeIndexed(savedData.get(3).getInode()));

            //----------------USING WRONG HOST IDENTIFIERS----------------------------
            //------------------------------------------------------------------------
            //Create the csv file to import
            reader = createTempFile(textFieldVarName + ", " + siteFieldVarName + "\r\n" +
                    "Test5, " + defaultSite.getIdentifier() + "\r\n" +
                    "Test6, " + "999-99999999-99999999-00000" + "\r\n" +
                    "Test7, " + "44444444-5555555555-2222" + "\r\n");
            csvreader = new CsvReader(reader);
            csvreader.setSafetySwitch(false);
            csvHeaders = csvreader.getHeaders();

            //Preview=true
            results = ImportUtil
                    .importFile(0L, defaultSite.getInode(), contentType.getInode(), new String[]{},
                            true, false, user, defaultLanguage.getId(), csvHeaders, csvreader, -1,
                            -1, reader, schemeStepActionResult1.getAction().getId(),getHttpRequest());
            //Validations
            validate(results, true, true, true);

            //We should have the same amount on data
            savedData = contentletAPI.findByStructure(contentType.getInode(), user, false, 0, 0);
            //Validations
            assertNotNull(savedData);
            assertEquals(savedData.size(), 4);

            //---------------USING KEY FIELDS-----------------------------------------
            //------------------------------------------------------------------------

            //Making sure the contentlets are in the indexes
            List<ContentletSearch> contentletSearchResults;
            int x = 0;
            do {
                Thread.sleep(30000);
                //Verify if it was added to the index
                contentletSearchResults = contentletAPI.searchIndex(
                        "+structureName:" + contentType.getVelocityVarName()
                                + " +working:true +deleted:false +" + contentType
                                .getVelocityVarName() + ".title:Test1 +languageId:1", 0, -1, null,
                        user, true);
                x++;
            } while ((contentletSearchResults == null || contentletSearchResults.isEmpty())
                    && x < 100);

            //Create the csv file to import
            reader = createTempFile(textFieldVarName + ", " + siteFieldVarName + "\r\n" +
                    "Test1, " + defaultSite.getIdentifier() + "\r\n" +
                    "Test2, " + defaultSite.getIdentifier() + "\r\n");
            csvreader = new CsvReader(reader);
            csvreader.setSafetySwitch(false);
            csvHeaders = csvreader.getHeaders();

            //Preview=false
            results = ImportUtil.importFile(0L, defaultSite.getInode(), contentType.getInode(),
                    new String[]{textField.getInode()}, false, false, user, defaultLanguage.getId(),
                    csvHeaders, csvreader, -1, -1, reader,
                    schemeStepActionResult1.getAction().getId(),getHttpRequest());
            //Validations
            validate(results, false, false,
                    true);//We should expect warnings: Line #X. The key fields chosen match 1 existing content(s) - more than one match suggests key(s) are not properly unique

            //We used the key fields, so the import process should update instead to add new records
            savedData = contentletAPI.findByStructure(contentType.getInode(), user, false, 0, 0);
            //Validations
            assertNotNull(savedData);
            assertEquals(savedData.size(), 4);

            //---------------USING IDENTIFIER COLUMN----------------------------------
            //------------------------------------------------------------------------
            //Create the csv file to import

            String id1 = null;
            String id2 = null;
            for (Contentlet content : savedData) {
                if (content.getMap().get("title").equals("Test1")) {
                    id1 = content.getIdentifier();
                } else if (content.getMap().get("title").equals("Test2")) {
                    id2 = content.getIdentifier();
                }
            }

            reader = createTempFile(
                    "Identifier, " + textFieldVarName + ", " + siteFieldVarName + "\r\n" +
                            id1 + ", Test1_edited, " + defaultSite.getIdentifier() + "\r\n" +
                            id2 + ", Test2_edited, " + defaultSite.getIdentifier() + "\r\n");
            csvreader = new CsvReader(reader);
            csvreader.setSafetySwitch(false);
            csvHeaders = csvreader.getHeaders();

            //Preview=false
            results = ImportUtil
                    .importFile(0L, defaultSite.getInode(), contentType.getInode(), new String[]{},
                            false, false, user, defaultLanguage.getId(), csvHeaders, csvreader, -1,
                            -1, reader, schemeStepActionResult1.getAction().getId(),getHttpRequest());
            //Validations
            validate(results, false, false, true);

            //We used a identifier column, so the import process should update instead to add new records
            savedData = contentletAPI.findByStructure(contentType.getInode(), user, false, 0, 0);
            //Validations
            assertNotNull(savedData);
            assertEquals(savedData.size(), 4);

            //-------------------------LANGUAGE AND KEY FIELDS------------------------
            //------------------------------------------------------------------------
            //Create the csv file to import
            reader = createTempFile(
                    "languageCode, countryCode, " + textFieldVarName + ", " + siteFieldVarName
                            + "\r\n" +
                            "es, ES, Test1_edited, " + defaultSite.getIdentifier() + "\r\n" +
                            "es, ES, Test2_edited, " + defaultSite.getIdentifier() + "\r\n");
            csvreader = new CsvReader(reader);
            csvreader.setSafetySwitch(false);
            csvHeaders = csvreader.getHeaders();

            int languageCodeHeaderColumn = 0;
            int countryCodeHeaderColumn = 1;
            //Preview=false
            results = ImportUtil.importFile(0L, defaultSite.getInode(), contentType.getInode(),
                    new String[]{textField.getInode()}, false, true, user, -1, csvHeaders,
                    csvreader, languageCodeHeaderColumn, countryCodeHeaderColumn, reader,
                    schemeStepActionResult1.getAction().getId(),getHttpRequest());
            //Validations
            validate(results, false, false, false);

            //We used the key fields, so the import process should update instead to add new records
            savedData = contentletAPI.findByStructure(contentType.getInode(), user, false, 0, 0);
            //Validations
            assertNotNull(savedData);
            assertEquals(savedData.size(), 6);

            //Validate we saved the contentlets on spanish
            int spanishFound = 0;
            for (Contentlet contentlet : savedData) {
                if (contentlet.getLanguageId() == 2) {
                    spanishFound++;
                }
            }
            assertEquals(spanishFound, 2);
        } finally {
            try {
                contentTypeApi.delete(new StructureTransformer(contentType).from());
            }catch (Exception e) {e.printStackTrace();}

        }
    }

    /**
     * Creates a temporal file using a given content
     */
    private Reader createTempFile(final String content) throws IOException {

        final File tempTestFile = File
                .createTempFile("csvTest_" + new Date().getTime(), ".txt");
        FileUtils.writeStringToFile(tempTestFile, content);
        final byte[] bytes = com.liferay.util.FileUtil.getBytes(tempTestFile);

        return new InputStreamReader(new ByteArrayInputStream(bytes), Charset.forName("UTF-8"));
    }

    /**
     * Validates a given result generated by the ImportUtil.importFile method
     */
    private void validate(final Map<String, List<String>> results, final Boolean preview,
            final Boolean expectingErrors, final Boolean expectingWarnings) {

        //Reading the results

        if (expectingErrors) {
            final List<String> errors = results.get("errors");
            assertNotNull(errors);//Expected warnings as no key fields were chosen
            assertTrue(!errors.isEmpty());
        } else {
            final List<String> errors = results.get("errors");
            assertTrue(errors == null || errors.isEmpty());//No errors should be found
        }

        if (expectingWarnings) {
            final List<String> warnings = results.get("warnings");
            assertNotNull(warnings);//Expected warnings as no key fields were chosen
            Logger.info(this, "List WARNINGS: " + warnings.size());
            assertTrue(!warnings.isEmpty());
        } else {
            final List<String> warnings = results.get("warnings");
            assertTrue(warnings == null || warnings.isEmpty());
        }

        final List<String> finalResults = results.get("results");
        assertNotNull(finalResults);//Expected final results messages
        assertTrue(!finalResults.isEmpty());

        final List<String> messages = results.get("messages");
        assertNotNull(messages);//Expected return messages
        assertTrue(!messages.isEmpty());

        if (!preview) {

            final List<String> lastInode = results.get("lastInode");
            assertNotNull(lastInode);
            assertTrue(!lastInode.isEmpty());

            final List<String> counters = results.get("counters");
            assertNotNull(counters);
            assertTrue(!counters.isEmpty());
        }
    }

    /**
     * Method to test: {@link ImportUtil#importFile(Long, String, String, String[], boolean, boolean, User, long, String[], CsvReader, int, int, Reader, String, HttpServletRequest)}
     * Test Case: Lines contain the same unique text key (testTitle) but different language
     * Expected Results: The import process should success
     */
    @UseDataProvider("testCasesUniqueTextField")
    @Test
    public void importFile_success_when_twoLinesHaveSameUniqueKeysButDifferentLanguage(final String uniqueTitle)
            throws DotSecurityException, DotDataException, IOException {

        CsvReader csvreader;
        com.dotcms.contenttype.model.field.Field titleField, hostField;
        HashMap<String, List<String>> results;
        Reader reader;
        String[] csvHeaders;

        ContentType type = new ContentTypeDataGen().host(APILocator.systemHost()).nextPersisted();

        titleField =
                FieldBuilder.builder(TextField.class).name("testTitle").variable("testTitle")
                        .unique(true)
                        .contentTypeId(type.id()).dataType(
                        DataTypes.TEXT).build();
        hostField =
                FieldBuilder.builder(HostFolderField.class).name("testHost")
                        .variable("testHost")
                        .contentTypeId(type.id()).dataType(
                        DataTypes.TEXT).build();
        titleField = fieldAPI.save(titleField, user);
        fieldAPI.save(hostField, user);

        workflowAPI.saveSchemesForStruct(new StructureTransformer(type).asStructure(),
                Arrays.asList(schemeStepActionResult1.getScheme()));

        //Creating csv
        reader = createTempFile("languageCode, countryCode, testTitle, testHost" + "\r\n" +
                "es, ES, " + uniqueTitle + " , " + defaultSite.getIdentifier() + "\r\n" +
                "en, US, " + uniqueTitle + " , " + defaultSite.getIdentifier() + "\r\n");
        csvreader = new CsvReader(reader);
        csvreader.setSafetySwitch(false);
        csvHeaders = csvreader.getHeaders();

        int languageCodeHeaderColumn = 0;
        int countryCodeHeaderColumn = 1;
        //Preview=false
        results =
                ImportUtil
                        .importFile(0L, defaultSite.getInode(), type.inode(),
                                new String[]{titleField.id()}, true, true,
                                user, -1, csvHeaders, csvreader, languageCodeHeaderColumn,
                                countryCodeHeaderColumn, reader,
                                schemeStepActionResult1.getAction().getId(),getHttpRequest());
        //Validations
        validate(results, true, false, true);

        assertTrue(results.get("warnings").size() == 1);
        assertEquals("The Content Type field testTitle is unique.", results.get("warnings").get(0));
    }

    /**
     * Method to test: {@link ImportUtil#importFile(Long, String, String, String[], boolean, boolean, User, long, String[], CsvReader, int, int, Reader, String, HttpServletRequest)}
     * Test Case: Lines contain the same unique text key (testTitle)
     * Expected Results: The import process should fail
     */
    @UseDataProvider("testCasesUniqueTextField")
    @Test
    public void importFile_fails_when_twoLinesHaveSameUniqueKeys(final String uniqueTitle)
            throws DotSecurityException, DotDataException, IOException {

        CsvReader csvreader;
        com.dotcms.contenttype.model.field.Field titleField, hostField;
        HashMap<String, List<String>> results;
        Reader reader;
        String[] csvHeaders;

        ContentType type = new ContentTypeDataGen().host(APILocator.systemHost()).nextPersisted();

        try {
            titleField =
                    FieldBuilder.builder(TextField.class).name("testTitle").variable("testTitle")
                            .unique(true)
                            .contentTypeId(type.id()).dataType(
                            DataTypes.TEXT).build();
            hostField =
                    FieldBuilder.builder(HostFolderField.class).name("testHost")
                            .variable("testHost")
                            .contentTypeId(type.id()).dataType(
                            DataTypes.TEXT).build();
            titleField = fieldAPI.save(titleField, user);
            fieldAPI.save(hostField, user);

            workflowAPI.saveSchemesForStruct(new StructureTransformer(type).asStructure(),
                    Arrays.asList(schemeStepActionResult1.getScheme()));

            //Creating csv
            reader = createTempFile("languageCode, countryCode, testTitle, testHost" + "\r\n" +
                    "en, US, " + uniqueTitle + " , " + defaultSite.getIdentifier() + "\r\n" +
                    "en, US, " + uniqueTitle + " , " + defaultSite.getIdentifier() + "\r\n");
            csvreader = new CsvReader(reader);
            csvreader.setSafetySwitch(false);
            csvHeaders = csvreader.getHeaders();

            int languageCodeHeaderColumn = 0;
            int countryCodeHeaderColumn = 1;
            //Preview=false
            results =
                    ImportUtil
                            .importFile(0L, defaultSite.getInode(), type.inode(),
                                    new String[]{titleField.id()}, true, true,
                                    user, -1, csvHeaders, csvreader, languageCodeHeaderColumn,
                                    countryCodeHeaderColumn, reader,
                                    schemeStepActionResult1.getAction().getId(),getHttpRequest());
            //Validations
            validate(results, true, false, true);

            assertTrue(results.get("warnings").size() == 2);
            assertEquals("The Content Type field testTitle is unique.", results.get("warnings").get(0));
            assertEquals("Line #3: contains duplicate values for a unique Content Type field 'testTitle', and will be ignored.", results.get("warnings").get(1));

        } finally {
            try {
                contentTypeApi.delete(type);
            }catch (Exception e) {e.printStackTrace();}
        }
    }

    /**
     * Method to test: {@link ImportUtil#importFile(Long, String, String, String[], boolean, boolean, User, long, String[], CsvReader, int, int, Reader, String, HttpServletRequest)}
     * Test Case: Lines contain the same unique number key (testNumber)
     * Expected Results: The import process should fail
     */
    @Test
    public void importFile_fails_when_twoLinesHaveSameUniqueNumberKeys()
            throws DotSecurityException, DotDataException, IOException {

        CsvReader csvreader;
        com.dotcms.contenttype.model.field.Field titleField, hostField;
        HashMap<String, List<String>> results;
        Reader reader;
        String[] csvHeaders;

        ContentType type = new ContentTypeDataGen().host(APILocator.systemHost()).nextPersisted();

        try {
            titleField =
                    FieldBuilder.builder(TextField.class).name("testNumber").variable("testNumber")
                            .unique(true)
                            .contentTypeId(type.id()).dataType(
                            DataTypes.FLOAT).build();
            hostField =
                    FieldBuilder.builder(HostFolderField.class).name("testHost")
                            .variable("testHost")
                            .contentTypeId(type.id()).dataType(
                            DataTypes.TEXT).build();
            titleField = fieldAPI.save(titleField, user);
            fieldAPI.save(hostField, user);

            workflowAPI.saveSchemesForStruct(new StructureTransformer(type).asStructure(),
                    Arrays.asList(schemeStepActionResult1.getScheme()));

            //Creating csv
            reader = createTempFile("languageCode, countryCode, testNumber, testHost" + "\r\n" +
                    "en, US, 35, " + defaultSite.getIdentifier() + "\r\n" +
                    "en, US, 35, " + defaultSite.getIdentifier() + "\r\n");
            csvreader = new CsvReader(reader);
            csvreader.setSafetySwitch(false);
            csvHeaders = csvreader.getHeaders();

            int languageCodeHeaderColumn = 0;
            int countryCodeHeaderColumn = 1;
            //Preview=false
            results =
                    ImportUtil
                            .importFile(0L, defaultSite.getInode(), type.inode(),
                                    new String[]{titleField.id()}, true, true,
                                    user, -1, csvHeaders, csvreader, languageCodeHeaderColumn,
                                    countryCodeHeaderColumn, reader,
                                    schemeStepActionResult1.getAction().getId(),getHttpRequest());
            //Validations
            validate(results, true, false, true);

            assertTrue(results.get("warnings").size() == 2);
            assertEquals("The Content Type field testNumber is unique.", results.get("warnings").get(0));
            assertEquals("Line #3: contains duplicate values for a unique Content Type field 'testNumber', and will be ignored.", results.get("warnings").get(1));

        } finally {
            try {
                contentTypeApi.delete(type);
            }catch (Exception e) {e.printStackTrace();}
        }
    }

    /**
     * Method to test: {@link ImportUtil#importFile(Long, String, String, String[], boolean, boolean, User, long, String[], CsvReader, int, int, Reader, String, HttpServletRequest)}
     * Test Case: Lines are imported with a unique text key (testTitle). Each line contains a unique testTitle
     * Expected Results: The import process should not fail
     */
    @Test
    public void importFile_success_when_twoLinesHaveDifferentUniqueKeys()
            throws DotSecurityException, DotDataException, IOException {

        CsvReader csvreader;
        com.dotcms.contenttype.model.field.Field titleField, hostField;
        HashMap<String, List<String>> results;
        Reader reader;
        String[] csvHeaders;

        ContentType type = new ContentTypeDataGen().host(APILocator.systemHost()).nextPersisted();

        try {
            titleField =
                    FieldBuilder.builder(TextField.class).name("testTitle").variable("testTitle")
                            .unique(true)
                            .contentTypeId(type.id()).dataType(
                            DataTypes.TEXT).build();
            hostField =
                    FieldBuilder.builder(HostFolderField.class).name("testHost")
                            .variable("testHost")
                            .contentTypeId(type.id()).dataType(
                            DataTypes.TEXT).build();
            titleField = fieldAPI.save(titleField, user);
            fieldAPI.save(hostField, user);

            workflowAPI.saveSchemesForStruct(new StructureTransformer(type).asStructure(),
                    Arrays.asList(schemeStepActionResult1.getScheme()));

            //Creating csv
            reader = createTempFile("languageCode, countryCode, testTitle, testHost" + "\r\n" +
                    "en, US, aaa-bbb-ccc, " + defaultSite.getIdentifier() + "\r\n" +
                    "en, US, aaa-bbb, " + defaultSite.getIdentifier() + "\r\n" +
                    "en, US, -bbb, " + defaultSite.getIdentifier() + "\r\n" +
                    "en, US, -bbb +again, " + defaultSite.getIdentifier() + "\r\n" +
                    "en, US, A+ Student, " + defaultSite.getIdentifier() + "\r\n" +
                    "en, US, UniqueTitle, " + defaultSite.getIdentifier() + "\r\n" +
                    "en, US, Unique, " + defaultSite.getIdentifier() + "\r\n" +
                    "en, US, Unique Again, " + defaultSite.getIdentifier() + "\r\n");
            csvreader = new CsvReader(reader);
            csvreader.setSafetySwitch(false);
            csvHeaders = csvreader.getHeaders();

            int languageCodeHeaderColumn = 0;
            int countryCodeHeaderColumn = 1;
            //Preview=false
            results =
                    ImportUtil
                            .importFile(0L, defaultSite.getInode(), type.inode(),
                                    new String[]{titleField.id()}, true, true,
                                    user, -1, csvHeaders, csvreader, languageCodeHeaderColumn,
                                    countryCodeHeaderColumn, reader,
                                    schemeStepActionResult1.getAction().getId(),getHttpRequest());
            //Validations
            validate(results, true, false, true);

            assertTrue(results.get("warnings").size() == 1);
            assertEquals("The Content Type field testTitle is unique.", results.get("warnings").get(0));
            assertTrue(results.get("errors").size() == 0);
        } finally {
            try {
                contentTypeApi.delete(type);
            }catch (Exception e) {e.printStackTrace();}
        }
    }

    /**
     * Method to test: {@link ImportUtil#importFile(Long, String, String, String[], boolean, boolean, User, long, String[], CsvReader, int, int, Reader, String, HttpServletRequest)}
     * Test Case: Lines are imported with a unique number key (testNumber). Each line contains a unique testNumber
     * Expected Results: The import process should not fail
     */
    @Test
    public void importFile_success_when_twoLinesHaveDifferentUniqueNumberKeys()
            throws DotSecurityException, DotDataException, IOException {

        CsvReader csvreader;
        com.dotcms.contenttype.model.field.Field titleField, hostField;
        HashMap<String, List<String>> results;
        Reader reader;
        String[] csvHeaders;

        ContentType type = new ContentTypeDataGen().host(APILocator.systemHost()).nextPersisted();

        try {
            titleField =
                    FieldBuilder.builder(TextField.class).name("testNumber").variable("testNumber")
                            .unique(true)
                            .contentTypeId(type.id()).dataType(
                            DataTypes.INTEGER).build();
            hostField =
                    FieldBuilder.builder(HostFolderField.class).name("testHost")
                            .variable("testHost")
                            .contentTypeId(type.id()).dataType(
                            DataTypes.TEXT).build();
            titleField = fieldAPI.save(titleField, user);
            fieldAPI.save(hostField, user);

            workflowAPI.saveSchemesForStruct(new StructureTransformer(type).asStructure(),
                    Arrays.asList(schemeStepActionResult1.getScheme()));

            //Creating csv
            reader = createTempFile("languageCode, countryCode, testNumber, testHost" + "\r\n" +
                    "en, US, 12345, " + defaultSite.getIdentifier() + "\r\n" +
                    "en, US, 12, " + defaultSite.getIdentifier() + "\r\n");
            csvreader = new CsvReader(reader);
            csvreader.setSafetySwitch(false);
            csvHeaders = csvreader.getHeaders();

            int languageCodeHeaderColumn = 0;
            int countryCodeHeaderColumn = 1;
            //Preview=false
            results =
                    ImportUtil
                            .importFile(0L, defaultSite.getInode(), type.inode(),
                                    new String[]{titleField.id()}, true, true,
                                    user, -1, csvHeaders, csvreader, languageCodeHeaderColumn,
                                    countryCodeHeaderColumn, reader,
                                    schemeStepActionResult1.getAction().getId(),getHttpRequest());
            //Validations
            validate(results, true, false, true);

            assertTrue(results.get("warnings").size() == 1);
            assertEquals("The Content Type field testNumber is unique.", results.get("warnings").get(0));
            assertTrue(results.get("errors").size() == 0);
        } finally {
            try {
                contentTypeApi.delete(type);
            }catch (Exception e) {e.printStackTrace();}
        }
    }

    /**
     * Import file saving the content with old checking method if :
     *
     * a) on testA, the CSV file doesn't have set a wfActionId
     * b) on testB, the User doesn't have permission to execute the wfActionId set on the CSV and/or
     * c) ont testC, the User doesn't have permission to set execute the action Id set on the dropdown
     *
     */
    @Test
    public void importFileWithoutwfActionIdOrWithoutPermissionsOnActionId_success_when_importLinesWithCheckinMethod()
            throws DotSecurityException, DotDataException, IOException {

        ContentType contentType = null;
        CsvReader csvreader;
        long time;
        HashMap<String, List<String>> results;
        Reader reader;
        String[] csvHeaders;
        com.dotcms.contenttype.model.field.Field titleField;

        try {
            time = System.currentTimeMillis();
            final String contentTypeName = "ContentTypeTestingWithActionIds1_" + time;
            final String contentTypeVarName = "velocityVarNameTestingWithActionIds1_" + time;
            final String testA = "TestA-" + time;
            final String testB = "TestB-" + time;
            final String testC = "TestC-" + time;

            //create content type
            contentType = createTestContentType(contentTypeName, contentTypeVarName);
            titleField = fieldAPI.byContentTypeAndVar(contentType, TITLE_FIELD_NAME);
            //Creating csv
            reader = createTempFile(TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + ", "
                    + Contentlet.WORKFLOW_ACTION_KEY + "\r\n" +
                    testA + TEST_WITHOUT_WF_ACTION_ON_CSV + "\r\n" +
                    testB + TEST_WITH_WF_ACTION_ON_CSV_BUT_NO_PERMISSIONS + saveAsDraftAction
                    .getId() + "\r\n" +
                    testC + TEST_WITH_WF_ACTION_ON_DROPDOWN_BUT_NO_PERMISSIONS
                    + saveAndPublishAction.getId());
            csvreader = new CsvReader(reader);
            csvreader.setSafetySwitch(false);
            csvHeaders = csvreader.getHeaders();

            //Preview=false
            results =
                    ImportUtil
                            .importFile(0L, defaultSite.getInode(), contentType.inode(),
                                    new String[]{titleField.id()}, false, false,
                                    joeContributor, defaultLanguage.getId(), csvHeaders, csvreader,
                                    -1, -1, reader,
                                    saveAsDraftAction.getId(),getHttpRequest());
            //Validations
            validate(results, false, false, true);
            /*
            Validate we are warning the user that does not have the proper permissions to
            execute the action selected in the Actions dropdown or the one found in the CSV file.
             */
            assertEquals(results.get("warnings").size(), 5);
            for (String warning : results.get("warnings")) {
                validateNoPermissionsWarning(warning);
            }
            assertEquals(results.get("errors").size(), 0);

            final List<Contentlet> savedData = contentletAPI
                    .findByStructure(contentType.inode(), user, false, 0, 0);
            assertNotNull(savedData);
            assertTrue(savedData.size() == 3);

            for (final Contentlet cont : savedData) {
                assertNotNull(workflowAPI.findTaskByContentlet(cont));
            }

        } finally {
            try {
                if (null != contentType) {
                    contentTypeApi.delete(contentType);
                }
            }catch (Exception e) {e.printStackTrace();}
        }
    }

    /**
     * Import file saving the content with the right action except for the second one that should
     * use the dropdown action. In the following way:
     *
     * a) the testD should be saved with the right action and go to step 1
     * b) on testE, the User doesn't have permission to execute the action Id and should be using the
     * dropdown action and go to step 3
     * c) the testD should be saved with the right action and go to step 3
     */
    @Test
    public void importFileWithwfActionIdAndPartialPermissionsOnActionId_success_when_importLinesWithPartialCheckinMethod()
            throws DotSecurityException, DotDataException, IOException {

        ContentType contentType = null;
        CsvReader csvreader;
        long time;
        HashMap<String, List<String>> results;
        Reader reader;
        String[] csvHeaders;
        com.dotcms.contenttype.model.field.Field titleField;

        try {
            time = System.currentTimeMillis();
            final String contentTypeName = "ContentTypeTestingWithActionIds2_" + time;
            final String contentTypeVarName = "velocityVarNameTestingWithActionIds2_" + time;
            final String testD = "TestD-" + time;
            final String testE = "TestE-" + time;
            final String testF = "TestF-" + time;

            //create content type
            contentType = createTestContentType(contentTypeName, contentTypeVarName);
            titleField = fieldAPI.byContentTypeAndVar(contentType, TITLE_FIELD_NAME);
            //Creating csv
            reader = createTempFile(TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + ", "
                    + Contentlet.WORKFLOW_ACTION_KEY + "\r\n"
                    + testD + TEST_WITH_WF_ACTION_ON_CSV + saveAction.getId() + "\r\n"
                    + testE + TEST_WITH_WF_ACTION_ON_CSV_BUT_NO_PERMISSIONS_AND_USING_DROPDOWN_ACTION
                    + saveAndPublishAction.getId() + "\r\n"
                    + testF+ TEST_WITH_WF_ACTION_ON_CSV + saveAsDraftAction.getId());

            csvreader = new CsvReader(reader);
            csvreader.setSafetySwitch(false);
            csvHeaders = csvreader.getHeaders();

            //Preview=false
            results =
                    ImportUtil
                            .importFile(0L, defaultSite.getInode(), contentType.inode(),
                                    new String[]{titleField.id()}, false, false,
                                    janeReviewer, defaultLanguage.getId(), csvHeaders, csvreader,
                                    -1, -1, reader,
                                    saveAsDraftAction.getId(),getHttpRequest());
            //Validations
            validate(results, false, false, true);
            /*
            Validate we are warning the user that does not have the proper permissions to
            execute the action selected in the Actions dropdown.
             */
            assertEquals(results.get("warnings").size(), 1);
            validateNoPermissionsWarning(results.get("warnings").get(0));
            assertEquals(results.get("errors").size(), 0);

            final List<Contentlet> savedData = contentletAPI
                    .findByStructure(contentType.inode(), user, false, 0, 0);
            assertNotNull(savedData);
            assertTrue(savedData.size() == 3);
            for (final Contentlet cont : savedData) {
                final WorkflowTask task = workflowAPI.findTaskByContentlet(cont);
                if (cont.getStringProperty(TITLE_FIELD_NAME).startsWith(testD)) {
                    assertNotNull(task);
                    assertEquals(task.getStatus(), step1.getId());
                } else {
                    assertNotNull(task);
                    assertEquals(task.getStatus(), step3.getId());
                }
            }

        } finally {
            try {
                if (null != contentType) {
                    contentTypeApi.delete(contentType);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Import file saving the content with the right action except for the second one that should
     * use the old checking. In the following way:
     *
     * a) the testG should be saved with the right action and go to step 1
     * b) on testH, the User doesn't have permission to execute the action Id and should be using
     * the old checkin and no step associated
     * c) the testI should be saved with the right action and go to step 3
     */
    @Test
    public void importFileWithwfActionIdAndPartialPermissionsOnActionId_success_when_importLinesWithActionsId_ExceptoOneWithCheckinMethod()
            throws DotSecurityException, DotDataException, IOException {

        ContentType contentType = null;
        CsvReader csvreader;
        long time;
        HashMap<String, List<String>> results;
        Reader reader;
        String[] csvHeaders;
        com.dotcms.contenttype.model.field.Field titleField;

        try {
            time = System.currentTimeMillis();
            final String contentTypeName = "ContentTypeTestingWithActionIds3_" + time;
            final String contentTypeVarName = "velocityVarNameTestingWithActionIds3_" + time;
            final String testG = "TestG-" + time;
            final String testH = "TestH-" + time;
            final String testI = "TestI-" + time;

            //create content type
            contentType = createTestContentType(contentTypeName, contentTypeVarName);
            titleField = fieldAPI.byContentTypeAndVar(contentType, TITLE_FIELD_NAME);
            //Creating csv
            reader = createTempFile(TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + ", "
                    + Contentlet.WORKFLOW_ACTION_KEY + "\r\n"
                    + testG + TEST_WITH_WF_ACTION_ON_CSV + saveAction.getId()+ "\r\n"
                    + testH + TEST_WITH_WF_ACTION_ON_CSV_BUT_NO_PERMISSIONS + saveAndPublishAction
                    .getId() + "\r\n"
                    + testI + TEST_WITH_WF_ACTION_ON_CSV + saveAsDraftAction.getId());
            csvreader = new CsvReader(reader);
            csvreader.setSafetySwitch(false);
            csvHeaders = csvreader.getHeaders();

            //Preview=false
            results =
                    ImportUtil
                            .importFile(0L, defaultSite.getInode(), contentType.inode(),
                                    new String[]{titleField.id()}, false, false,
                                    janeReviewer, defaultLanguage.getId(), csvHeaders, csvreader,
                                    -1, -1, reader,
                                    null,getHttpRequest());
            //Validations
            validate(results, false, false, true);
            /*
            Validate we are warning the user that does not have the proper permissions to
            execute the action passed in the CSV file.
             */
            assertEquals(results.get("warnings").size(), 1);
            validateNoPermissionsWarning(results.get("warnings").get(0));
            assertEquals(results.get("errors").size(), 0);

            final List<Contentlet> savedData = contentletAPI
                    .findByStructure(contentType.inode(), user, false, 0, 0);
            assertNotNull(savedData);
            assertTrue(savedData.size() == 3);

            for (final Contentlet cont : savedData) {
                final WorkflowTask task = workflowAPI.findTaskByContentlet(cont);
                if (cont.getStringProperty(TITLE_FIELD_NAME).startsWith(testG)) {
                    assertNotNull(task);
                    assertEquals(task.getStatus(), step1.getId());
                } else if (cont.getStringProperty(TITLE_FIELD_NAME).startsWith(testH)) {
                    Logger.info(this, "******** task: " + task);
                    assertNotNull(task);
                } else {
                    assertNotNull(task);
                    assertEquals(task.getStatus(), step3.getId());
                }
            }

        } finally {
            try {
                if (null != contentType) {
                    contentTypeApi.delete(contentType);
                }
            }catch (Exception e) {e.printStackTrace();}
        }
    }

    /**
     * Import file saving with the right action Id using shortIds. In the
     * following way:
     *
     * a) the testJ should be saved with the rigth action and go to step 1
     * b) the testK should be saved with the rigth action and go to step 2
     * c) the testL should be saved with the rigth action and go to step 3
     */
    @Test
    public void importFile_success_when_importLinesWithShortActionId()
            throws DotSecurityException, DotDataException, IOException {

        ContentType contentType = null;
        CsvReader csvreader;
        long time;
        HashMap<String, List<String>> results;
        Reader reader;
        String[] csvHeaders;
        com.dotcms.contenttype.model.field.Field titleField;

        try {
            time = System.currentTimeMillis();
            final String contentTypeName = "ContentTypeTestingWithActionIds4_" + time;
            final String contentTypeVarName = "velocityVarNameTestingWithActionIds4_" + time;
            final String testJ = "TestJ-" + time;
            final String testK = "TestK-" + time;
            final String testL = "TestL-" + time;

            //create content type
            contentType = createTestContentType(contentTypeName, contentTypeVarName);
            titleField = fieldAPI.byContentTypeAndVar(contentType, TITLE_FIELD_NAME);
            //Creating csv
            reader = createTempFile(TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + ", "
                    + Contentlet.WORKFLOW_ACTION_KEY + "\r\n" +
                    testJ + TEST_WITH_WF_ACTION_ON_CSV + shortyIdAPI.shortify(saveAction.getId())
                    + "\r\n" +
                    testK + TEST_WITH_WF_ACTION_ON_CSV + shortyIdAPI.shortify(saveAndPublishAction
                    .getId()) + "\r\n" +
                    testL + TEST_WITH_WF_ACTION_ON_CSV + shortyIdAPI.shortify(saveAsDraftAction.getId()));

            csvreader = new CsvReader(reader);
            csvreader.setSafetySwitch(false);
            csvHeaders = csvreader.getHeaders();

            //Preview=false
            results =
                    ImportUtil
                            .importFile(0L, defaultSite.getInode(), contentType.inode(),
                                    new String[]{titleField.id()}, false, false,
                                    chrisPublisher, defaultLanguage.getId(), csvHeaders, csvreader,
                                    -1, -1, reader,
                                    null,getHttpRequest());
            //Validations
            validate(results, false, false, false);
            assertEquals(results.get("warnings").size(), 0);
            assertEquals(results.get("errors").size(), 0);

            final List<Contentlet> savedData = contentletAPI
                    .findByStructure(contentType.inode(), user, false, 0, 0);
            assertNotNull(savedData);
            assertTrue(savedData.size() == 3);
            for (final Contentlet cont : savedData) {
                final WorkflowTask task = workflowAPI.findTaskByContentlet(cont);
                if (cont.getStringProperty(TITLE_FIELD_NAME).startsWith(testJ)) {
                    assertNotNull(task);
                    assertEquals(task.getStatus(), step1.getId());
                } else if (cont.getStringProperty(TITLE_FIELD_NAME).startsWith(testK)) {
                    assertNotNull(task);
                    assertEquals(task.getStatus(), step2.getId());
                } else {
                    assertNotNull(task);
                    assertEquals(task.getStatus(), step3.getId());
                }
            }

        } finally {
            try {
                if (null != contentType) {
                    contentTypeApi.delete(contentType);
                }
            }catch (Exception e) {e.printStackTrace();}
        }
    }

    /**
     * Import file to update existing contents using action Ids with shortIds. In the
     * following way:
     *
     * a) the testM should be Publish with the rigth action and stay on step 1
     * b) the testN should be Unpublish with the rigth action and stay on step 2
     * c) the testO should be Publish with the rigth action and stay on step 3
     */
    @Test
    public void importFile_success_when_importLinesUpdateExistingContent()
            throws Exception {

        ContentType contentType = null;
        CsvReader csvreader;
        long time;
        HashMap<String, List<String>> results;
        Reader reader;
        String[] csvHeaders;
        com.dotcms.contenttype.model.field.Field titleField;

        try {
            time = System.currentTimeMillis();
            final String contentTypeName = "ContentTypeTestingWithActionIds5_" + time;
            final String contentTypeVarName = "velocityVarNameTestingWithActionIds5_" + time;
            final String testM = "TestM-" + time;
            final String testN = "TestN-" + time;
            final String testO = "TestO-" + time;

            //create content type
            contentType = createTestContentType(contentTypeName, contentTypeVarName);
            titleField = fieldAPI.byContentTypeAndVar(contentType, TITLE_FIELD_NAME);

            //Creating csv
            String tempFile = TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + ", "
                    + Contentlet.WORKFLOW_ACTION_KEY + "\r\n" +
                    testM + TEST_WITH_WF_ACTION_ON_CSV + shortyIdAPI.shortify(saveAction.getId())
                    + "\r\n" +
                    testN + TEST_WITH_WF_ACTION_ON_CSV + shortyIdAPI.shortify(saveAndPublishAction
                    .getId()) + "\r\n" +
                    testO + TEST_WITH_WF_ACTION_ON_CSV + shortyIdAPI.shortify(saveAsDraftAction
                    .getId());

            Logger.info(this, "tempFile1: " + tempFile);
            reader = createTempFile(tempFile);

            csvreader = new CsvReader(reader);
            csvreader.setSafetySwitch(false);
            csvHeaders = csvreader.getHeaders();

            //Preview=false
            results =
                    ImportUtil
                            .importFile(0L, defaultSite.getInode(), contentType.inode(),
                                    new String[]{titleField.id()}, false, false,
                                    chrisPublisher, defaultLanguage.getId(), csvHeaders, csvreader,
                                    -1, -1, reader,
                                    null,getHttpRequest());
            //Validations
            validate(results, false, false, false);
            assertEquals(results.get("warnings").size(), 0);
            assertEquals(results.get("errors").size(), 0);

            List<Contentlet> savedData = contentletAPI
                    .findByStructure(contentType.inode(), user, false, 0, 0);
            assertNotNull(savedData);
            assertEquals(3, savedData.size());

            String identifier1 = savedData.get(0).getIdentifier();
            String identifier2 = savedData.get(1).getIdentifier();
            String identifier3 = savedData.get(2).getIdentifier();

            for (final Contentlet cont : savedData) {
                final WorkflowTask task = workflowAPI.findTaskByContentlet(cont);
                if (cont.getStringProperty(TITLE_FIELD_NAME).startsWith(testM)) {
                    assertNotNull(task);
                    assertFalse(cont.isLive());
                    assertEquals(task.getStatus(), step1.getId());
                    identifier1 = cont.getIdentifier();
                } else if(cont.getStringProperty(TITLE_FIELD_NAME).startsWith(testN)) {
                    assertNotNull(task);
                    assertTrue(cont.isLive());
                    assertEquals(task.getStatus(), step2.getId());
                    identifier2 = cont.getIdentifier();
                } else if(cont.getStringProperty(TITLE_FIELD_NAME).startsWith(testO)) {
                    assertNotNull(task);
                    assertFalse(cont.isLive());
                    assertEquals(task.getStatus(), step3.getId());
                    identifier3 = cont.getIdentifier();
                }

                cont.setIndexPolicy(IndexPolicy.FORCE);
            }

            APILocator.getContentletIndexAPI().addContentToIndex(savedData);

            //Update ContentType
            Thread.sleep(1000);

            tempFile = "Identifier," + TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + ", "
                    + Contentlet.WORKFLOW_ACTION_KEY + "\r\n" +
                    identifier1 + "," + testM + TEST_WITH_WF_ACTION_ON_CSV + shortyIdAPI.shortify(publishAction.getId())
                    + "\r\n" +
                    identifier2 + "," + testN + TEST_WITH_WF_ACTION_ON_CSV + shortyIdAPI.shortify(unpublishAction.getId())
                    + "\r\n" +
                    identifier3 + "," + testO + TEST_WITH_WF_ACTION_ON_CSV + shortyIdAPI.shortify(publish2Action.getId());

            Logger.info(this, "tempFile2: " + tempFile);
            final Reader reader2 = createTempFile(tempFile);

            final CsvReader csvreader2 = new CsvReader(reader2);
            csvreader2.setSafetySwitch(false);

            //Preview=false
            final String inode = contentType.inode();
            results =
                    LocalTransaction.wrapReturnWithListeners( ()-> importFile(inode, titleField, csvreader2, reader2));
            //Validations
            validate(results, false, false, true);
            Logger.info(this, "results.get(\"warnings\"): " + results.get("warnings"));
            assertEquals(results.get("warnings").size(), 3);
            assertEquals(results.get("errors").size(), 0);

            Thread.sleep(2000);

            savedData = contentletAPI
                    .findByStructure(contentType.inode(), user, false, 0, 0);
            assertNotNull(savedData);
            assertEquals(3, savedData.size());

            for (final Contentlet cont : savedData) {
                final WorkflowTask task = workflowAPI.findTaskByContentlet(cont);
                boolean isLive = APILocator.getVersionableAPI().hasLiveVersion(cont);

                Logger.info(this, "Contentlet id:    "  + cont.getIdentifier());
                Logger.info(this, "Contentlet inode: "  + cont.getInode());
                Logger.info(this, "Contentlet isLive: " + isLive);
                Logger.info(this, "Contentlet name: " + cont.getStringProperty(TITLE_FIELD_NAME));
                Logger.info(this, "Contentlet task: " + task.getStatus());
                Logger.info(this, "Contentlet step1: " + step1.getId());
                Logger.info(this, "Contentlet step2: " + step2.getId());
                Logger.info(this, "Contentlet step3: " + step3.getId());

                if (cont.getIdentifier().equals(identifier1)) {
                    assertNotNull(task);
                    assertTrue("the contentlet: " + cont.getIdentifier() + " should be live", isLive);
                    assertEquals( "the contentlet: " + cont.getIdentifier() + ":" + cont.getStringProperty(TITLE_FIELD_NAME) + " is on wrong step", step1.getId(), task.getStatus());
                } else if (cont.getIdentifier().equals(identifier2)) {
                    assertNotNull(task);
                    assertFalse("the contentlet: " + cont.getIdentifier() + " should NOT be live", isLive);
                    assertEquals( "the contentlet: " + cont.getIdentifier() + ":" + cont.getStringProperty(TITLE_FIELD_NAME) + " is on wrong step", step2.getId(), task.getStatus());
                } else if (cont.getIdentifier().equals(identifier3)) {
                    assertNotNull(task);
                    assertTrue("the contentlet: " + cont.getIdentifier() + " should be live", isLive);
                    assertEquals( "the contentlet: " + cont.getIdentifier() + ":" + cont.getStringProperty(TITLE_FIELD_NAME) + " is on wrong step", step3.getId(), task.getStatus());
                }
            }

        } finally {
            try {
                if (null != contentType) {
                    contentTypeApi.delete(contentType);
                }
            }catch (Exception e) {e.printStackTrace();}
        }
    }

    private HashMap<String, List<String>>  importFile (final String inode, com.dotcms.contenttype.model.field.Field titleField,
            final CsvReader csvreader, final Reader reader) {
        try  {
            return ImportUtil
                    .importFile(0L, defaultSite.getInode(), inode,
                            new String[]{titleField.id()}, false, false,
                            chrisPublisher, defaultLanguage.getId(),
                            csvreader.getHeaders(), csvreader,
                            -1, -1, reader,
                            null,getHttpRequest());
        }catch (Exception e) {

            return new HashMap<>();
        } finally {
            CloseUtils.closeQuietly(reader);
        }
    }

    @Test
    public void importFile_success_when_lineContainsLegacySelfRelatedContent()
            throws DotSecurityException, DotDataException, IOException {

        //Creates content type
        final ContentType type = createTestContentType("selfRelatedType", "selfRelatedType");
        final Structure structure = new StructureTransformer(type).asStructure();

        try {
            //Saves legacy self relationship
            final Relationship relationship = new Relationship(structure, structure,
                    "selRelatedParent", "selfRelatedChild",
                    RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal(), false, false);
            relationshipAPI.save(relationship);

            //Creates parent and child contentlets
            final ContentletDataGen contentletDataGen = new ContentletDataGen(type.id());
            contentletDataGen.languageId(defaultLanguage.getId());

            final Contentlet parentContentlet = contentletDataGen
                    .setProperty(TITLE_FIELD_NAME, "parent contentlet")
                    .setProperty(BODY_FIELD_NAME, "parent contentlet").nextPersisted();

            final Contentlet childContentlet = contentletDataGen
                    .setProperty(TITLE_FIELD_NAME, "child contentlet")
                    .setProperty(BODY_FIELD_NAME, "child contentlet").nextPersisted();

            //Creating csv
            final Reader reader = createTempFile(
                    TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + ", " + relationship
                            .getRelationTypeValue() + "-RELPARENT, " + relationship
                            .getRelationTypeValue() + "-RELCHILD"
                            + "\r\n" +
                            "Self related test, Testing Site, +identifier:"
                            + parentContentlet.getIdentifier() + ", +identifier:" + childContentlet
                            .getIdentifier());

            final HashMap<String, List<String>> results = importContentWithRelationships(type,
                    reader, new String[]{type.fieldMap().get(TITLE_FIELD_NAME).inode(),
                            type.fieldMap().get(BODY_FIELD_NAME).inode()});

            validateRelationshipResults(relationship, parentContentlet, childContentlet, results);

        } finally {
            try {
                if (type != null) {
                    contentTypeApi.delete(type);
                }
            }catch (Exception e) {e.printStackTrace();}
        }
    }

    @UseDataProvider("relationshipTestCases")
    @Test
    public void importFile_success_when_lineContainsRelationships(final RelationshipTestCase relationshipTestCase)
            throws DotSecurityException, DotDataException, IOException {

        //Creates content types
        ContentType parentContentType = null;
        ContentType childContentType  = null;
        com.dotcms.contenttype.model.field.Field field = null;

        final int cardinality = RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal();

        try {
            final Relationship relationship;
            parentContentType = createTestContentType("parentContentType", "parentContentType");
            childContentType = createTestContentType("childContentType", "childContentType");
            final Structure parentStructure = new StructureTransformer(parentContentType).asStructure();
            final Structure childStructure = new StructureTransformer(childContentType).asStructure();

            //Saves relationship parent --> child
            if (relationshipTestCase.legacyRelationship){
                relationship = new Relationship(parentStructure, childStructure,
                        parentContentType.name(), childContentType.name(), cardinality
                        , false, false);
                relationshipAPI.save(relationship);
            } else {
                field = FieldBuilder.builder(RelationshipField.class).name("testRelationship").variable("testRelationship")
                        .contentTypeId(parentContentType.id()).values(String.valueOf(cardinality))
                        .relationType(childContentType.variable()).build();

                field = fieldAPI.save(field, user);
                relationship = relationshipAPI.byTypeValue(
                        parentContentType.variable() + StringPool.PERIOD + field.variable());
            }

            //Creates child contentlet
            final ContentletDataGen contentletDataGen = new ContentletDataGen(childContentType.id());
            contentletDataGen.languageId(defaultLanguage.getId());

            final Contentlet childContentlet = contentletDataGen
                    .setProperty(TITLE_FIELD_NAME, "child contentlet")
                    .setProperty(BODY_FIELD_NAME, "child contentlet").nextPersisted();

            //Creating csv
            final Reader reader = createTempFile(
                    TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + ", " + (relationshipTestCase.legacyRelationship? relationship
                            .getRelationTypeValue(): field.variable())
                            + "\r\n" +
                            "Import related content test, Import related content test, " + (relationshipTestCase.useLucene? "+identifier:":"")
                            + childContentlet.getIdentifier());

            final HashMap<String, List<String>> results;
            if (relationshipTestCase.legacyRelationship) {
                results = importContentWithRelationships(parentContentType, reader,
                        new String[]{parentContentType.fieldMap().get(TITLE_FIELD_NAME).inode(),
                                parentContentType.fieldMap().get(BODY_FIELD_NAME).inode()});
            } else {
                results = importContentWithRelationships(parentContentType, reader,
                        new String[]{parentContentType.fieldMap().get(TITLE_FIELD_NAME).inode(),
                                parentContentType.fieldMap().get(BODY_FIELD_NAME).inode(),
                                field.inode()});
            }

            validateRelationshipResults(relationship, null, childContentlet, results);

        } finally {
            try {
                if (parentContentType != null) {
                    contentTypeApi.delete(parentContentType);
                }

                if (childContentType != null) {
                    contentTypeApi.delete(childContentType);
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void importFile_updateRelatedContentWithEmptyColumn_shouldWipeOutRelatedContentList()
            throws DotSecurityException, DotDataException, IOException {

        //Creates content types
        ContentType parentContentType = null;
        ContentType childContentType  = null;
        final int cardinality = RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal();

        try {
            final Relationship relationship;
            parentContentType = createTestContentType("parentContentType", "parentContentType");
            childContentType = createTestContentType("childContentType", "childContentType");

            com.dotcms.contenttype.model.field.Field field = FieldBuilder.builder(RelationshipField.class).name("testRelationship")
                    .variable("testRelationship")
                    .contentTypeId(parentContentType.id()).values(String.valueOf(cardinality))
                    .relationType(childContentType.variable()).build();

            field = fieldAPI.save(field, user);
            relationship = relationshipAPI.byTypeValue(
                    parentContentType.variable() + StringPool.PERIOD + field.variable());


            //Creates child contentlet
            final Contentlet childContentlet = new ContentletDataGen(childContentType.id())
                    .languageId(defaultLanguage.getId())
                    .setProperty(TITLE_FIELD_NAME, "child contentlet")
                    .setProperty(BODY_FIELD_NAME, "child contentlet").nextPersisted();

            //Creates parent contentlet
            Contentlet parentContentlet = new ContentletDataGen(parentContentType.id())
                    .languageId(defaultLanguage.getId())
                    .setProperty(TITLE_FIELD_NAME, "parent contentlet")
                    .setProperty(BODY_FIELD_NAME, "parent contentlet").next();

            parentContentlet = contentletAPI.checkin(parentContentlet,
                    Map.of(relationship, CollectionsUtils.list(childContentlet)),
                    user, false);

            //Creating csv to update parent
            final Reader reader = createTempFile(
                    TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + ", " + field.variable()
                            + "\r\n" +
                            "parent contentlet, parent contentlet body, ");

            final Map results = importContentWithRelationships(parentContentType, reader,
                    new String[]{
                            parentContentType.fieldMap().get(TITLE_FIELD_NAME).inode()});

            validate(results, false,false, true);

            //validate that related content was wiped out
            final List<Tree> childTrees = relationshipAPI
                    .relatedContentTrees(relationship, parentContentlet, true);
            assertNotNull(childTrees);
            assertEquals(0, childTrees.size());
        } finally {
            try {
                if (parentContentType != null) {
                    contentTypeApi.delete(parentContentType);
                }

                if (childContentType != null) {
                    contentTypeApi.delete(childContentType);
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void importFile_updateRelatedContentWithTheSameIdentifier_doesNotThrowException()
            throws DotSecurityException, DotDataException, IOException {

        //Creates content types
        ContentType parentContentType = null;
        ContentType childContentType  = null;
        final int cardinality = RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal();

        try {
            final Relationship relationship;
            parentContentType = createTestContentType("parentContentType", "parentContentType");
            childContentType = createTestContentType("childContentType", "childContentType");

            com.dotcms.contenttype.model.field.Field field = FieldBuilder.builder(RelationshipField.class).name("testRelationship")
                    .variable("testRelationship")
                    .contentTypeId(parentContentType.id()).values(String.valueOf(cardinality))
                    .relationType(childContentType.variable()).build();

            field = fieldAPI.save(field, user);
            relationship = relationshipAPI.byTypeValue(
                    parentContentType.variable() + StringPool.PERIOD + field.variable());


            //Creates child contentlet
            final Contentlet childContentlet = new ContentletDataGen(childContentType.id())
                    .languageId(defaultLanguage.getId())
                    .setProperty(TITLE_FIELD_NAME, "child contentlet")
                    .setProperty(BODY_FIELD_NAME, "child contentlet").nextPersisted();

            //Creates parent contentlet
            Contentlet parentContentlet = new ContentletDataGen(parentContentType.id())
                    .languageId(defaultLanguage.getId())
                    .setProperty(TITLE_FIELD_NAME, "parent contentlet")
                    .setProperty(BODY_FIELD_NAME, "parent contentlet").next();

            parentContentlet = contentletAPI.checkin(parentContentlet,
                    Map.of(relationship, CollectionsUtils.list(childContentlet)),
                    user, false);

            //Creating csv to update parent
            final Reader reader = createTempFile(
                    TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + ", " + field.variable()
                            + "\r\n" +
                            "parent contentlet, parent contentlet body, " + childContentlet.getIdentifier());

            final Map results = importContentWithRelationships(parentContentType, reader,
                    new String[]{
                            parentContentType.fieldMap().get(TITLE_FIELD_NAME).inode()});

            validate(results, false,false, true);
            //validate that related content is kept
            final List<Tree> childTrees = relationshipAPI
                    .relatedContentTrees(relationship, parentContentlet, true);
            assertNotNull(childTrees);
            assertEquals(1, childTrees.size());
            assertEquals(childContentlet.getIdentifier(), childTrees.get(0).getChild());
        } finally {
            try {
                if (parentContentType != null) {
                    contentTypeApi.delete(parentContentType);
                }

                if (childContentType != null) {
                    contentTypeApi.delete(childContentType);
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void importFile_updateRelatedContentWithoutRelationshipFieldColumn_shouldKeepRelatedContentList()
            throws DotSecurityException, DotDataException, IOException {

        //Creates content types
        ContentType parentContentType = null;
        ContentType childContentType  = null;
        final int cardinality = RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal();

        try {
            final Relationship relationship;
            parentContentType = createTestContentType("parentContentType", "parentContentType");
            childContentType = createTestContentType("childContentType", "childContentType");

            com.dotcms.contenttype.model.field.Field field = FieldBuilder.builder(RelationshipField.class).name("testRelationship")
                    .variable("testRelationship")
                    .contentTypeId(parentContentType.id()).values(String.valueOf(cardinality))
                    .relationType(childContentType.variable()).build();

            field = fieldAPI.save(field, user);
            relationship = relationshipAPI.byTypeValue(
                    parentContentType.variable() + StringPool.PERIOD + field.variable());


            //Creates child contentlet
            final Contentlet childContentlet = new ContentletDataGen(childContentType.id())
                    .languageId(defaultLanguage.getId())
                    .setProperty(TITLE_FIELD_NAME, "child contentlet")
                    .setProperty(BODY_FIELD_NAME, "child contentlet").nextPersisted();

            //Creates parent contentlet
            Contentlet parentContentlet = new ContentletDataGen(parentContentType.id())
                    .languageId(defaultLanguage.getId())
                    .setProperty(TITLE_FIELD_NAME, "parent contentlet")
                    .setProperty(BODY_FIELD_NAME, "parent contentlet").next();

            parentContentlet = contentletAPI.checkin(parentContentlet,
                    Map.of(relationship, CollectionsUtils.list(childContentlet)),
                    user, false);

            //Creating csv to update parent
            final Reader reader = createTempFile(
                    TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME
                            + "\r\n" +
                            "parent contentlet, parent contentlet body");

            final Map results = importContentWithRelationships(parentContentType, reader,
                    new String[]{parentContentType.fieldMap().get(TITLE_FIELD_NAME).inode(),
                            parentContentType.fieldMap().get(BODY_FIELD_NAME).inode()});

            validate(results, false,false, true);
            //validate that related content is kept
            final List<Tree> childTrees = relationshipAPI
                    .relatedContentTrees(relationship, parentContentlet, true);
            assertNotNull(childTrees);
            assertEquals(1, childTrees.size());
            assertEquals(childContentlet.getIdentifier(), childTrees.get(0).getChild());
        } finally {
            try {

                if (parentContentType != null) {
                    contentTypeApi.delete(parentContentType);
                }

                if (childContentType != null) {
                    contentTypeApi.delete(childContentType);
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void importFile_success_when_lineContainsOneSidedSelfRelationship()
            throws DotSecurityException, DotDataException, IOException {

        //Creates content types
        ContentType parentContentType = null;
        com.dotcms.contenttype.model.field.Field field;

        final int cardinality = RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal();

        try {

            parentContentType = createTestContentType("parentContentType", "parentContentType");

            field = FieldBuilder.builder(RelationshipField.class).name("testRelationship")
                    .variable("testRelationship")
                    .contentTypeId(parentContentType.id()).values(String.valueOf(cardinality))
                    .relationType(parentContentType.variable()).build();

            field = fieldAPI.save(field, user);
            final Relationship relationship = relationshipAPI.byTypeValue(
                    parentContentType.variable() + StringPool.PERIOD + field.variable());

            //Creates child contentlet
            final ContentletDataGen contentletDataGen = new ContentletDataGen(
                    parentContentType.id());

            final Contentlet childContentlet = contentletDataGen.languageId(defaultLanguage.getId())
                    .setProperty(TITLE_FIELD_NAME, "child contentlet")
                    .setProperty(BODY_FIELD_NAME, "child contentlet").nextPersisted();

            //Creating csv
            final Reader reader = createTempFile(
                    TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + ", " + field.variable()
                            + "\r\n" +
                            "Import related content test, Import related content test, "
                            + childContentlet.getIdentifier());

            final HashMap<String, List<String>> results = importContentWithRelationships(
                    parentContentType, reader,
                    new String[]{parentContentType.fieldMap().get(TITLE_FIELD_NAME).inode(),
                            parentContentType.fieldMap().get(BODY_FIELD_NAME).inode(),
                            field.inode()});

            validateRelationshipResults(relationship, null, childContentlet, results);

        } finally {
            try {
                if (parentContentType != null) {
                    contentTypeApi.delete(parentContentType);
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void importFile_success_when_lineContainsBothSidedSelfRelationship()
            throws DotSecurityException, DotDataException, IOException {

        //Creates content types
        ContentType parentContentType = null;
        com.dotcms.contenttype.model.field.Field field;
        com.dotcms.contenttype.model.field.Field secondField;
        HashMap<String, List<String>> results;
        Relationship relationship;
        final int cardinality = RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal();

        try {
            parentContentType = createTestContentType("parentContentType", "parentContentType");

            //child field
            field = FieldBuilder.builder(RelationshipField.class).name("testRelationship")
                    .variable("testRelationship")
                    .contentTypeId(parentContentType.id()).values(String.valueOf(cardinality))
                    .relationType(parentContentType.variable()).build();

            field = fieldAPI.save(field, user);
            final String fullFieldVar =
                    parentContentType.variable() + StringPool.PERIOD + field.variable();
            relationship = relationshipAPI.byTypeValue(fullFieldVar);

            //parent field
            secondField = FieldBuilder.builder(RelationshipField.class)
                    .name("testRelationshipParent").variable("testRelationshipParent")
                    .contentTypeId(parentContentType.id()).values(String.valueOf(cardinality))
                    .relationType(fullFieldVar).build();

            secondField = fieldAPI.save(secondField, user);

            //Creates child contentlet
            final ContentletDataGen contentletDataGen = new ContentletDataGen(
                    parentContentType.id());
            contentletDataGen.languageId(defaultLanguage.getId());

            final Contentlet childContentlet = contentletDataGen
                    .setProperty(TITLE_FIELD_NAME, "child contentlet")
                    .setProperty(BODY_FIELD_NAME, "child contentlet").nextPersisted();

            final Contentlet parentContentlet = contentletDataGen
                    .setProperty(TITLE_FIELD_NAME, "parent contentlet")
                    .setProperty(BODY_FIELD_NAME, "parent contentlet").nextPersisted();

            //Creating csv
            final Reader reader = createTempFile(
                    TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + ", " + field.variable() + ", "
                            + secondField.variable()
                            + "\r\n" +
                            "Import related content test, Import related content test, "
                            + childContentlet.getIdentifier() + ", " + parentContentlet
                            .getIdentifier());

            results = importContentWithRelationships(parentContentType, reader,
                    new String[]{parentContentType.fieldMap().get(TITLE_FIELD_NAME).inode(),
                            parentContentType.fieldMap().get(BODY_FIELD_NAME).inode(),
                            field.inode(), secondField.inode()});

            validateRelationshipResults(relationship, parentContentlet, childContentlet, results);

        } finally {
            try {
                if (parentContentType != null) {
                    contentTypeApi.delete(parentContentType);
                }
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Method to test: {@link ImportUtil#importFile}
     * Case: Import file with legacy folder inode in the line (folder inode is different from the
     * folder identifier)
     * @throws DotSecurityException when there is a security exception
     * @throws DotDataException when there is a dotCMS data exception
     * @throws IOException when there is an IO exception
     */
    @Test
    public void importFile_success_when_lineContainsLegacyFolderInode()
            throws DotSecurityException, DotDataException, IOException {


        ContentType contentType = null;

        try {
            long time = System.currentTimeMillis();
            final String contentTypeName = "ContentTypeTestingWithOldFolderInode_" + time;
            final String contentTypeVarName = "velocityVarNameTestingWithOldFolderInode_" + time;

            // create content type
            contentType = createTestContentType(contentTypeName, contentTypeVarName);
            new FieldDataGen().contentTypeId(contentType.id())
                    .velocityVarName(SITE_FIELD_NAME)
                    .type(HostFolderField.class).nextPersisted();

            // create test folder with inode different from identifier
            final Folder folder = new FolderDataGen()
                    .name("import-folder-inode-test_" + time)
                    .site(defaultSite).next();
            final Identifier folderIdentifier = APILocator.getIdentifierAPI()
                    .createNew(folder, defaultSite);
            folder.setIdentifier(folderIdentifier.getId());
            folder.setInode(UUIDGenerator.generateUuid());
            APILocator.getFolderAPI().save(folder, user, false);
            final String folderInode = folder.getInode();

            // create test csv
            final Reader reader = createTempFile(
                    TITLE_FIELD_NAME + "," + BODY_FIELD_NAME + "," + SITE_FIELD_NAME
                            + "\r\n" +
                            "Folder inode test,Testing folder inode," + folderInode);
            final CsvReader csvreader = new CsvReader(reader);
            csvreader.setSafetySwitch(false);

            final String[] csvHeaders = csvreader.getHeaders();

            // import content
            final Map<String, List<String>> results = ImportUtil.importFile(
                    0L, defaultSite.getIdentifier(), contentType.inode(),
                            new String[]{contentType.fieldMap().get(TITLE_FIELD_NAME).inode()},
                    false, false, user, defaultLanguage.getId(),
                            csvHeaders, csvreader, -1, -1,
                            reader, null, getHttpRequest());
            // validations
            validate(results, false, false, false);

            final List<Contentlet> savedData = contentletAPI
                    .findByStructure(contentType.inode(), user, false, 0, 0);
            assertNotNull(savedData);
            assertEquals(1, savedData.size());
            final Contentlet contentlet = savedData.get(0);
            assertEquals(folderInode, contentlet.getFolder());

        } finally {
            try {
                if (contentType != null) {
                    contentTypeApi.delete(contentType);
                }
            } catch (Exception e) {
                Logger.error("Error deleting content type", e);
            }
        }
    }

    /**
     * Method to test: {@link ImportUtil#importFile}
     * Case: Import file including a page with URL field set to a valid URL
     * @throws DotSecurityException when there is a security exception
     * @throws DotDataException when there is a dotCMS data exception
     * @throws IOException when there is an IO exception
     */
    @Test
    public void importFile_PagesWithURL_success()
            throws DotSecurityException, DotDataException, IOException {

        Host site = null;
        Template template = null;
        ContentType pageType = null;
        Folder parentFolder = null;
        try {
            site = new SiteDataGen().nextPersisted();

            // create test template
            template = new TemplateDataGen()
                    .site(APILocator.systemHost())
                    .nextPersisted();
            final String templateId = template.getIdentifier();

            long time = System.currentTimeMillis();
            final String pageTypeName = "TestPageTypeWithURL_" + time;
            final String pageTypeVarName = "velocityVarNameTestPageTypeWithURL_" + time;

            // create test page type
            pageType = new ContentTypeDataGen()
                    .baseContentType(BaseContentType.HTMLPAGE)
                    .host(APILocator.systemHost())
                    .name(pageTypeName)
                    .velocityVarName(pageTypeVarName)
                    .nextPersisted();

            // create test folder
            final String parentFolderName = "test-base-folder_" + time;
            parentFolder = new FolderDataGen()
                    .name(parentFolderName)
                    .site(site)
                    .nextPersisted();
            final Folder subFolder = new FolderDataGen()
                    .name("test-sub-folder")
                    .parent(parentFolder)
                    .nextPersisted();

            final String testPageURL = StringPool.FORWARD_SLASH
                    + parentFolderName + "/test-sub-folder/test-page-1";
            final Reader reader = createTempFile(
                    HTMLPageAssetAPI.TITLE_FIELD
                            + "," + HTMLPageAssetAPI.URL_FIELD
                            + ",hostFolder"
                            + "," + HTMLPageAssetAPI.TEMPLATE_FIELD
                            + "," + HTMLPageAssetAPI.FRIENDLY_NAME_FIELD
                            + "," + HTMLPageAssetAPI.SORT_ORDER_FIELD
                            + "," + HTMLPageAssetAPI.CACHE_TTL_FIELD
                            + "\r\n" +
                            "Test Page 1," + testPageURL + ","
                                + site.getIdentifier() + "," + templateId
                                + ",Test Page 1,0,300\r\n");

            final CsvReader csvreader = new CsvReader(reader);
            csvreader.setSafetySwitch(false);

            final String[] csvHeaders = csvreader.getHeaders();

            final Map<String, List<String>> results = ImportUtil
                    .importFile(0L, defaultSite.getInode(), pageType.inode(),
                            new String[]{}, false, false,
                            user, defaultLanguage.getId(), csvHeaders, csvreader,
                            -1, -1,
                            reader, null, getHttpRequest());

            Logger.info(ImportUtilTest.class, "page errors: " + results.get("errors"));
            validate(results, false, false, true);

            final List<Contentlet> savedData = contentletAPI
                    .findByStructure(pageType.inode(), user, false, 0, 0);
            assertNotNull(savedData);
            assertEquals(1, savedData.size());

            final Contentlet contentlet = savedData.get(0);
            final Identifier identifier = APILocator.getIdentifierAPI().find(contentlet);
            assertNotNull(identifier);
            assertEquals(testPageURL, identifier.getURI());

        } finally {
            try {
                if (parentFolder != null) {
                    FolderDataGen.remove(parentFolder);
                }

                if (pageType != null) {
                    contentTypeApi.delete(pageType);
                }

                if (template != null) {
                    TemplateDataGen.remove(template);
                }

                if (null != site) {
                    APILocator.getHostAPI().archive(site, APILocator.systemUser(), false);
                    APILocator.getHostAPI().delete(site, APILocator.systemUser(), false);
                }

            } catch (Exception e) {
                Logger.error("Error deleting test page type", e);
            }

        }
    }

    /**
     *
     * @param relationship
     * @param parentContentlet
     * @param childContentlet
     * @param results
     * @throws DotDataException
     * @throws DotSecurityException
     */
    private void validateRelationshipResults(Relationship relationship, Contentlet parentContentlet,
            Contentlet childContentlet, Map<String, List<String>> results)
            throws DotDataException, DotSecurityException {
        //Validations
        validate(results, true, false, false);

        //Validate that contentlet has been created
        final List<String> lastInode = results.get("lastInode");
        assertNotNull(lastInode);
        assertEquals(1, lastInode.size());

        final Contentlet contentlet = contentletAPI.find(lastInode.get(0), user, false);

        //Validates the parent relationship has been added
        if (parentContentlet != null) {
            final List<Tree> parentTrees = relationshipAPI
                    .relatedContentTrees(relationship, contentlet, false);
            assertNotNull(parentTrees);
            assertEquals(1, parentTrees.size());
            assertEquals(parentContentlet.getIdentifier(), parentTrees.get(0).getParent());
        }

        //Validates the child relationship has been added
        if (childContentlet != null) {
            final List<Tree> childTrees = relationshipAPI
                    .relatedContentTrees(relationship, contentlet, true);
            assertNotNull(childTrees);
            assertEquals(1, childTrees.size());
            assertEquals(childContentlet.getIdentifier(), childTrees.get(0).getChild());
        }
    }

    /**
     *
     * @param type
     * @param reader
     * @param keyFields
     * @return
     * @throws IOException
     * @throws DotDataException
     */
    private HashMap<String, List<String>> importContentWithRelationships(ContentType type,
            Reader reader, final String[] keyFields) throws IOException, DotDataException {

        final CsvReader csvreader = new CsvReader(reader);
        csvreader.setSafetySwitch(false);

        final String[] csvHeaders = csvreader.getHeaders();

        //Preview=false
        return ImportUtil
                .importFile(0L, defaultSite.getInode(), type.inode(),
                        keyFields, false, false,
                        user, defaultLanguage.getId(), csvHeaders, csvreader, -1, -1,
                        reader, null,getHttpRequest());
    }

    /**
     * Create content type for import with different actions and with the schemeStepActionResult2
     * workflow associated
     *
     * @param contentTypeName Name of the new Content Type
     * @return a ContentType
     */
    private ContentType createTestContentType(final String contentTypeName,
            final String contentTypeVarName)
            throws DotDataException, DotSecurityException {

        List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();
        fields.add(
                ImmutableTextField.builder()
                        .name(TITLE_FIELD_NAME)
                        .variable(TITLE_FIELD_NAME)
                        .required(true)
                        .listed(true)
                        .indexed(true)
                        .sortOrder(1)
                        .fixed(true)
                        .searchable(true)
                        .values("")
                        .build());

        fields.add(
                ImmutableTextAreaField.builder()
                        .name(BODY_FIELD_NAME)
                        .variable(BODY_FIELD_NAME)
                        .required(false)
                        .listed(true)
                        .indexed(true)
                        .sortOrder(1)
                        .fixed(true)
                        .searchable(true)
                        .values("")
                        .build());

        ContentType contentType = new ContentTypeDataGen()
                .name(contentTypeName)
                .velocityVarName(contentTypeVarName)
                .host(APILocator.systemHost())
                .fields(fields)
                .nextPersisted();

        //refresh Content Type
        contentType = contentTypeApi.find(contentType.id());

        //Setting permissions permissions
        Permission p = new Permission(contentType.getPermissionId(), contributorRole.getId(),
                EDIT_PERMISSION, true);
        permissionAPI.save(p, contentType, user, true);

        p = new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(),
                contributorRole.getId(), EDIT_PERMISSION, true);
        permissionAPI.save(p, contentType, user, true);

        //Publish permission for any publisher
        p = new Permission(contentType.getPermissionId(), publisherRole.getId(),
                PUBLISH_PERMISSION, true);
        permissionAPI.save(p, contentType, user, true);

        p = new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(),
                publisherRole.getId(), PUBLISH_PERMISSION, true);
        permissionAPI.save(p, contentType, user, true);

        //Edit Prmission for reviewer
        p = new Permission(contentType.getPermissionId(), reviewerRole.getId(),
                EDIT_PERMISSION, true);
        permissionAPI.save(p, contentType, user, true);

        p = new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(),
                reviewerRole.getId(), EDIT_PERMISSION, true);
        permissionAPI.save(p, contentType, user, true);

        workflowAPI.saveSchemesForStruct(new StructureTransformer(contentType).asStructure(),
                Arrays.asList(schemeStepActionResult2.getScheme()));

        return contentType;
    }

    private void validateNoPermissionsWarning(String warning) {
        assertTrue(warning.contains("doesn't have permissions to execute"));
    }

    private HttpServletRequest getHttpRequest() {
        MockHeaderRequest request = new MockHeaderRequest(
                new MockSessionRequest(new MockAttributeRequest(new MockHttpRequestIntegrationTest("localhost", "/").request()).request())
                        .request());

        request.setHeader("Authorization", "Basic " + new String(Base64.encode("admin@dotcms.com:admin".getBytes())));
        request.setHeader("Origin", "localhost");
        request.setAttribute(WebKeys.USER,user);
        request.setAttribute(WebKeys.USER_ID,user.getUserId());

        return request;
    }

    /***
     * Creates a ContentType with a Title, Body and Binary Field
     * Creates a CSV with an URL on the Binary Field
     * Import the CSV and the Content should be created successfully and File downloaded
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void importFile_importBinaryFieldUsingURL_success()
            throws DotSecurityException, DotDataException, IOException {

        ContentType contentType = null;
        CsvReader csvreader;
        long time;
        HashMap<String, List<String>> results;
        Reader reader;
        String[] csvHeaders;
        com.dotcms.contenttype.model.field.Field titleField;

        try {
            time = System.currentTimeMillis();
            final String contentTypeName = "ContentTypeBinaryField" + time;
            final String contentTypeVarName = "contentTypeBinaryField" + time;

            //create content type
            contentType = createTestContentType(contentTypeName, contentTypeVarName);
            titleField = fieldAPI.byContentTypeAndVar(contentType, TITLE_FIELD_NAME);

            final com.dotcms.contenttype.model.field.Field binaryField = FieldBuilder.builder(BinaryField.class).name(BINARY_FIELD_NAME)
                    .contentTypeId(contentType.id()).variable(BINARY_FIELD_NAME).sortOrder(1).required(false)
                    .build();
            fieldAPI.save(binaryField,user);

            //Creating csv
            reader = createTempFile(TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + ", " + BINARY_FIELD_NAME + "\r\n" +
                    "test1" + time + ", " +
                    "test1" + time + ", " +
                    "https://raw.githubusercontent.com/dotCMS/core/main/dotCMS/src/main/webapp/html/images/skin/logo.gif");
            csvreader = new CsvReader(reader);
            csvreader.setSafetySwitch(false);
            csvHeaders = csvreader.getHeaders();

            results =
                    ImportUtil
                            .importFile(0L, defaultSite.getInode(), contentType.inode(),
                                    new String[]{titleField.id()}, false, false,
                                    user, defaultLanguage.getId(), csvHeaders, csvreader, -1,
                                    -1, reader,
                                    saveAsDraftAction.getId(), getHttpRequest());

            //Validations
            validate(results, false, false, false);

            assertEquals(results.get("warnings").size(), 0);
            assertEquals(results.get("errors").size(), 0);

            final List<Contentlet> savedData = contentletAPI
                    .findByStructure(contentType.inode(), user, false, 0, 0);
            assertNotNull(savedData);
            assertTrue(savedData.size() == 1);
            assertNotNull(savedData.get(0).getBinary(BINARY_FIELD_NAME));

        } finally {
            try {
                if (null != contentType) {
                    contentTypeApi.delete(contentType);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    /***
     * Creates a ContentType with a Title, Body and Binary Field
     * Creates a CSV with 3 Lines, each one returns a diff error 400, 404, and false (since URL does not starts with http or https)
     * Import the CSV and the importer should return with errors, so no content can be created and no file is downloaded.
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void importFile_importBinaryFieldUsingURL_failed_URLsareNotValid()
            throws DotSecurityException, DotDataException, IOException {

        ContentType contentType = null;
        CsvReader csvreader;
        long time;
        HashMap<String, List<String>> results;
        Reader reader;
        String[] csvHeaders;
        com.dotcms.contenttype.model.field.Field titleField;

        try {
            time = System.currentTimeMillis();
            final String contentTypeName = "ContentTypeBinaryField" + time;
            final String contentTypeVarName = "contentTypeBinaryField" + time;

            //create content type
            contentType = createTestContentType(contentTypeName, contentTypeVarName);
            titleField = fieldAPI.byContentTypeAndVar(contentType, TITLE_FIELD_NAME);

            final com.dotcms.contenttype.model.field.Field binaryField = FieldBuilder.builder(BinaryField.class).name(BINARY_FIELD_NAME)
                    .contentTypeId(contentType.id()).variable(BINARY_FIELD_NAME).sortOrder(1).required(false)
                    .build();
            fieldAPI.save(binaryField,user);

            //Creating csv
            reader = createTempFile(TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + ", " + BINARY_FIELD_NAME + "\r\n" +
                    "test400" + time + ", " +
                    "test400" + time + ", " +
                    "https://raw.githubusercontent.com/url/throws/400.jpg" + "\r\n" +
                    "test404" + time + ", " +
                    "test404" + time + ", " +
                    "https://raw.githubusercontent.com/dotCMS/core/throws/dotCMS/404.jpg" + "\r\n" +
                    "testDoesNotStartWithHTTPorHTTPS" + time + ", " +
                    "testDoesNotStartWithHTTPorHTTPS" + time + ", " +
                    "test://raw.githubusercontent.com/dotCMS/core/main/dotCMS/src/main/webapp/html/images/skin/logo.gif");
            csvreader = new CsvReader(reader);
            csvreader.setSafetySwitch(false);
            csvHeaders = csvreader.getHeaders();

            results =
                    ImportUtil
                            .importFile(0L, defaultSite.getInode(), contentType.inode(),
                                    new String[]{titleField.id()}, false, false,
                                    user, defaultLanguage.getId(), csvHeaders, csvreader, -1,
                                    -1, reader,
                                    saveAsDraftAction.getId(), getHttpRequest());

            //Validations
            validate(results, true, true, false);

            assertEquals(4,results.get("errors").size());//one for each line, and the fourth one is the summary

            final List<Contentlet> savedData = contentletAPI
                    .findByStructure(contentType.inode(), user, false, 0, 0);
            assertNotNull(savedData);
            assertTrue(savedData.isEmpty());

        } finally {
            try {
                if (null != contentType) {
                    contentTypeApi.delete(contentType);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /***
     * Creates a ContentType with a Title, Body and Binary Field
     * Creates a CSV with an empty Binary Field
     * Import the CSV and the Content should be created successfully
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void importFile_importContentWithOptionalBinaryField_success()
            throws DotSecurityException, DotDataException, IOException {

        ContentType contentType = null;
        CsvReader csvreader;
        long time;
        HashMap<String, List<String>> results;
        Reader reader;
        String[] csvHeaders;
        com.dotcms.contenttype.model.field.Field titleField;

        try {
            time = System.currentTimeMillis();
            final String contentTypeName = "ContentTypeBinaryField" + time;
            final String contentTypeVarName = "contentTypeBinaryField" + time;

            //create content type
            contentType = createTestContentType(contentTypeName, contentTypeVarName);
            titleField = fieldAPI.byContentTypeAndVar(contentType, TITLE_FIELD_NAME);

            final com.dotcms.contenttype.model.field.Field binaryField = FieldBuilder.builder(BinaryField.class).name(BINARY_FIELD_NAME)
                    .contentTypeId(contentType.id()).variable(BINARY_FIELD_NAME).sortOrder(1).required(false)
                    .build();
            fieldAPI.save(binaryField,user);

            //Creating csv
            reader = createTempFile(TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + ", " + BINARY_FIELD_NAME + "\r\n" +
                    "test1" + time + ", " +
                    "test1" + time + ", " +
                    "");
            csvreader = new CsvReader(reader);
            csvreader.setSafetySwitch(false);
            csvHeaders = csvreader.getHeaders();

            results =
                    ImportUtil
                            .importFile(0L, defaultSite.getInode(), contentType.inode(),
                                    new String[]{titleField.id()}, false, false,
                                    user, defaultLanguage.getId(), csvHeaders, csvreader, -1,
                                    -1, reader,
                                    saveAsDraftAction.getId(), getHttpRequest());

            //Validations
            validate(results, false, false, false);

            assertEquals(results.get("warnings").size(), 0);
            assertEquals(results.get("errors").size(), 0);

            final List<Contentlet> savedData = contentletAPI
                    .findByStructure(contentType.inode(), user, false, 0, 0);
            assertNotNull(savedData);
            assertTrue(savedData.size() == 1);
            assertNull(savedData.get(0).getBinary(BINARY_FIELD_NAME));

        } finally {
            try {
                if (null != contentType) {
                    contentTypeApi.delete(contentType);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /***
     * Creates a ContentType that has all the fields, but for this test
     * we'll only use the title and the categories fields.
     * Creates a content with a couple of categories.
     * Creates a CSV without the categories column.
     * Import the CSV and the Content should retain the original categories.
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void importLine_shouldRetainCategoriesIfHeaderNotSent() throws DotSecurityException, DotDataException, IOException {
        CsvReader csvreader;
        HashMap<String, List<String>> results;
        Reader reader;
        String[] csvHeaders;
        com.dotcms.contenttype.model.field.Field titleField;

        final Category parentCategory = TestDataUtils.createCategories();
        final List<Category> childCategories = APILocator.getCategoryAPI().getAllChildren(parentCategory,user,false);
        final ContentType contentType = TestDataUtils.newContentTypeFieldTypesGalore(parentCategory);
        titleField = fieldAPI.byContentTypeAndVar(contentType, "textField");

        final Optional<com.dotcms.contenttype.model.field.Field> categoryField = contentType.fields(CategoryField.class).stream()
                .findFirst();
        assertTrue(categoryField.isPresent());

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .languageId(1)
                .setProperty("textField", "test1")
                .addCategory(childCategories.get(0))
                .nextPersisted();

        //Creating csv
        reader = createTempFile("textField" + "\r\n" +
                "test1");
        csvreader = new CsvReader(reader);
        csvreader.setSafetySwitch(false);
        csvHeaders = csvreader.getHeaders();

        results =
                ImportUtil
                        .importFile(0L, defaultSite.getInode(), contentType.inode(),
                                new String[]{titleField.id()}, false, false,
                                user, defaultLanguage.getId(), csvHeaders, csvreader, -1,
                                -1, reader,
                                saveAsDraftAction.getId(), getHttpRequest());

        //Validations
        validate(results, false, false, true);

        final List<Contentlet> savedData = contentletAPI
                .findByStructure(contentType.inode(), user, false, 0, 0);
        assertNotNull(savedData);
        assertTrue(savedData.size() == 1);
        assertFalse(APILocator.getCategoryAPI().getParents(savedData.get(0),user,false).isEmpty());
    }


    /**
     * Method to test: This test tries the {@link ImportUtil#importFile}
     * Given Scenario: A Content type which has a binary field, the binary field is required. Will try to preview the import of a CSV with a URL pointing to a valid image.
     * ExpectedResult: The importer should return without errors, so content will be ready to be imported.
     */
    @Test
    public void importFile_importBinaryFieldUsingURLWithRequiredBinaryField_success()
            throws DotSecurityException, DotDataException, IOException {

        ContentType contentType = null;
        CsvReader csvreader;
        long time;
        HashMap<String, List<String>> results;
        Reader reader;
        String[] csvHeaders;
        com.dotcms.contenttype.model.field.Field titleField;

        try {
            time = System.currentTimeMillis();
            final String contentTypeName = "ContentTypeBinaryField" + time;
            final String contentTypeVarName = "contentTypeBinaryField" + time;

            //create content type
            contentType = createTestContentType(contentTypeName, contentTypeVarName);
            titleField = fieldAPI.byContentTypeAndVar(contentType, TITLE_FIELD_NAME);

            final com.dotcms.contenttype.model.field.Field binaryField = FieldBuilder.builder(BinaryField.class).name(BINARY_FIELD_NAME)
                    .contentTypeId(contentType.id()).variable(BINARY_FIELD_NAME).sortOrder(1).required(true)
                    .build();
            fieldAPI.save(binaryField,user);

            //Creating csv
            reader = createTempFile(TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + ", " + BINARY_FIELD_NAME + "\r\n" +
                    "test1" + time + ", " +
                    "test1" + time + ", " +
                    "https://raw.githubusercontent.com/dotCMS/core/main/dotCMS/src/main/webapp/html/images/skin/logo.gif");
            csvreader = new CsvReader(reader);
            csvreader.setSafetySwitch(false);
            csvHeaders = csvreader.getHeaders();

            results =
                    ImportUtil
                            .importFile(0L, defaultSite.getInode(), contentType.inode(),
                                    new String[]{titleField.id()}, true, false,
                                    user, defaultLanguage.getId(), csvHeaders, csvreader, -1,
                                    -1, reader,
                                    saveAsDraftAction.getId(), getHttpRequest());

            //Validations
            validate(results, true, false, false);

            assertEquals(results.get("warnings").size(), 0);
            assertEquals(results.get("errors").size(), 0);

        } finally {
            try {
                if (null != contentType) {
                    contentTypeApi.delete(contentType);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
