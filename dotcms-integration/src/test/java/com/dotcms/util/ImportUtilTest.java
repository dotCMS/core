package com.dotcms.util;

import com.dotcms.contenttype.business.ContentTypeAPIImpl;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.business.uniquefields.extratable.UniqueFieldDataBaseUtil;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.CategoryField;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.DateField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.ImageField;
import com.dotcms.contenttype.model.field.ImmutableTextAreaField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.JSONField;
import com.dotcms.contenttype.model.field.KeyValueField;
import com.dotcms.contenttype.model.field.RelationshipField;
import com.dotcms.contenttype.model.field.SelectField;
import com.dotcms.contenttype.model.field.StoryBlockField;
import com.dotcms.contenttype.model.field.TextAreaField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.field.TimeField;
import com.dotcms.contenttype.model.field.WysiwygField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.datagen.CategoryDataGen;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.datagen.FieldDataGen;
import com.dotcms.datagen.FolderDataGen;
import com.dotcms.datagen.LanguageDataGen;
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
import com.dotcms.variant.VariantAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.Tree;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.RelationshipAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.common.db.DotConnect;
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
import com.dotmarketing.util.ImmutableImportFileParams;
import com.dotmarketing.util.ImportUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.dotmarketing.util.WebKeys.Relationship.RELATIONSHIP_CARDINALITY;
import com.dotmarketing.util.importer.ImportResultConverter;
import com.dotmarketing.util.importer.model.ImportResult;
import com.dotmarketing.util.importer.model.ResultData;
import com.dotmarketing.util.importer.model.ValidationMessage;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.dotcms.util.CollectionsUtils.list;
import static com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants.WORKFLOW_PUBLISH_ACTION_ID;
import static com.dotmarketing.util.importer.ImportLineValidationCodes.DUPLICATE_UNIQUE_VALUE;
import static com.dotmarketing.util.importer.ImportLineValidationCodes.INVALID_BINARY_URL;
import static com.dotmarketing.util.importer.ImportLineValidationCodes.INVALID_CATEGORY_KEY;
import static com.dotmarketing.util.importer.ImportLineValidationCodes.INVALID_DATE_FORMAT;
import static com.dotmarketing.util.importer.ImportLineValidationCodes.INVALID_FILE_PATH;
import static com.dotmarketing.util.importer.ImportLineValidationCodes.INVALID_JSON;
import static com.dotmarketing.util.importer.ImportLineValidationCodes.INVALID_NUMBER_FORMAT;
import static com.dotmarketing.util.importer.ImportLineValidationCodes.INVALID_SITE_FOLDER_REF;
import static com.dotmarketing.util.importer.ImportLineValidationCodes.RELATIONSHIP_VALIDATION_ERROR;
import static com.dotmarketing.util.importer.ImportLineValidationCodes.REQUIRED_FIELD_MISSING;
import static com.dotmarketing.util.importer.ImportLineValidationCodes.UNREACHABLE_URL_CONTENT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
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
    private static final String UNIQUE_FIELDS_WARNING = "There are unique fields in this Content Type. Duplicate values are rejected during import.";

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

        return new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8);
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

        assertEquals(1, results.get("warnings").size());
        assertThat(results.get("warnings").get(0), allOf(containsString(UNIQUE_FIELDS_WARNING)));
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
            assertThat(results.get("warnings").get(0), allOf(containsString(UNIQUE_FIELDS_WARNING)));
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
                    Collections.singletonList(schemeStepActionResult1.getScheme()));

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

            assertEquals(2, results.get("warnings").size());
            assertThat(results.get("warnings").get(0), allOf(containsString(UNIQUE_FIELDS_WARNING)));
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
                    Collections.singletonList(schemeStepActionResult1.getScheme()));

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

            assertEquals(1, results.get("warnings").size());
            assertThat(results.get("warnings").get(0), allOf(containsString(UNIQUE_FIELDS_WARNING)));
            assertEquals(0, results.get("errors").size());
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
                    Collections.singletonList(schemeStepActionResult1.getScheme()));

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

            assertEquals(1, results.get("warnings").size());
            assertThat(results.get("warnings").get(0), allOf(containsString(UNIQUE_FIELDS_WARNING)));
            assertEquals(0, results.get("errors").size());
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
            validate(results, false, false, false);

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
                    Map.of(relationship, list(childContentlet)),
                    user, false);

            //Creating csv to update parent
            final Reader reader = createTempFile(
                    TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + ", " + field.variable()
                            + "\r\n" +
                            "parent contentlet, parent contentlet body, ");

            final Map results = importContentWithRelationships(parentContentType, reader,
                    new String[]{
                            parentContentType.fieldMap().get(TITLE_FIELD_NAME).inode()});

            validate(results, false,false, false);

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
                    Map.of(relationship, list(childContentlet)),
                    user, false);

            //Creating csv to update parent
            final Reader reader = createTempFile(
                    TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + ", " + field.variable()
                            + "\r\n" +
                            "parent contentlet, parent contentlet body, " + childContentlet.getIdentifier());

            final Map results = importContentWithRelationships(parentContentType, reader,
                    new String[]{
                            parentContentType.fieldMap().get(TITLE_FIELD_NAME).inode()});

            validate(results, false,false, false);
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
                    Map.of(relationship, list(childContentlet)),
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
                Logger.error("Error deleting content type", e);
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
        long time;
        ImportResult results;
        Reader reader;
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

            results = importAndValidate(contentType, titleField, reader, false, 1);

            final ResultData data = results.data().orElse(null);
            assertNotNull(data);
            //Validations
            assertEquals(3, results.error().size()); //one for each line
            assertEquals(0, results.warning().size());
            assertEquals(0, data.summary().commits());

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
                Logger.error("Error deleting content type", e);
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

            assertEquals(0, results.get("warnings").size());
            assertEquals(0, results.get("errors").size());

            final List<Contentlet> savedData = contentletAPI
                    .findByStructure(contentType.inode(), user, false, 0, 0);
            assertNotNull(savedData);
            assertEquals(1, savedData.size());
            assertNull(savedData.get(0).getBinary(BINARY_FIELD_NAME));

        } finally {
            try {
                if (null != contentType) {
                    contentTypeApi.delete(contentType);
                }
            } catch (Exception e) {
                Logger.error("Error deleting content type", e);
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
        assertEquals(1, savedData.size());
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
                Logger.error("Error deleting content type", e);
            }
        }
    }
    /**
     * Method to test: This test tries the {@link ImportUtil#importFile}
     * Given Scenario: A parent content type related to a child content type that has two versions in two different languages
     * ExpectedResult: The importer should return without errors, so content will be ready to be imported.
     */
    @Test
    public void importPreviewRelationshipLanguageTest() throws DotDataException, DotSecurityException, IOException {
        //Creates content types
        ContentType parentContentType = null;
        ContentType childContentType  = null;

        HashMap<String, List<String>> results;
        CsvReader csvreader;
        Reader reader;
        String[] csvHeaders;
        final int cardinality = RELATIONSHIP_CARDINALITY.ONE_TO_MANY.ordinal();

        final Language language_1 = new LanguageDataGen().nextPersisted();
        final Language language_2 = new LanguageDataGen().nextPersisted();

        try {
            final Relationship relationship;
            parentContentType = createTestContentType("parentContentType", "parentContentType" + new Date().getTime());
            childContentType = createTestContentType("childContentType", "childContentType" + new Date().getTime());


            com.dotcms.contenttype.model.field.Field field = FieldBuilder.builder(RelationshipField.class).name("testRelationship")
                    .variable("testRelationship")
                    .contentTypeId(parentContentType.id()).values(String.valueOf(cardinality))
                    .relationType(childContentType.variable()).build();

            field = fieldAPI.save(field, user);
            relationship = relationshipAPI.byTypeValue(
                    parentContentType.variable() + StringPool.PERIOD + field.variable());


            //Creates child contentlet
            final Contentlet childContentlet = new ContentletDataGen(childContentType.id())
                    .languageId(language_1.getId())
                    .setProperty(TITLE_FIELD_NAME, "child contentlet")
                    .setProperty(BODY_FIELD_NAME, "child contentlet").nextPersisted();

            ContentletDataGen.createNewVersion(childContentlet, VariantAPI.DEFAULT_VARIANT, language_2, null);

            //Creates parent contentlet
            Contentlet parentContentlet = new ContentletDataGen(parentContentType.id())
                    .languageId(language_1.getId())
                    .setProperty(TITLE_FIELD_NAME, "parent contentlet")
                    .setProperty(BODY_FIELD_NAME, "parent contentlet").next();

            parentContentlet = contentletAPI.checkin(parentContentlet,
                    Map.of(relationship, list(childContentlet)),
                    user, false);

            reader = createTempFile(
                    "identifier, languageCode, countryCode, " + TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME
                            + "\r\n"
                            + parentContentlet.getIdentifier() + ",en, US, Test1_edited, " + "\r\n" );
            csvreader = new CsvReader(reader);
            csvreader.setSafetySwitch(false);
            csvHeaders = csvreader.getHeaders();

            int languageCodeHeaderColumn = 0;
            int countryCodeHeaderColumn = 1;


            results = ImportUtil.importFile(0L, defaultSite.getInode(), parentContentType.inode(),
                    new String[]{}, true, true, user, language_1.getId(), csvHeaders,
                    csvreader, languageCodeHeaderColumn, countryCodeHeaderColumn, reader,
                    schemeStepActionResult1.getAction().getId(),getHttpRequest());

            validate(results, true, false, true);

            assertEquals(results.get("errors").size(), 0);
        }finally {
            try {
                if (parentContentType != null) {
                    contentTypeApi.delete(parentContentType);
                }

                if (childContentType != null) {
                    contentTypeApi.delete(childContentType);
                }
            } catch (Exception e) {
                Logger.error("Error deleting content type", e);
            }
        }
    }

    /**
     * Given Scenario: A parent content type related to a child content type with a one-to-one relationship,
     * where both the parent and child have two versions in two different languages
     * ExpectedResult: The importer should return without errors, so content will be ready to be imported.
     */
    @Test
    public void importPreviewRelationshipLanguageOneToOneTest() throws DotDataException, DotSecurityException, IOException {
        //Creates content types
        ContentType parentContentType = null;
        ContentType childContentType  = null;

        HashMap<String, List<String>> results;
        CsvReader csvreader;
        Reader reader;
        String[] csvHeaders;
        final int cardinality = RELATIONSHIP_CARDINALITY.ONE_TO_ONE.ordinal();

        final Language language_1 = new LanguageDataGen().nextPersisted();
        final Language language_2 = new LanguageDataGen().nextPersisted();

        try {
            final Relationship relationship;
            parentContentType = createTestContentType("parentContentType", "parentContentType" + new Date().getTime());
            childContentType = createTestContentType("childContentType", "childContentType" + new Date().getTime());


            com.dotcms.contenttype.model.field.Field field = FieldBuilder.builder(RelationshipField.class).name("testRelationship")
                    .variable("testRelationship")
                    .contentTypeId(parentContentType.id()).values(String.valueOf(cardinality))
                    .relationType(childContentType.variable()).build();

            field = fieldAPI.save(field, user);
            relationship = relationshipAPI.byTypeValue(
                    parentContentType.variable() + StringPool.PERIOD + field.variable());


            //Creates child contentlet with 2 language versions
            final Contentlet childContentlet = new ContentletDataGen(childContentType.id())
                    .languageId(language_1.getId())
                    .setProperty(TITLE_FIELD_NAME, "child contentlet")
                    .setProperty(BODY_FIELD_NAME, "child contentlet").nextPersisted();

            ContentletDataGen.createNewVersion(childContentlet, VariantAPI.DEFAULT_VARIANT, language_2, null);

            //Creates parent contentlet with 2 language versions
            Contentlet parentContentlet = new ContentletDataGen(parentContentType.id())
                    .languageId(language_1.getId())
                    .setProperty(TITLE_FIELD_NAME, "parent contentlet")
                    .setProperty(BODY_FIELD_NAME, "parent contentlet").next();

            parentContentlet = contentletAPI.checkin(parentContentlet,
                    Map.of(relationship, list(childContentlet)),
                    user, false);

            //Create second language version of parent
            ContentletDataGen.createNewVersion(parentContentlet, VariantAPI.DEFAULT_VARIANT, language_2, null);

            reader = createTempFile(
                    "identifier, languageCode, countryCode, " + TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME
                            + "\r\n"
                            + parentContentlet.getIdentifier() + ",en, US, Test1_edited, " + "\r\n" );
            csvreader = new CsvReader(reader);
            csvreader.setSafetySwitch(false);
            csvHeaders = csvreader.getHeaders();

            int languageCodeHeaderColumn = 0;
            int countryCodeHeaderColumn = 1;


            results = ImportUtil.importFile(0L, defaultSite.getInode(), parentContentType.inode(),
                    new String[]{}, true, true, user, language_1.getId(), csvHeaders,
                    csvreader, languageCodeHeaderColumn, countryCodeHeaderColumn, reader,
                    schemeStepActionResult1.getAction().getId(),getHttpRequest());

            validate(results, true, false, true);

            assertEquals(0, results.get("errors").size());
        }finally {
            try {
                if (parentContentType != null) {
                    contentTypeApi.delete(parentContentType);
                }

                if (childContentType != null) {
                    contentTypeApi.delete(childContentType);
                }
            } catch (Exception e) {
                Logger.error("Error deleting content type", e);
            }
        }
    }


    /**
     * Tests importing a CSV file with invalid null values for required fields
     * Tests both scenarios: stopping on error and continuing despite errors
     * Given: A content type with a required title field is created and a CSV file is generated with different commit granularities and errors
     * Expected: Depending on the commit granularity and the errors, the importer should commit and rollback as expected
     */
    @Test
    public void importFile_whenRequiredFieldIsNull_shouldHandleErrorsBasedOnStopFlag()
            throws DotSecurityException, DotDataException, IOException {

        ContentType contentType = null;
        ImportResult results;
        Reader reader;
        com.dotcms.contenttype.model.field.Field titleField;

        try {
            final String contentTypeName = "ContentTypeWithRequired_" + System.currentTimeMillis();
            final String contentTypeVarName = "velocityVarNameRequired_" + System.currentTimeMillis();

            // Create oneInvalidRowAndFourGood type
            contentType = createTestContentType(contentTypeName, contentTypeVarName);
            titleField = fieldAPI.byContentTypeAndVar(contentType, TITLE_FIELD_NAME);

            // Make title field required
            titleField = FieldBuilder.builder(TextField.class)
                    .from(titleField)
                    .required(true)
                    .build();
            fieldAPI.save(titleField, user);

            // Creating CSV with valid and invalid rows
            final String oneInvalidRowAndFourGood = TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + "\r\n" +
                    "Valid Title 1, Valid Body 1\r\n" +
                    "Valid Title 2, Valid Body 2\r\n" +
                    "Valid Title 3, Valid Body 3\r\n" +
                    ", Invalid Row Missing Required Title\r\n" + // This row should cause an error
                    "Valid Title 4, Valid Body 4\r\n";
            reader = createTempFile(oneInvalidRowAndFourGood);

            results = importAndValidate(contentType, titleField, reader, false, 1);

            // Should only have imported the first row before stopping
            List<Contentlet> savedData = contentletAPI.findByStructure(contentType.inode(), user, false, 0, 0);
            assertNotNull(savedData);

            final ResultData data = results.data().orElse(null);
            assertNotNull(data);

            assertEquals(4, savedData.size());
            assertEquals(1, data.summary().rollbacks());
            assertEquals(4, data.summary().commits());

        } finally {
            try {
                if (null != contentType) {
                    contentTypeApi.delete(contentType);
                }
            } catch (Exception e) {
                Logger.error("Error deleting content type", e);
            }
        }
    }

    /**
     * Extended test cases that verify savepoint and rollback functionality with
     * different commit granularity settings and error scenarios
     * Given: A content type with a required title field is created and a CSV file is generated with different commit granularities and errors
     * Expected: Depending on the commit granularity and the errors, the importer should commit and rollback as expected
     */
    @Test
    public void importFile_withDifferentCommitGranularities_shouldHandleErrorsCorrectly()
            throws DotSecurityException, DotDataException, IOException {

        ContentType contentType = null;
        Reader reader;
        com.dotcms.contenttype.model.field.Field titleField;

        try {
            final String contentTypeName = "CommitGranularityTest_" + System.currentTimeMillis();
            final String contentTypeVarName = "velocityVarNameGranularity_" + System.currentTimeMillis();

            // Create content type for testing
            contentType = createTestContentType(contentTypeName, contentTypeVarName);
            titleField = fieldAPI.byContentTypeAndVar(contentType, TITLE_FIELD_NAME);

            // Make title field required
            titleField = FieldBuilder.builder(TextField.class)
                    .from(titleField)
                    .required(true)
                    .build();
            fieldAPI.save(titleField, user);

            // Test Case 1: Commit every 2 records with 1 error (stopOnError=false)
            // Expected: 3 commits, 1 rollback
            String csvWithOneError = TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + "\r\n" +
                    "Title 1, Body 1\r\n" +
                    "Title 2, Body 2\r\n" +
                    "Title 3, Body 3\r\n" +
                    ", Missing Required Title\r\n" + // This row will cause an error
                    "Title 4, Body 4\r\n" +
                    "Title 5, Body 5\r\n";

            reader = createTempFile(csvWithOneError);
            ImportResult resultCase1 = importAndValidate(contentType, titleField, reader, false, 2);

            // Validate results for Test Case 1
            // With granularity=2, we should have commits at rows 2, 4, and 6, with a rollback at row 4
            List<Contentlet> savedDataCase1 = contentletAPI.findByStructure(contentType.inode(), user, false, 0, 0);
            assertNotNull(savedDataCase1);

            final ResultData resultData1 = resultCase1.data().orElse(null);
            assertNotNull(resultData1);

            assertEquals(5, savedDataCase1.size()); // All valid rows (1, 2, 3, 4, 5)
            assertEquals(1, resultData1.summary().rollbacks()); // 1 error → 1 rollback
            assertEquals(3, resultData1.summary().commits()); // We should have 3 commits with granularity=2

            // Delete all contentlets to prepare for next test
            for (Contentlet contentlet : savedDataCase1) {
                contentletAPI.archive(contentlet, user, false);
                contentletAPI.delete(contentlet, user, false);
            }

            // Test Case 2: Commit every 3 records with 2 errors at rows 4 and 6 (stopOnError=false)
            // Expected: 2 commits, 2 rollbacks
            String csvWithTwoErrors = TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + "\r\n" +
                    "Title A, Body A\r\n" +
                    "Title B, Body B\r\n" +
                    "Title C, Body C\r\n" +
                    ", Missing Title 1\r\n" + // First error
                    "Title D, Body D\r\n" +
                    ", Missing Title 2\r\n" + // Second error
                    "Title E, Body E\r\n" +
                    "Title F, Body F\r\n";

            reader = createTempFile(csvWithTwoErrors);
            ImportResult resultCase2 = importAndValidate(contentType, titleField, reader, false, 3);

            // Validate results for Test Case 2
            // With granularity=3, we should have commits at rows 3, 6, and 9
            // With errors at rows 4 and 6, we should have rollbacks for each error
            List<Contentlet> savedDataCase2 = contentletAPI.findByStructure(contentType.inode(), user, false, 0, 0);
            assertNotNull(savedDataCase2);

            final ResultData resultDataCase2 = resultCase2.data().orElse(null);
            assertNotNull(resultDataCase2);

            assertEquals(6, savedDataCase2.size()); // All valid rows (A, B, C, D, E, F)
            assertEquals(2, resultDataCase2.summary().rollbacks()); // 2 errors → 2 rollbacks
            assertEquals(2, resultDataCase2.summary().commits()); // 2 commits with granularity=3 because there were two rollbacks in a total of 8 rows

            // Delete all contentlets for next test
            for (Contentlet contentlet : savedDataCase2) {
                contentletAPI.archive(contentlet, user, false);
                contentletAPI.delete(contentlet, user, false);
            }

            // Test Case 3: Commit every record (granularity=1) with 1 error (stopOnError=true)
            // Expected: Should stop at the error, only records before the error should be saved
            String csvWithErrorAndStop = TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + "\r\n" +
                    "Record 1, Body 1\r\n" +
                    "Record 2, Body 2\r\n" +
                    "Record 3, Body 3\r\n" +
                    ", Missing Title Error\r\n" + // Error here, should stop
                    "Record 4, Body 4\r\n" + // This and subsequent records should not be processed
                    "Record 5, Body 5\r\n";

            reader = createTempFile(csvWithErrorAndStop);
            ImportResult resultCase3 = importAndValidate(contentType, titleField, reader, true, 1);

            // Validate results for Test Case 3
            // With stopOnError=true, should only process rows 1-3, error at row 4, then stop
            List<Contentlet> savedDataCase3 = contentletAPI.findByStructure(contentType.inode(), user, false, 0, 0);
            assertNotNull(savedDataCase3);

            final ResultData resultDataCase3 = resultCase3.data().orElse(null);
            assertNotNull(resultDataCase3);

            assertEquals(3, savedDataCase3.size()); // Only rows before the error (1, 2, 3)
            assertEquals(0, resultDataCase3.summary().rollbacks()); // No rollbacks with stopOnError=true
            assertEquals(3, resultDataCase3.summary().commits()); // 3 commits (1 per row)

            // Delete all contentlets
            for (Contentlet contentlet : savedDataCase3) {
                contentletAPI.archive(contentlet, user, false);
                contentletAPI.delete(contentlet, user, false);
            }

            // Test Case 4: Commit rarely (granularity=5) with errors in the middle of a batch
            // Expected: One commit at the end, with one rollback for the error
            String csvWithErrorInBatch = TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + "\r\n" +
                    "Batch 1, Body 1\r\n" +
                    "Batch 2, Body 2\r\n" +
                    "Batch 3, Body 3\r\n" +
                    ", Error in Batch\r\n" + // Error in the middle of batch
                    "Batch 4, Body 4\r\n" +
                    "Batch 5, Body 5\r\n" +
                    "Batch 6, Body 6\r\n";

            reader = createTempFile(csvWithErrorInBatch);
            ImportResult resultCase4 = importAndValidate(contentType, titleField, reader, false, 5);

            // Validate results for Test Case 4
            // With granularity=5, should have commits at rows 5 and at the end (row 7)
            List<Contentlet> savedDataCase4 = contentletAPI.findByStructure(contentType.inode(), user, false, 0, 0);
            assertNotNull(savedDataCase4);

            final ResultData resultDataCase4 = resultCase4.data().orElse(null);
            assertNotNull(resultDataCase4);

            assertEquals(6, savedDataCase4.size()); // All valid rows
            assertEquals(1, resultDataCase4.summary().rollbacks()); // 1 error → 1 rollback
            assertEquals(2, resultDataCase4.summary().commits()); // 2 commit with granularity=5 (1 at row 5, 1 at row 7)

        } finally {
            try {
                if (null != contentType) {
                    contentTypeApi.delete(contentType);
                }
            } catch (Exception e) {
                Logger.error("Error deleting content type", e);
            }
        }
    }

    /**
     * Tests behavior when multiple errors occur in the same commit batch
     * Verifies that all rows up to the batch with errors are committed
     * Given: A content type with a required title field is created and a CSV file is generated with different commit granularities and errors
     * Expected: Depending on the commit granularity and the errors, the importer should commit and rollback as expected
     */
    @Test
    public void importFile_withMultipleErrorsInSameBatch_shouldRollbackCorrectly()
            throws DotSecurityException, DotDataException, IOException {

        ContentType contentType = null;
        Reader reader;
        com.dotcms.contenttype.model.field.Field titleField;

        try {
            final String contentTypeName = "MultiErrorBatchTest_" + System.currentTimeMillis();
            final String contentTypeVarName = "velocityVarNameMultiError_" + System.currentTimeMillis();

            // Create content type for testing
            contentType = createTestContentType(contentTypeName, contentTypeVarName);
            titleField = fieldAPI.byContentTypeAndVar(contentType, TITLE_FIELD_NAME);

            // Make title field required
            titleField = FieldBuilder.builder(TextField.class)
                    .from(titleField)
                    .required(true)
                    .build();
            fieldAPI.save(titleField, user);

            // Create CSV with multiple errors in the same batch
            // Records 4, 5, and 6 all have errors
            String csvWithMultipleErrors = TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + "\r\n" +
                    "Multi 1, Body 1\r\n" +
                    "Multi 2, Body 2\r\n" +
                    "Multi 3, Body 3\r\n" +
                    ", Error 1\r\n" + // First error
                    ", Error 2\r\n" + // Second error
                    ", Error 3\r\n" + // Third error
                    "Multi 4, Body 4\r\n" +
                    "Multi 5, Body 5\r\n";

            reader = createTempFile(csvWithMultipleErrors);

            // Set commit granularity to 4, so rows 1-4 should be in the first batch
            // The first error is at row 4, which should cause a rollback
            ImportResult result = importAndValidate(contentType, titleField, reader, false, 4);

            // Validate results
            List<Contentlet> savedData = contentletAPI.findByStructure(contentType.inode(), user, false, 0, 0);
            assertNotNull(savedData);
            assertEquals(5, savedData.size()); // Should have imported rows 1, 2, 3, 7, 8

            final ResultData data = result.data().orElse(null);
            assertNotNull(data);

            assertEquals(3, data.summary().rollbacks()); // 3 errors → 3 rollbacks
            assertEquals(2, data.summary().commits()); // 2 commits with granularity=4

        } finally {
            try {
                if (null != contentType) {
                    contentTypeApi.delete(contentType);
                }
            } catch (Exception e) {
                Logger.error("Error deleting content type", e);
            }
        }
    }

    /**
     * Tests the interaction between commit granularity and stopOnError
     * When stopOnError=true, processing should stop at the first error,
     * Given: A content type with a required title field is created and a CSV file is generated with different commit granularities and errors
     * Expected: Depending on the commit granularity and the errors, the importer should commit and rollback as expected
     */
    @Test
    public void importFile_withHighGranularityAndStopOnError_shouldStopAtFirstError()
            throws DotSecurityException, DotDataException, IOException {

        ContentType contentType = null;
        Reader reader;
        com.dotcms.contenttype.model.field.Field titleField;

        try {
            final String contentTypeName = "StopErrorTest_" + System.currentTimeMillis();
            final String contentTypeVarName = "velocityVarNameStopError_" + System.currentTimeMillis();

            // Create content type for testing
            contentType = createTestContentType(contentTypeName, contentTypeVarName);
            titleField = fieldAPI.byContentTypeAndVar(contentType, TITLE_FIELD_NAME);

            // Make title field required
            titleField = FieldBuilder.builder(TextField.class)
                    .from(titleField)
                    .required(true)
                    .build();
            fieldAPI.save(titleField, user);

            // Create CSV with an error in the middle
            String csvWithError = TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + "\r\n" +
                    "Stop 1, Body 1\r\n" +
                    "Stop 2, Body 2\r\n" +
                    "Stop 3, Body 3\r\n" +
                    ", Missing Title\r\n" + // Error here, should stop
                    "Stop 4, Body 4\r\n" +
                    "Stop 5, Body 5\r\n" +
                    "Stop 6, Body 6\r\n" +
                    "Stop 7, Body 7\r\n" +
                    "Stop 8, Body 8\r\n" +
                    "Stop 9, Body 9\r\n" +
                    "Stop 10, Body 10\r\n";

            reader = createTempFile(csvWithError);

            // Set commit granularity to 10, but with stopOnError=true
            // Should stop at the first error (row 4) regardless of granularity
            ImportResult result = importAndValidate(contentType, titleField, reader, true, 10);

            // Validate results
            List<Contentlet> savedData = contentletAPI.findByStructure(contentType.inode(), user, false, 0, 0);
            assertNotNull(savedData);

            // Should only have rows before the error (1, 2, 3)
            assertEquals(3, savedData.size());

            final ResultData data = result.data().orElse(null);
            assertNotNull(data);

            // With stopOnError=true, we don't do rollbacks, we just stop
            assertEquals(0, data.summary().rollbacks());

            // No commits would occur with granularity=10 if we stopped at row 4
            // The implementation might perform a final commit of successfully processed rows
            // So commits should be either 0 or 1 depending on implementation
            assertTrue("Expected 0 or 1 commits, but got: " + data.summary().commits(), data.summary().commits() <= 1);

        } finally {
            try {
                if (null != contentType) {
                    contentTypeApi.delete(contentType);
                }
            } catch (Exception e) {
                Logger.error("Error deleting content type", e);
            }
        }
    }


    /**
     * Tests the interaction between commit granularity and stopOnError
     * When stopOnError=true, processing should stop at the first error,
     * Given: A content type with a required title and site field is created and a CSV file is generated with different commit granularities and errors
     * Expected: Depending on the commit granularity and the errors, the importer should commit and rollback as expected
     */
    @Test
    public void importFile_withHighGranularityAndStopOnError_shouldStopAtFirstInvalidLocationError()
            throws DotSecurityException, DotDataException, IOException {

        ContentType contentType = null;
        Reader reader;
        com.dotcms.contenttype.model.field.Field titleField;

        try {
            final String contentTypeName = "StopErrorTest_" + System.currentTimeMillis();
            final String contentTypeVarName = "velocityVarNameStopError_" + System.currentTimeMillis();

            // Create content type for testing
            contentType = createTestContentType(contentTypeName, contentTypeVarName);
            titleField = fieldAPI.byContentTypeAndVar(contentType, TITLE_FIELD_NAME);

            // Make title field required
            titleField = FieldBuilder.builder(TextField.class)
                    .from(titleField)
                    .required(true)
                    .build();
            fieldAPI.save(titleField, user);

            FieldBuilder.builder(HostFolderField.class)
                    .from(titleField)
                    .required(true)
                    .build();

            var hostField = FieldBuilder.builder(HostFolderField.class)
                    .name(SITE_FIELD_NAME)
                    .variable(SITE_FIELD_NAME)
                    .contentTypeId(contentType.id())
                    .dataType(DataTypes.TEXT)
                    .build();
            fieldAPI.save(hostField, user);

            // Create CSV with an error in the middle
            String csvWithError = SITE_FIELD_NAME + ", " + TITLE_FIELD_NAME + ", " + BODY_FIELD_NAME + "\r\n" +
                    defaultSite.getIdentifier() + ",Stop 1, Body 1\r\n" +
                    defaultSite.getIdentifier() + "2,Stop 2, Body 2\r\n" + // Invalid host
                    defaultSite.getIdentifier() + ",Stop 3, Body 3\r\n";

            reader = createTempFile(csvWithError);

            // Set commit granularity to 10, but with stopOnError=true
            // Should stop at the first error (row 4) regardless of granularity
            ImportResult result = importAndValidate(contentType, titleField, reader, true, 10);

            // Validate results
            List<Contentlet> savedData = contentletAPI.findByStructure(contentType.inode(), user, false, 0, 0);
            assertNotNull(savedData);

            // Should only have rows before the error (1)
            assertEquals(1, savedData.size());

            final ResultData data = result.data().orElse(null);
            assertNotNull(data);

            // With stopOnError=true, we don't do rollbacks, we just stop
            assertEquals(0, data.summary().rollbacks());

            // No commits would occur with granularity=10 if we stopped at row 4
            // The implementation might perform a final commit of successfully processed rows
            // So commits should be either 0 or 1 depending on implementation
            assertTrue("Expected 0 or 1 commits, but got: " + data.summary().commits(), data.summary().commits() <= 1);

            assertEquals(1, data.summary().createdContent());
            assertEquals(1, data.summary().failedDisplay());
            assertEquals(0, data.summary().updatedContent());

            final var error = result.error().get(0);
            assertEquals(INVALID_SITE_FOLDER_REF.name(), error.code().orElse(null));
            assertEquals(SITE_FIELD_NAME, error.field().orElse(null));


            var info = result.info();
            assertNotNull(info);
            assertEquals("Import statistics: 1 successful imports, 1 failed rows, 1 commits, 0 rollbacks", info.get(0).message());

        } finally {
            try {
                if (null != contentType) {
                    contentTypeApi.delete(contentType);
                }
            } catch (Exception e) {
                Logger.error("Error deleting content type", e);
            }
        }
    }

    /**
     * Helper method to perform the import and basic validation
     * @param contentType
     * @param titleField
     * @param reader
     * @param stopOnError
     * @param commitGranularity
     * @return
     * @throws IOException
     * @throws DotDataException
     */
    private ImportResult importAndValidate(
            final ContentType contentType,
            final com.dotcms.contenttype.model.field.Field titleField,
            final Reader reader,
            final boolean stopOnError,
            final int commitGranularity
    ) throws IOException, DotDataException{
        return importAndValidate(
                contentType,
                titleField,
                reader,
                stopOnError,
                commitGranularity,
                null
        );
    }

    /**
     * Helper method to perform the import and basic validation
     */
    private ImportResult importAndValidate(
            final ContentType contentType,
            final com.dotcms.contenttype.model.field.Field titleField,
            final Reader reader,
            final boolean stopOnError,
            final int commitGranularity,
            final String workflowActionId
    ) throws IOException, DotDataException {

        CsvReader csvreader = new CsvReader(reader);
        csvreader.setSafetySwitch(false);
        String[] csvHeaders = csvreader.getHeaders();
        final ImmutableImportFileParams importFileParams = ImmutableImportFileParams.builder()
                .importId(0L)
                .siteId(defaultSite.getIdentifier())
                .contentTypeInode(contentType.inode())
                .keyFields(titleField.id())
                .user(user)
                .language(defaultLanguage.getId())
                .csvHeaders(csvHeaders)
                .csvReader(csvreader)
                .request(getHttpRequest())
                .stopOnError(stopOnError)
                .commitGranularityOverride(commitGranularity)
                .workflowActionId(workflowActionId)
                .build();
        return ImportUtil.importFileResult(importFileParams);
    }

    /**
     * Method to test: {@link ImportUtil#importFile(Long, String, String, String[], boolean, boolean, User, long, String[], CsvReader, int, int, Reader, String, HttpServletRequest)}
     * When:
     * - Create a ContentType with a unique fields
     * - Create a Contentlet with a value equals to 'A' in the unique fields value
     * - Run the import with a file with 2 Contentlets with "A" and "B" as unique field values
     * - Should create both Contentlets
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void testingImportWithUniqueFields() throws DotSecurityException, DotDataException, IOException {
        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        uniqueFieldDataBaseUtil.createUniqueFieldsValidationTable();

        com.dotcms.contenttype.model.field.Field titleField = new FieldDataGen()
                .name("title").velocityVarName("title").type(TextField.class).next();

        com.dotcms.contenttype.model.field.Field uniqueField = new FieldDataGen()
                .name("unique").type(TextField.class).unique(true).next();

        ContentType contentType = new ContentTypeDataGen().field(titleField).field(uniqueField).nextPersisted();

        titleField = fieldAPI.byContentTypeAndVar(contentType, titleField.variable());
        uniqueField = fieldAPI.byContentTypeAndVar(contentType, uniqueField.variable());

        String csvWContent = "A, A" + "\r\n" +
                "B, B" + "\r\n";

        final Reader reader = createTempFile(csvWContent);

        final CsvReader csvreader = new CsvReader(reader);
        csvreader.setSafetySwitch(false);
        final String[] csvHeaders = new String[]{titleField.variable(), uniqueField.variable()};

        final  HashMap<String, List<String>> imported = ImportUtil.importFile(0L, defaultSite.getInode(),
                contentType.inode(),
                new String[]{titleField.id(), uniqueField.id()}, false, false,
                user, defaultLanguage.getId(), csvHeaders, csvreader, -1,
                -1, reader,
                schemeStepActionResult1.getAction().getId(), getHttpRequest());

        //Checking import result
        final List<String> results = imported.get("results");
        assertEquals(2, results.size());

        final String expectedMessage = String.format("2 New \"%s\" were created.", contentType.name());
        assertTrue(String.format("Expected Message %s, real messages (%s)", expectedMessage, results),
                results.contains(expectedMessage));

        final List<String> errors = imported.get("errors");
        assertTrue( errors.isEmpty());

        final List<Contentlet> contentlets = APILocator.getContentletAPI().findByStructure(contentType.inode(),
                APILocator.systemUser(), false, -1, 0);

        assertEquals(2, contentlets.size());

        final List<String> titles = contentlets.stream()
                .map(Contentlet::getTitle)
                .collect(Collectors.toList());

        assertTrue(titles.contains("A"));
        assertTrue(titles.contains("B"));

        //Checking unique_fields table
        List<Map<String, Object>> maps = new DotConnect().setSQL("SELECT * FROM unique_fields " +
                        "WHERE supporting_values->>'contentTypeId' = ?")
                .addParam(contentType.id())
                .loadObjectResults();

        assertEquals(2, maps.size());

        final List<String> titlesUniqueFields = maps.stream()
                .map(entry -> getSupportingValues(entry))
                .flatMap(supportingValues -> ((List<String>) supportingValues.get("contentletIds")).stream())
                .map(id -> {
                    try {
                        return APILocator.getContentletAPI().findContentletByIdentifier(id, false, 1, APILocator.systemUser(), false);
                    } catch (DotDataException | DotSecurityException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(Contentlet::getTitle)
                .collect(Collectors.toList());

        assertTrue(titlesUniqueFields.contains("A"));
        assertTrue(titlesUniqueFields.contains("B"));
    }

    private static Map<String, Object> getSupportingValues(Map<String, Object> entry)  {
        try {
            return JsonUtil.getJsonFromString(entry.get("supporting_values").toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Method to test: {@link ImportUtil#importFile(Long, String, String, String[], boolean, boolean, User, long, String[], CsvReader, int, int, Reader, String, HttpServletRequest)}
     * When:
     * - Create a ContentType with a unique field
     * - Create a Contentlet with a value equals to 'A' in the unique field value
     * - Run the import in preview with a file with 2 Contentlets with "A" and "B" as unique field values
     * - Should return a duplicate error and one Contentlet that is valid
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void testingImportPreviewWithUniqueFields() throws DotSecurityException, DotDataException, IOException {
        final UniqueFieldDataBaseUtil uniqueFieldDataBaseUtil = new UniqueFieldDataBaseUtil();
        uniqueFieldDataBaseUtil.createUniqueFieldsValidationTable();

        com.dotcms.contenttype.model.field.Field titleField = new FieldDataGen()
                .name("title").velocityVarName("title").type(TextField.class).next();

        com.dotcms.contenttype.model.field.Field uniqueField = new FieldDataGen()
                .name("unique").type(TextField.class).unique(true).next();

        ContentType contentType = new ContentTypeDataGen().field(titleField).field(uniqueField).nextPersisted();

        titleField = fieldAPI.byContentTypeAndVar(contentType, titleField.variable());
        uniqueField = fieldAPI.byContentTypeAndVar(contentType, uniqueField.variable());

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .setProperty(titleField.variable(), "C")
                .setProperty(uniqueField.variable(), "A")
                .nextPersisted();

        String csvWContent = "A, A" + "\r\n" + //This has the dupe value that we expect will break the import
                "B, B" + "\r\n";               //This is valid

        final Reader reader = createTempFile(csvWContent);

        final CsvReader csvreader = new CsvReader(reader);
        csvreader.setSafetySwitch(false);
        final String[] csvHeaders = new String[]{titleField.variable(), uniqueField.variable()};

        final ImmutableImportFileParams importFileParams = ImmutableImportFileParams.builder()
                .importId(0L)
                .siteId(defaultSite.getInode())
                .contentTypeInode(contentType.inode())
                .keyFields(titleField.id(), uniqueField.id())
                .user(user)
                .language(defaultLanguage.getId())
                .csvHeaders(csvHeaders)
                .csvReader(csvreader)
                .languageCodeHeaderColumn(-1)
                .countryCodeHeaderColumn(-1)
                .stopOnError(true)
                .workflowActionId(schemeStepActionResult1.getAction().getId())
                .request(getHttpRequest())
                .build();
        final ImportResult result = ImportUtil.importFileResult(importFileParams);
        final  HashMap<String, List<String>> imported = ImportResultConverter.toLegacyFormat(result, user);

        //Check import result
        final List<String> results = imported.get("results");
        assertEquals(2, results.size());

        final String resultErrorMessage = String.format("0 \"%s\" content updated corresponding to 0 repeated content based on the key provided",
                contentType.name());

        final String expectedMessage = String.format("0 New \"%s\" were created.", contentType.name());
        assertTrue(String.format("Expected message: %s /real message: %s", expectedMessage, results),
                results.contains(expectedMessage));
        assertTrue(String.format("Expected: %s / reals: %s", resultErrorMessage, results),
                results.contains(resultErrorMessage));

        final List<String> errors = imported.get("errors");
        assertEquals(2, errors.size());

        final String errorMessage = String.format("Line #2: Contentlet with ID 'Unknown/New' ['A'] has invalid/missing field(s). " +
                        "The unique value 'a' for the field '%s' in the Content Type '%s' already exists " +
                        "- Fields: [UNIQUE]: %s (%s)",
                uniqueField.variable(), contentType.variable(), uniqueField.name(), uniqueField.variable());

        assertTrue("Expected error message is not present", errors.contains(errorMessage));

        final List<Contentlet> contentlets = APILocator.getContentletAPI().findByStructure(contentType.inode(),
                APILocator.systemUser(), false, -1, 0);

        assertEquals(1, contentlets.size());
        assertEquals("C", contentlets.get(0).getTitle());

        //Checking unique_fields table
        List<Map<String, Object>> maps = new DotConnect().setSQL("SELECT * FROM unique_fields " +
                        "WHERE supporting_values->>'contentTypeId' = ?")
                .addParam(contentType.id())
                .loadObjectResults();

        assertEquals(1, maps.size());

        final Map<String, Object> supportingValues =
                getSupportingValues(maps.get(0));
        final List<String> contentletIds = ((List<String>) supportingValues.get("contentletIds")).stream()
                .filter(id -> !contentlet.getIdentifier().equals(id))
                .collect(Collectors.toList());

        assertTrue(contentletIds.isEmpty());
    }

    /**
     * Method to test: {@link ImportUtil#importFile(Long, String, String, String[], boolean, boolean, User, long, String[], CsvReader, int, int, Reader, String, HttpServletRequest)}
     * When: We have one contentlet with identifier "A" and we try to import 10 contentlets with the same identifier but different title
     *  We should end up with 10 contentlets with the same identifier and different titles and inode as we are importing versions
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void importVersionsSharingSameIdentifier()
            throws DotSecurityException, DotDataException, IOException {

        String contentTypeName = "ImportFileManyItemsSharingIdentifier_" + System.currentTimeMillis();
        String contentTypeVarName = contentTypeName.replaceAll("_", "Var_");
        com.dotcms.contenttype.model.field.Field titleField = new FieldDataGen()
                .name("title")
                .velocityVarName("title")
                .type(TextField.class)
                .next();

        ContentType contentType = new ContentTypeDataGen()
                .name(contentTypeName)
                .velocityVarName(contentTypeVarName)
                .host(APILocator.systemHost())
                .fields(List.of(titleField))
                .nextPersisted();

        final Contentlet contentlet = new ContentletDataGen(contentType)
                .host(defaultSite)
                .setProperty(titleField.variable(), "Here we go 0")
                .nextPersisted();

        final ContentType saved = contentTypeApi.find(contentType.inode());
        titleField = saved.fields().get(0);

        final Reader reader = createTempFile("identifier,title \r\n" +
                contentlet.getIdentifier()+ ", Here we go 10" + "\r\n" +
                contentlet.getIdentifier()+ ", Here we go 9" + "\r\n" +
                contentlet.getIdentifier()+ ", Here we go 8" + "\r\n" +
                contentlet.getIdentifier()+ ", Here we go 7" + "\r\n" +
                contentlet.getIdentifier()+ ", Here we go 6" + "\r\n" +
                contentlet.getIdentifier()+ ", Here we go 5" + "\r\n" +
                contentlet.getIdentifier()+ ", Here we go 4" + "\r\n" +
                contentlet.getIdentifier()+ ", Here we go 3" + "\r\n" +
                contentlet.getIdentifier()+ ", Here we go 2" + "\r\n" +
                contentlet.getIdentifier()+ ", Here we go 1" + "\r\n"
        );

        final ImportResult result = importAndValidate(contentType, titleField, reader, true, 1, WORKFLOW_PUBLISH_ACTION_ID);
        // Validate results
        final List<Contentlet> savedData = contentletAPI.findByStructure(contentType.inode(), user, false, 0, 0);
        assertNotNull(savedData);

        // Should only have rows before the error (1)
        assertEquals(1, savedData.size());
        final Identifier identifier = new Identifier(contentlet.getIdentifier());
        final List<Contentlet> allVersions = contentletAPI.findAllVersions(identifier, user, false);
        assertEquals(11, allVersions.size());

        final ResultData data = result.data().orElse(null);
        assertNotNull(data);
    }


    /**
     * Method to test: {@link ImportUtil#importFile(Long, String, String, String[], boolean, boolean, User, long, String[], CsvReader, int, int, Reader, String, HttpServletRequest)}
     * Given scenario: We try to import a file with different errors e.g. missing required fields, invalid date formats, etc.
     * Expected behavior: The import should fail with appropriate error messages for each error
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void TestMissingRequiredFieldErrorMessage()
            throws DotSecurityException, DotDataException, IOException {

        String contentTypeName = "TestMissingRequiredFieldErrorMessage_" + System.currentTimeMillis();
        String contentTypeVarName = contentTypeName.replaceAll("_", "Var_");
        com.dotcms.contenttype.model.field.Field titleField = new FieldDataGen()
                .name("title")
                .velocityVarName("title")
                .type(TextField.class)
                .next();
        com.dotcms.contenttype.model.field.Field reqField = new FieldDataGen()
                .name("req")
                .velocityVarName("req")
                .required(true)
                .type(TextField.class)
                .next();

        com.dotcms.contenttype.model.field.Field dateTimeField = new FieldDataGen()
                .name("dateTime")
                .velocityVarName("dateTime")
                .defaultValue(null)
                .type(DateTimeField.class)
                .next();

        com.dotcms.contenttype.model.field.Field numericField = new FieldDataGen()
                .name("numeric")
                .velocityVarName("numeric")
                .defaultValue(null)
                .type(TextField.class)
                .dataType(DataTypes.INTEGER)
                .next();

        ContentType contentType = new ContentTypeDataGen()
                .name(contentTypeName)
                .velocityVarName(contentTypeVarName)
                .host(APILocator.systemHost())
                .fields(List.of(titleField, reqField, dateTimeField, numericField))
                .nextPersisted();

        final ContentType saved = contentTypeApi.find(contentType.inode());
        titleField = saved.fields().get(0);

        final Reader reader = createTempFile("title,req,dateTime,numeric \r\n" +
                "Title 1, ,14-05-2027,12" + "\r\n" +     // Missing required field here
                "Title 2,lol,invalid-date,12" + "\r\n" + // Invalid date
                "Title 3,lol,14-05-2027,Text" + "\r\n"   // Invalid number
        );
        final ImportResult result = importAndValidate(contentType, titleField, reader, false, 1, WORKFLOW_PUBLISH_ACTION_ID);

        assertNotNull(result);
        assertFalse(result.error().isEmpty());
        final Optional<String> code1 = result.error().get(0).code();
        assertTrue(code1.isPresent());
        assertEquals(REQUIRED_FIELD_MISSING.name(),code1.get());

        final Optional<String> code2 = result.error().get(1).code();
        assertTrue(code2.isPresent());
        assertEquals(INVALID_DATE_FORMAT.name(),code2.get());

    }


    /**
     * Method to test: {@link ImportUtil#importFile(Long, String, String, String[], boolean, boolean, User, long, String[], CsvReader, int, int, Reader, String, HttpServletRequest)}
     * Given scenario: We try to import a file with a valid binary image URL including query parameters
     * Expected behavior: The import should NOT fail
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void TestImportBinaryExpectErrors()
            throws DotSecurityException, DotDataException, IOException {

        String contentTypeName = "TestImportBinaryErrorMessage_" + System.currentTimeMillis();
        String contentTypeVarName = contentTypeName.replaceAll("_", "Var_");
        com.dotcms.contenttype.model.field.Field titleField = new FieldDataGen()
                .name("title")
                .velocityVarName("title")
                .type(TextField.class)
                .next();
        com.dotcms.contenttype.model.field.Field reqField = new FieldDataGen()
                .name("bin")
                .velocityVarName("bin")
                .required(false)
                .type(BinaryField.class)
                .next();

        ContentType contentType = new ContentTypeDataGen()
                .name(contentTypeName)
                .velocityVarName(contentTypeVarName)
                .host(APILocator.systemHost())
                .fields(List.of(titleField, reqField))
                .nextPersisted();

        final ContentType saved = contentTypeApi.find(contentType.inode());
        titleField = saved.fields().stream().filter(f -> "title".equals(f.variable())).findFirst().orElseThrow();

        final Reader reader = createTempFile("title,bin \r\n" +
                "Company Logo, https://www.dotcms.com/assets/logo.svg?w=3840 " + "\r\n" +
                "Non-Existing-file path, /fake/path" + "\r\n" +
                "Non-Existing-url, https://demo.dotcms.com/lol.jpg" + "\r\n" +
                "Non-Valid-url, https://demo.dotcms.com/ lol.jpg" + "\r\n"
        );
        final ImportResult result = importAndValidate(contentType, titleField, reader, false, 1, WORKFLOW_PUBLISH_ACTION_ID);

        assertNotNull(result);
        assertFalse(result.error().isEmpty());
        assertTrue(result.error().get(0).code().isPresent());
        assertEquals(INVALID_BINARY_URL.name(), result.error().get(0).code().get());
        assertTrue(result.error().get(1).code().isPresent());
        assertEquals(UNREACHABLE_URL_CONTENT.name(), result.error().get(1).code().get());
        assertTrue(result.error().get(2).code().isPresent());
        assertEquals(INVALID_BINARY_URL.name(), result.error().get(2).code().get());

        final List<Contentlet> byStructure = contentletAPI.findByStructure(contentType.inode(),
                user, false, 0, 0);
        assertEquals(1,byStructure.size());
        assertNotNull(byStructure.get(0).get("bin"));
    }


    /**
     * Method to test: {@link ImportUtil#importFile(Long, String, String, String[], boolean, boolean, User, long, String[], CsvReader, int, int, Reader, String, HttpServletRequest)}
     * Given scenario: We try to import a file with a valid image URL including query parameters and some invalid URLs
     * Expected behavior: The import should fail for the invalid URLs but succeed for the valid one
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void TestImportImageExpectErrors()
            throws DotSecurityException, DotDataException, IOException {

        String contentTypeName = "TestImportImageErrorMessage_" + System.currentTimeMillis();
        String contentTypeVarName = contentTypeName.replaceAll("_", "Var_");
        com.dotcms.contenttype.model.field.Field titleField = new FieldDataGen()
                .name("title")
                .velocityVarName("title")
                .type(TextField.class)
                .next();
        com.dotcms.contenttype.model.field.Field reqField = new FieldDataGen()
                .name("image")
                .velocityVarName("image")
                .required(false)
                .type(ImageField.class)
                .next();

        ContentType contentType = new ContentTypeDataGen()
                .name(contentTypeName)
                .velocityVarName(contentTypeVarName)
                .host(APILocator.systemHost())
                .fields(List.of(titleField, reqField))
                .nextPersisted();

        final ContentType saved = contentTypeApi.find(contentType.inode());
        titleField = saved.fields().stream().filter(f -> "title".equals(f.variable())).findFirst().orElseThrow();

        final Reader reader = createTempFile("title,image \r\n" +
                "Company Logo, https://www.dotcms.com/assets/logo.svg?w=3840 " + "\r\n" +
                "Non-Existing file path, /fake/path" + "\r\n" +
                "Non-Existing-url, https://demo.dotcms.com/lol.jpg" + "\r\n" +
                "Non-Valid-url, https://www.dotcms.com/ assets/logo.svg?w=3840" + "\r\n"
        );
        final ImportResult result = importAndValidate(contentType, titleField, reader, false, 1, WORKFLOW_PUBLISH_ACTION_ID);

        assertNotNull(result);
        assertFalse(result.error().isEmpty());
        assertTrue(result.error().get(0).code().isPresent());
        assertEquals(INVALID_FILE_PATH.name(), result.error().get(0).code().get());
        assertTrue(result.error().get(1).code().isPresent());
        assertEquals(UNREACHABLE_URL_CONTENT.name(), result.error().get(1).code().get());
        assertTrue(result.error().get(2).code().isPresent());
        assertEquals(INVALID_BINARY_URL.name(), result.error().get(2).code().get());

        //Make sure we got one row with the image as the other two should have failed
        final List<Contentlet> byStructure = contentletAPI.findByStructure(contentType.inode(),
                user, false, 0, 0);
        assertEquals(1,byStructure.size());
        assertNotNull(byStructure.get(0).get("image"));

    }


    /**
     * Method to test: {@link ImportUtil#importFile(Long, String, String, String[], boolean, boolean, User, long, String[], CsvReader, int, int, Reader, String, HttpServletRequest)}
     * Given scenario: We try to import a category that exists but is not a child of the configured root
     * Expected behavior: The import should fail with INVALID_CATEGORY_KEY and no content should be saved
     */
    @Test
    public void importLine_shouldFailIfCategoryIsNotUnderConfiguredRoot() throws DotDataException, DotSecurityException, IOException {
        final Category configuredRoot = TestDataUtils.createCategories(); // Has valid children
        final Category unrelatedCategory = new CategoryDataGen()
                .setCategoryName("Unrelated-Category-" + System.currentTimeMillis())
                .setKey("unrelated-key-" + System.currentTimeMillis())
                .setCategoryVelocityVarName("unrelatedVar")
                .setSortOrder(1)
                .nextPersisted(); // Not a child of configuredRoot

        final ContentType contentType = TestDataUtils.newContentTypeFieldTypesGalore(configuredRoot);
        final com.dotcms.contenttype.model.field.Field titleField = fieldAPI.byContentTypeAndVar(contentType, "textField");
        final com.dotcms.contenttype.model.field.Field categoryField = contentType.fields(CategoryField.class).stream()
                .findFirst()
                .orElseThrow();

        final String csvContent = String.format("textField,%s\r\nSome Title,%s", categoryField.variable(), unrelatedCategory.getKey());
        final Reader reader = createTempFile(csvContent);

        final ImportResult result = importAndValidate(contentType, titleField, reader, false, 1, WORKFLOW_PUBLISH_ACTION_ID);

        assertNotNull(result);
        assertFalse(result.error().isEmpty());


        final ValidationMessage error = result.error().get(0);
        Optional<String> errorCode = error.code();
        String errorMessage = error.message();

        assertTrue(errorCode.isPresent());
        assertEquals(INVALID_CATEGORY_KEY.name(), errorCode.get());
        assertTrue(errorMessage.contains("Invalid category key found: '" + unrelatedCategory.getKey()));
        assertTrue(errorMessage.contains("be a child of '" + configuredRoot.getCategoryName() + "'"));

        final Optional<ResultData> resultData = result.data();
        assertTrue(resultData.isPresent());
        assertEquals(2, resultData.get().processed().parsedRows());
        assertEquals(1, resultData.get().processed().failedRows());
        assertEquals(0, resultData.get().summary().createdContent());

        List<Contentlet> saved = contentletAPI.findByStructure(contentType.inode(), user, false, 0, 0);
        assertEquals(0, saved.size());
    }

    /**
     * Method to test: {@link ImportUtil#importFile(Long, String, String, String[], boolean, boolean, User, long, String[], CsvReader, int, int, Reader, String, HttpServletRequest)}
     * Given scenario: We try to import a category key that doesn't exist
     * Expected behavior: The import should fail with INVALID_CATEGORY_KEY and no content should be saved
     */
    @Test
    public void importLine_shouldFailIfCategoryKeyDoesNotExist() throws DotDataException, DotSecurityException, IOException {
        final Category configuredRoot = TestDataUtils.createCategories(); // Has valid children

        final ContentType contentType = TestDataUtils.newContentTypeFieldTypesGalore(configuredRoot);
        final com.dotcms.contenttype.model.field.Field titleField = fieldAPI.byContentTypeAndVar(contentType, "textField");
        final com.dotcms.contenttype.model.field.Field categoryField = contentType.fields(CategoryField.class).stream()
                .findFirst()
                .orElseThrow();

        final String invalidKey = "non-existent-category-key";
        final String csvContent = String.format("textField,%s\r\nSome Title,%s", categoryField.variable(), invalidKey);
        final Reader reader = createTempFile(csvContent);

        final ImportResult result = importAndValidate(contentType, titleField, reader, false, 1, WORKFLOW_PUBLISH_ACTION_ID);

        assertNotNull(result);
        assertFalse(result.error().isEmpty());

        final ValidationMessage error = result.error().get(0);
        Optional<String> errorCode = error.code();
        String errorMessage = error.message();

        assertTrue(errorCode.isPresent());
        assertEquals(INVALID_CATEGORY_KEY.name(), errorCode.get());
        assertTrue(errorMessage.contains("Invalid category key found: '" + invalidKey));
        assertTrue(errorMessage.contains("be a child of '" + configuredRoot.getCategoryName() + "'"));

        final Optional<ResultData> resultData = result.data();
        assertTrue(resultData.isPresent());
        assertEquals(2, resultData.get().processed().parsedRows());
        assertEquals(1, resultData.get().processed().failedRows());
        assertEquals(0, resultData.get().summary().createdContent());

        List<Contentlet> saved = contentletAPI.findByStructure(contentType.inode(), user, false, 0, 0);
        assertEquals(0, saved.size());
    }


    /**
     * Method to test: {@link ImportUtil#importFile(Long, String, String, String[], boolean, boolean, User, long, String[], CsvReader, int, int, Reader, String, HttpServletRequest)}
     * Given scenario: We import a valid category key that is a child of the configured root
     * Expected behavior: The import should succeed and the category should be assigned
     */
    @Test
    public void importLine_shouldSucceedIfCategoryIsValidChild() throws DotDataException, DotSecurityException, IOException {
        final Category configuredRoot = TestDataUtils.createCategories(); // Has children
        final List<Category> children = APILocator.getCategoryAPI().getAllChildren(configuredRoot, user, false);
        assertFalse(children.isEmpty());
        final Category validChild = children.get(0);

        final ContentType contentType = TestDataUtils.newContentTypeFieldTypesGalore(configuredRoot);
        final com.dotcms.contenttype.model.field.Field titleField = fieldAPI.byContentTypeAndVar(contentType, "textField");
        final com.dotcms.contenttype.model.field.Field categoryField = contentType.fields(CategoryField.class).stream()
                .findFirst()
                .orElseThrow();

        final String csvContent = String.format("textField,%s\r\nTest Title,%s", categoryField.variable(), validChild.getKey());
        final Reader reader = createTempFile(csvContent);

        final ImportResult result = importAndValidate(contentType, titleField, reader, false, 1, WORKFLOW_PUBLISH_ACTION_ID);

        assertNotNull(result);
        assertTrue(result.error().isEmpty());

        final Optional<ResultData> resultData = result.data();
        assertTrue(resultData.isPresent());
        assertEquals(2, resultData.get().processed().parsedRows());
        assertEquals(0, resultData.get().processed().failedRows());
        assertEquals(1, resultData.get().summary().createdContent());

        List<Contentlet> saved = contentletAPI.findByStructure(contentType.inode(), user, false, 0, 0);
        assertEquals(1, saved.size());

        List<Category> assignedCategories = APILocator.getCategoryAPI().getParents(saved.get(0), user, false);
        assertTrue(assignedCategories.stream().anyMatch(cat -> cat.getInode().equals(validChild.getInode())));
    }

    /**
     * Method to test: {@link ImportUtil#importFile(Long, String, String, String[], boolean, boolean, User, long, String[], CsvReader, int, int, Reader, String, HttpServletRequest)}
     * Given scenario: We try to import a file with a valid text field and a JSON field where one entry contains invalid JSON
     * Expected behavior: The import should fail for the invalid JSON but succeed for valid entries, returning specific error details
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void importFile_withInvalidJSONField_shouldReturnSpecificErrorDetails()
            throws DotSecurityException, DotDataException, IOException {

        ContentType contentType = null;
        long time = System.currentTimeMillis();

        try {
            final String contentTypeName = "JSONTestContentType_" + time;
            final String contentTypeVarName = "jsonTestContentType_" + time;

            // Create text field
            com.dotcms.contenttype.model.field.Field titleField = new FieldDataGen()
                    .name("title")
                    .velocityVarName("title")
                    .type(TextField.class)
                    .required(true)
                    .next();

            // Create JSON field
            com.dotcms.contenttype.model.field.Field jsonField = new FieldDataGen()
                    .name("keyValue")
                    .velocityVarName("keyValue")
                    .type(KeyValueField.class)
                    .required(false)
                    .next();

            // Create content type with both fields
            contentType = new ContentTypeDataGen()
                    .name(contentTypeName)
                    .velocityVarName(contentTypeVarName)
                    .host(APILocator.systemHost())
                    .fields(List.of(titleField, jsonField))
                    .nextPersisted();

            // Refresh content type to get saved field references
            final ContentType savedContentType = contentTypeApi.find(contentType.inode());
            titleField = savedContentType.fields().stream()
                    .filter(f -> "title".equals(f.variable()))
                    .findFirst()
                    .orElseThrow();
            jsonField = savedContentType.fields().stream()
                    .filter(f -> "keyValue".equals(f.variable()))
                    .findFirst()
                    .orElseThrow();

            // Create CSV with valid and invalid JSON entries
            final String csvContent = "title,keyValue\r\n" +
                    "Valid Entry 1,\"{\"\"timeline\"\":\"\"18_months\"\",\"\"team_size\"\":\"\"12\"\"}\"\r\n" +
                    "Invalid Entry,\"{'timeline':'18_months','team_size':'12'}\"\r\n" +
                    "Valid Entry 2,\"{\"\"status\"\":\"\"active\"\",\"\"priority\"\":\"\"high\"\"}\"\r\n";

            final Reader reader = createTempFile(csvContent);

            // Perform import with stopOnError=false to process all rows
            final ImportResult result = importAndValidate(
                    savedContentType,
                    titleField,
                    reader,
                    false, // stopOnError = false to continue processing
                    1,     // commitGranularity = 1
                    WORKFLOW_PUBLISH_ACTION_ID
            );

            // Validate that we have errors
            assertNotNull(result);
            assertFalse("Expected errors for invalid JSON", result.error().isEmpty());

            // Find the JSON validation error
            ValidationMessage jsonError = result.error().stream()
                    .filter(error -> "INVALID_JSON".equals(error.code().orElse("")))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Expected INVALID_JSON error not found"));

            // Validate error type
            assertEquals("Error type should be ERROR", "ERROR", jsonError.type().toString());

            // Validate error code
            assertTrue("Error code should be present", jsonError.code().isPresent());
            assertEquals("Error code should be INVALID_JSON", "INVALID_JSON", jsonError.code().get());

            // Validate error message
            String errorMessage = jsonError.message();
            assertEquals("Error message should match expected format",
                    "Invalid JSON field provided. Key Value Field variable: keyValue", errorMessage);

            // Validate field
            assertTrue("Error field should be present", jsonError.field().isPresent());
            assertEquals("Error field should be keyValue", "keyValue", jsonError.field().get());

            // Validate line number (row number in CSV)
            assertTrue("Line number should be present", jsonError.lineNumber().isPresent());
            assertEquals("Line number should be 3 (header + 2 data rows)", Integer.valueOf(3), jsonError.lineNumber().get());


            Map<String, Object> context = jsonError.context();

            // Validate context size and keys
            assertEquals("Context should have 3 entries", 3, context.size());
            assertTrue("Context should contain 'line' key", context.containsKey("line"));
            assertTrue("Context should contain 'column' key", context.containsKey("column"));
            assertTrue("Context should contain 'errorHint' key", context.containsKey("errorHint"));

            // Validate context values
            assertEquals("Context line should be 1", 1, context.get("line"));
            assertEquals("Context column should be 2", 2, context.get("column"));

            // Validate parse error details
            String errorHint = (String) context.get("errorHint");
            assertNotNull("Parse error should not be null", errorHint);
            assertTrue("Parse error should mention unexpected character",
                    errorHint.contains("Unexpected character"));
            assertTrue("Parse error should mention single quote character code",
                    errorHint.contains("''' (code 39)"));
            assertTrue("Parse error should mention expecting double-quote",
                    errorHint.contains("was expecting double-quote to start field name"));


            Logger.info(this, "JSON Validation Error Details:");
            Logger.info(this, "  Type: " + jsonError.type());
            Logger.info(this, "  Code: " + jsonError.code().orElse("N/A"));
            Logger.info(this, "  Message: " + jsonError.message());
            Logger.info(this, "  Field: " + jsonError.field().orElse("N/A"));
            Logger.info(this, "  Context: " + context);

            // Validate import results
            final Optional<ResultData> resultData = result.data();
            assertTrue("Result data should be present", resultData.isPresent());

            // Should have processed 3 rows + header = 4 total
            assertEquals("Should have parsed 4 rows including header",
                    4, resultData.get().processed().parsedRows());
            assertEquals("Should have 1 failed row",
                    1, resultData.get().processed().failedRows());
            assertEquals("Should have created 2 content items",
                    2, resultData.get().summary().createdContent());
            assertEquals("Should have 1 rollback for the failed row",
                    1, resultData.get().summary().rollbacks());

            // Verify that valid entries were saved
            List<Contentlet> savedData = contentletAPI.findByStructure(
                    contentType.inode(), user, false, 0, 0);
            assertNotNull("Saved data should not be null", savedData);
            assertEquals("Should have 2 saved contentlets", 2, savedData.size());

            // Verify the valid JSON entries were saved correctly
            boolean foundValidEntry1 = false;
            boolean foundValidEntry2 = false;

            for (Contentlet contentlet : savedData) {
                String title = contentlet.getStringProperty("title");
                Object jsonValue = contentlet.get("keyValue");

                if ("Valid Entry 1".equals(title)) {
                    foundValidEntry1 = true;
                    assertNotNull("JSON field should not be null for valid entry 1", jsonValue);
                    // Verify JSON was parsed correctly
                    String jsonString = jsonValue.toString();
                    assertTrue("JSON should contain timeline", jsonString.contains("timeline"));
                    assertTrue("JSON should contain 18_months", jsonString.contains("18_months"));
                } else if ("Valid Entry 2".equals(title)) {
                    foundValidEntry2 = true;
                    assertNotNull("JSON field should not be null for valid entry 2", jsonValue);
                    // Verify JSON was parsed correctly
                    String jsonString = jsonValue.toString();
                    assertTrue("JSON should contain status", jsonString.contains("status"));
                    assertTrue("JSON should contain active", jsonString.contains("active"));
                }
            }

            assertTrue("Should have found Valid Entry 1", foundValidEntry1);
            assertTrue("Should have found Valid Entry 2", foundValidEntry2);

            // Verify the invalid entry was not saved
            boolean foundInvalidEntry = savedData.stream()
                    .anyMatch(c -> "Invalid Entry".equals(c.getStringProperty("title")));
            assertFalse("Invalid entry should not have been saved", foundInvalidEntry);

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
     * Functional interface for field-specific validation strategies
     */
    @FunctionalInterface
    public interface AssertionsStrategy {
        /**
         * Validates the specific aspects of a field test result
         *
         * @param result The import result to validate
         * @param testCase The test case being executed
         * @param contentType The content type used in the test
         * @throws DotDataException if there's a data access error
         * @throws DotSecurityException if there's a security error
         */
        void asserts(ImportResult result, FieldTestCase testCase, ContentType contentType) throws DotDataException, DotSecurityException;
    }

    /**
     * Test case definition for each field type
     */
    public static class FieldTestCase {
        final String fieldTypeName;
        final Class<? extends com.dotcms.contenttype.model.field.Field> fieldClass;
        final String fieldVariable;
        final DataTypes dataType;
        final String validValue;
        final String invalidValue;
        final String expectedErrorCode;
        final boolean requiresCategory;
        final boolean requiresHost;
        final String fieldSpecificConfig;
        final AssertionsStrategy assertionsStrategy;

        public FieldTestCase(String fieldTypeName,
                Class<? extends com.dotcms.contenttype.model.field.Field> fieldClass,
                String fieldVariable,
                DataTypes dataType,
                String validValue,
                String invalidValue,
                String expectedErrorCode,
                boolean requiresCategory,
                boolean requiresHost,
                String fieldSpecificConfig,
                AssertionsStrategy assertionsStrategy) {
            this.fieldTypeName = fieldTypeName;
            this.fieldClass = fieldClass;
            this.fieldVariable = fieldVariable;
            this.dataType = dataType;
            this.validValue = validValue;
            this.invalidValue = invalidValue;
            this.expectedErrorCode = expectedErrorCode;
            this.requiresCategory = requiresCategory;
            this.requiresHost = requiresHost;
            this.fieldSpecificConfig = fieldSpecificConfig;
            this.assertionsStrategy = assertionsStrategy;
        }

        /**
         * Needed when creating a relationship
         * @param newValidValue
         * @return
         */
        public FieldTestCase withValidValue(String newValidValue) {
            return new FieldTestCase(
                    this.fieldTypeName,
                    this.fieldClass,
                    this.fieldVariable,
                    this.dataType,
                    newValidValue,
                    this.invalidValue,
                    this.expectedErrorCode,
                    this.requiresCategory,
                    this.requiresHost,
                    this.fieldSpecificConfig,
                    this.assertionsStrategy
            );
        }


        @Override
        public String toString() {
            return fieldTypeName;
        }
    }

    @DataProvider
    public static Object[][] fieldTestCases() {
        return new Object[][]{

                // Required TextField - Missing value
                {new FieldTestCase(
                        "RequiredTextField",
                        TextField.class,
                        "requiredTextField",
                        DataTypes.TEXT,
                        "Valid text value",
                        "", // Empty required field
                        REQUIRED_FIELD_MISSING.name(),
                        false,
                        false,
                        "required",
                        REQUIRED_TEXT_FIELD_ASSERTION
                )},
                // DateTimeField - Invalid date format
                {new FieldTestCase(
                        "DateTimeField",
                        DateTimeField.class,
                        "dateField",
                        DataTypes.DATE,
                        "2023-12-25",
                        "invalid-date-format",
                        INVALID_DATE_FORMAT.name(),
                        false,
                        false,
                        null,
                        INVALID_DATE_ASSERTION
                )},
                {new FieldTestCase(
                        "DateField",
                        DateField.class,
                        "dateField",
                        DataTypes.DATE,
                        "2023-12-25",
                        "invalid-date-format",
                        INVALID_DATE_FORMAT.name(),
                        false,
                        false,
                        null,
                        INVALID_DATE_ASSERTION
                )},
                {new FieldTestCase(
                        "TimeField",
                        TimeField.class,
                        "timeField",
                        DataTypes.DATE,
                        "14:30:00",
                        "invalid-time",
                        INVALID_DATE_FORMAT.name(),
                        false,
                        false,
                        null,
                        INVALID_DATE_ASSERTION
                )},
                // Integer TextField - Invalid number
                {new FieldTestCase(
                        "IntegerField",
                        TextField.class,
                        "integerField",
                        DataTypes.INTEGER,
                        "123",
                        "not-a-number",
                        INVALID_NUMBER_FORMAT.name(),
                        false,
                        false,
                        null,
                        INVALID_INT_NUMBER_ASSERTION
                )},
                // Float TextField - Invalid decimal
                {new FieldTestCase(
                        "FloatField",
                        TextField.class,
                        "floatField",
                        DataTypes.FLOAT,
                        "123.45",
                        "not-a-decimal",
                        INVALID_NUMBER_FORMAT.name(),
                        false,
                        false,
                        null,
                        INVALID_INT_NUMBER_ASSERTION
                )},
                // BinaryField - Unreachable URL
                {new FieldTestCase(
                        "BinaryField",
                        BinaryField.class,
                        "binaryField",
                        DataTypes.SYSTEM,
                        "", // Empty is valid for optional binary
                        "https://www.dotcms.com/invalid-url-does-not-exist-" + System.currentTimeMillis() + ".com/file.pdf",
                        UNREACHABLE_URL_CONTENT.name(),
                        false,
                        false,
                        null,
                        INVALID_BINARY_ASSERTION
                )},
                // ImageField - Invalid file path
                {new FieldTestCase(
                        "ImageField",
                        ImageField.class,
                        "imageField",
                        DataTypes.TEXT,
                        "", // Empty is valid for optional image
                        "/invalid/path/to/image.jpg",
                        INVALID_FILE_PATH.name(),
                        false,
                        false,
                        null,
                        INVALID_IMAGE_PATH_ASSERTION
                )},
                // ImageField - Invalid file path
                {new FieldTestCase(
                        "ImageField",
                        ImageField.class,
                        "imageField",
                        DataTypes.TEXT,
                        "", // Empty is valid for optional image
                        "https://www.dotcms.com/invalid-url-does-not-exist-" + System.currentTimeMillis() + ".com/file.pdf",
                        UNREACHABLE_URL_CONTENT.name(),
                        false,
                        false,
                        null,
                        INVALID_IMAGE_URL_ASSERTION
                )},
                // HostFolderField - Invalid location
                {new FieldTestCase(
                        "HostFolderField",
                        HostFolderField.class,
                        "hostField",
                        DataTypes.TEXT,
                        "", // Will be set to valid host in test
                        "invalid-host-identifier-" + System.currentTimeMillis(),
                        INVALID_SITE_FOLDER_REF.name(),
                        false,
                        true,
                        null,
                        INVALID_HOST_FOLDER_ASSERTION
                )},
                // CategoryField - Invalid category key
                {new FieldTestCase(
                        "CategoryField",
                        CategoryField.class,
                        "categoryField",
                        DataTypes.TEXT,
                        "", // Will be set to valid category in test
                        "invalid-category-key-" + System.currentTimeMillis(),
                        INVALID_CATEGORY_KEY.name(),
                        true,
                        false,
                        null,
                        INVALID_CATEGORY_ASSERTION
                )},
                // KeyValueField (JSON) - Invalid JSON
                {new FieldTestCase(
                        "KeyValueField",
                        KeyValueField.class,
                        "keyValueField",
                        DataTypes.LONG_TEXT,
                        "{\"\"valid\"\":\"\"json\"\"}",
                        "{'invalid':'json'}", // Single quotes make it invalid
                        INVALID_JSON.name(),
                        false,
                        false,
                        null,
                        JSON_FIELD_ASSERTION
                )},
                {new FieldTestCase(
                        "JsonField",
                        JSONField.class,
                        "jsonField",
                        DataTypes.LONG_TEXT,
                        "{}",
                        "{'invalid':'json'}", // Single quotes make it invalid
                        INVALID_JSON.name(),
                        false,
                        false,
                        null,
                        JSON_FIELD_ASSERTION
                )},
                {new FieldTestCase(
                        "RequiredTextAreaField",
                        TextAreaField.class,
                        "textAreaField",
                        DataTypes.LONG_TEXT,
                        "Valid long text content for text area",
                        "", // Empty required field
                        REQUIRED_FIELD_MISSING.name(),
                        false,
                        false,
                        "required",
                        REQUIRED_TEXT_AREA_ASSERTION
                )},
                // WysiwygField - Required field missing
                {new FieldTestCase(
                        "RequiredWysiwygField",
                        WysiwygField.class,
                        "wysiwygField",
                        DataTypes.LONG_TEXT,
                        "<p>Valid HTML content</p>",
                        "", // Empty required field
                        REQUIRED_FIELD_MISSING.name(),
                        false,
                        false,
                        "required",
                        REQUIRED_WYSIWYG_ASSERTION
                )},
                // StoryBlockField - Required field missing
                {new FieldTestCase(
                        "RequiredStoryBlockField",
                        StoryBlockField.class,
                        "storyBlockField",
                        DataTypes.LONG_TEXT,
                        "{\"blocks\":[{\"type\":\"paragraph\",\"data\":{\"text\":\"Valid block\"}}]}",
                        "", // Empty required field
                        REQUIRED_FIELD_MISSING.name(),
                        false,
                        false,
                        "required",
                        REQUIRED_STORY_BLOCK_ASSERTION
                )},

                {new FieldTestCase(
                        "SelectField",
                        SelectField.class,
                        "SelectField",
                        DataTypes.FLOAT,
                        "10", // Will be set to valid option in test
                        "lol", // Invalid
                        INVALID_NUMBER_FORMAT.name(),
                        false,
                        false,
                        null, // Select options
                        INVALID_SELECT_NUMBER_ASSERTION
                )},
                {new FieldTestCase(
                        "UniqueTextField",
                        TextField.class,
                        "uniqueTextField",
                        DataTypes.TEXT,
                        "dupe-value",
                        "dupe-value", // Test should fail here
                        DUPLICATE_UNIQUE_VALUE.name(),
                        false,
                        false,
                        "unique",
                        UNIQUE_TEXT_FIELD_ASSERTION
                )},
                // RelationshipField - Invalid content reference
                {new FieldTestCase(
                        "RelationshipField",
                        RelationshipField.class,
                        "relationshipField",
                        DataTypes.SYSTEM,
                        "", // Will be set to valid content reference by the test once the related content is created
                        "invalid-content-id-12345",
                        RELATIONSHIP_VALIDATION_ERROR.name(),
                        false,
                        false,
                        null,
                        RELATIONSHIP_FIELD_VALIDATION
                )},
        };
    }

    @UseDataProvider("fieldTestCases")
    @Test
    public void importFile_stopOnError_individualFieldTest(FieldTestCase testCase)
            throws DotSecurityException, DotDataException, IOException {

        ContentType contentType = null;
        Category testCategory = null;
        ContentType relatedContentType = null;
        Contentlet relatedContentlet = null;

        Host testSite = null;

        try {
            final long time = System.currentTimeMillis();
            final String baseName = "TestFor_" + testCase.fieldTypeName;
            final String contentTypeName = baseName + "_CT_" + time;
            final String contentTypeVarName = baseName.toLowerCase() + "CT" + time;

            // Setup test dependencies
            if (testCase.requiresHost) {
                testSite = new SiteDataGen().nextPersisted();
            }

            Category parent = null;
            if (testCase.requiresCategory) {

                parent = new CategoryDataGen()
                        .setCategoryName("ParentTestCat_" + time)
                        .setKey("parent-test-cat-" + time)
                        .setCategoryVelocityVarName("parentTestCatVar")
                        .setSortOrder(1)
                        .nextPersisted();

                testCategory = new CategoryDataGen()
                        .setCategoryName("TestCat_" + time)
                        .setKey("test-cat-" + time)
                        .setCategoryVelocityVarName("testCatVar")
                        .parent(parent)
                        .setSortOrder(1)
                        .nextPersisted();
            }

            if(testCase.fieldClass == RelationshipField.class){
                // 1. Create the related content type first
                relatedContentType = createRelatedContentType();

                // 2. Create related contentlet
                relatedContentlet = createRelatedContentlet(relatedContentType);
                // Update test case valid value with the related contentlet identifier
                testCase = testCase.withValidValue(relatedContentlet.getIdentifier());
                Logger.info(ImportUtil.class,"Related contentlet is :: "+relatedContentlet.getIdentifier());
            }

            // Create fields for this test case
            List<com.dotcms.contenttype.model.field.Field> fields = createFieldsForTestCase(testCase, parent, relatedContentType);

            // Create content type (Parent for relationships)
            contentType = new ContentTypeDataGen()
                    .name(contentTypeName)
                    .velocityVarName(contentTypeVarName)
                    .host(APILocator.systemHost())
                    .fields(fields)
                    .nextPersisted();

            // Refresh to get field IDs
            final ContentType savedContentType = contentTypeApi.find(contentType.inode());
            final com.dotcms.contenttype.model.field.Field trackingField = savedContentType.fields().stream()
                    .filter(f -> "trackingInfo".equals(f.variable()))
                    .findFirst()
                    .orElseThrow();

            // Create CSV content for this specific field test
            final String csvContent = createCSVForFieldTest(testCase, testSite, testCategory);

            Logger.info(this, "Testing field type: " + testCase.fieldTypeName);
            Logger.info(this, "CSV Content:\n" + csvContent);

            // Execute test with stopOnError = TRUE
            final Reader reader = createTempFile(csvContent);
            final ImportResult result = importAndValidate(
                    savedContentType,
                    trackingField,
                    reader,
                    true,  // stopOnError = true
                    1,     // commitGranularity = 1
                    WORKFLOW_PUBLISH_ACTION_ID
            );

            // Validate results for this specific field type
            validateResults(result, testCase, savedContentType);

        } finally {
            cleanupTestData(contentType, testCategory, testSite, relatedContentlet, relatedContentType);
        }
    }

    private String getCardinalityValue(FieldTestCase testCase) {
        // Default to MANY_TO_MANY if not specified
        if (testCase.fieldSpecificConfig != null && testCase.fieldSpecificConfig.contains("cardinality:")) {
            return testCase.fieldSpecificConfig.split("cardinality:")[1].trim();
        }

        // Default cardinality (MANY_TO_MANY = 2)
        return String.valueOf(RELATIONSHIP_CARDINALITY.MANY_TO_MANY.ordinal());
    }

    private ContentType createRelatedContentType() throws DotDataException {
        final String timestamp = String.valueOf(System.currentTimeMillis());
        final String relatedTypeName = "RelatedType_" + timestamp;
        final String relatedTypeVar = "relatedType" + timestamp;

        // Create basic fields for the related content type
        List<com.dotcms.contenttype.model.field.Field> relatedFields = new ArrayList<>();

        relatedFields.add(
                ImmutableTextField.builder()
                        .name("relatedTitle")
                        .variable("relatedTitle")
                        .required(true)
                        .listed(true)
                        .indexed(true)
                        .sortOrder(1)
                        .searchable(true)
                        .build());

        relatedFields.add(
                ImmutableTextAreaField.builder()
                        .name("relatedBody")
                        .variable("relatedBody")
                        .required(false)
                        .listed(true)
                        .indexed(true)
                        .sortOrder(2)
                        .searchable(true)
                        .build());

        // Create the content type
        ContentType relatedType = new ContentTypeDataGen()
                .name(relatedTypeName)
                .velocityVarName(relatedTypeVar)
                .host(APILocator.systemHost())
                .fields(relatedFields)
                .nextPersisted();

        // Set up workflow for the related content type
        workflowAPI.saveSchemesForStruct(new StructureTransformer(relatedType).asStructure(),
                Collections.singletonList(schemeStepActionResult1.getScheme()));

        return relatedType;
    }

    private Contentlet createRelatedContentlet(ContentType relatedContentType){
        final String timestamp = String.valueOf(System.currentTimeMillis());

        return new ContentletDataGen(relatedContentType.id())
                .languageId(defaultLanguage.getId())
                .host(defaultSite)
                .setProperty("relatedTitle", "Related Content " + timestamp)
                .setProperty("relatedBody", "This is related content for relationship testing")
                .nextPersisted();
    }

    /**
     * Creates the field list for a specific test case
     */
    private List<com.dotcms.contenttype.model.field.Field> createFieldsForTestCase(
            FieldTestCase testCase, Category testCategory, ContentType relatedContentType) {

        List<com.dotcms.contenttype.model.field.Field> fields = new ArrayList<>();

        // Always add a tracking field
        fields.add(
                FieldBuilder.builder(TextField.class)
                        .name("trackingInfo")
                        .variable("trackingInfo")
                        .required(false)
                        .build()
        );

        // Create the specific field being tested
        FieldBuilder fieldBuilder = FieldBuilder.builder(testCase.fieldClass)
                .name(testCase.fieldVariable)
                .variable(testCase.fieldVariable)
                .dataType(testCase.dataType)
                .required("required".equals(testCase.fieldSpecificConfig))
                .unique("unique".equals(testCase.fieldSpecificConfig));

        if(null != relatedContentType){
            // Set-up pieces required for the relationship to work
            fieldBuilder
                    .values(getCardinalityValue(testCase)) // Set cardinality
                    .relationType(relatedContentType.variable());
        }

        // Add field-specific configurations
        if (testCase.requiresCategory && testCategory != null) {
            fieldBuilder = fieldBuilder.values(testCategory.getInode());
        }

        fields.add(fieldBuilder.build());

        return fields;
    }

    /**
     * Creates CSV content specific to the field being tested
     */
    private String createCSVForFieldTest(FieldTestCase testCase, Host testSite, Category testCategory) {
        String validValue = testCase.validValue;
        String invalidValue = testCase.invalidValue;

        // Adjust values based on field requirements
        if (testCase.requiresHost && testSite != null) {
            validValue = defaultSite.getIdentifier();
        }
        if (testCase.requiresCategory && testCategory != null) {
            validValue = testCategory.getKey();
        }

        return String.format(
                "trackingInfo,%s\r\n" +
                        "Row1-Valid,\"%s\"\r\n" +
                        "Row2-Error,\"%s\"\r\n" +
                        "Row3-NeverProcessed,\"This should never be processed\"\r\n",
                testCase.fieldVariable,
                validValue,
                invalidValue
        );
    }

    public static final AssertionsStrategy REQUIRED_TEXT_FIELD_ASSERTION = (result, testCase, contentType) -> {
        final ValidationMessage error = result.error().get(0);
        assertTrue(error.field().isPresent());
        assertEquals("Test expected required field label is present", "requiredTextField",error.field().get());

        assertRequiredField(testCase, error);

        assertTrue(error.code().isPresent());
        assertEquals("Expected Error Code does not match!", REQUIRED_FIELD_MISSING.name(), error.code().get());
    };

    public static final AssertionsStrategy REQUIRED_TEXT_AREA_ASSERTION = (result, testCase, contentType) -> {
        final ValidationMessage error = result.error().get(0);
        assertTrue(error.field().isPresent());
        assertEquals("Test expected required field label is present", "textAreaField",error.field().get());

        assertRequiredField(testCase, error);

        assertTrue(error.code().isPresent());
        assertEquals("Expected Error Code does not match!", REQUIRED_FIELD_MISSING.name(), error.code().get());
    };

    public static final AssertionsStrategy REQUIRED_WYSIWYG_ASSERTION = (result, testCase, contentType) -> {
        final ValidationMessage error = result.error().get(0);
        assertTrue(error.field().isPresent());
        assertEquals("Test expected required field label is present", "wysiwygField",error.field().get());

        assertRequiredField(testCase, error);

        assertTrue(error.code().isPresent());
        assertEquals("Expected Error Code does not match!", REQUIRED_FIELD_MISSING.name(), error.code().get());
    };

    public static final AssertionsStrategy REQUIRED_STORY_BLOCK_ASSERTION = (result, testCase, contentType) -> {
        final ValidationMessage error = result.error().get(0);
        assertTrue(error.field().isPresent());
        assertEquals("Test expected required field label is present", "storyBlockField",error.field().get());

        assertRequiredField(testCase, error);

        assertTrue(error.code().isPresent());
        assertEquals("Expected Error Code does not match!", REQUIRED_FIELD_MISSING.name(), error.code().get());
    };

    public static final AssertionsStrategy INVALID_SELECT_NUMBER_ASSERTION = (result, testCase, contentType) -> {
        final ValidationMessage error = result.error().get(0);
        assertTrue(error.field().isPresent());
        assertTrue(error.invalidValue().isPresent());
        assertEquals("Test expected required field label is present",
                String.format("Unable to set string value '%s' as a Float for the field: %s",testCase.invalidValue,testCase.fieldVariable),
                error.message());

        assertTrue(error.code().isPresent());
        assertEquals("Expected Error Code does not match!", INVALID_NUMBER_FORMAT.name(), error.code().get());
    };

    public static final AssertionsStrategy UNIQUE_TEXT_FIELD_ASSERTION = (result, testCase, contentType) -> {
        final ValidationMessage error = result.error().get(0);
        assertTrue(error.field().isPresent());
        assertThat(error.message().trim(), allOf(
            containsString(testCase.fieldVariable)
        ));

        assertTrue(error.code().isPresent());
        assertEquals("Expected Error Code does not match!", DUPLICATE_UNIQUE_VALUE.name(), error.code().get());
    };

    private static void assertRequiredField(FieldTestCase testCase, ValidationMessage error) {
        assertThat(error.message().trim(), allOf(
                startsWith("Contentlet with ID"),
                containsString("has invalid/missing field(s)"),
                containsString("Fields: [REQUIRED]"),
                containsString(testCase.fieldVariable),
                containsString("(" + testCase.fieldVariable  + ")")
        ));
    }

    public static final AssertionsStrategy INVALID_DATE_ASSERTION = (result, testCase, contentType) -> {
        final ValidationMessage error = result.error().get(0);
        assertTrue(error.field().isPresent());
        assertTrue(error.invalidValue().isPresent());

        assertEquals("Test expected required field label is present",testCase.fieldVariable, error.field().get());
        assertEquals("Test expected required field label is present",testCase.invalidValue, error.invalidValue().get());

        assertThat(error.message().trim(), allOf(
                startsWith("Unable to convert string "),
                containsString(testCase.invalidValue),
                containsString("to"),
                containsString("field:"),
                containsString(testCase.fieldVariable))
        );

        assertTrue(error.code().isPresent());
        assertEquals("Expected Error Code does not match!", INVALID_DATE_FORMAT.name(), error.code().get());
    };

    public static final AssertionsStrategy INVALID_INT_NUMBER_ASSERTION = (result, testCase, contentType) -> {
        final ValidationMessage error = result.error().get(0);
        assertTrue(error.field().isPresent());
        assertTrue(error.invalidValue().isPresent());

        assertEquals("Test expected required field label is present",testCase.fieldVariable,error.field().get());
        assertEquals("Test expected required field label is present",testCase.invalidValue,error.invalidValue().get());
        assertThat(error.message().trim(), allOf(
                startsWith(String.format("Unable to set string value '%s' as a ",testCase.invalidValue)),
                containsString("for the field:"),
                endsWith(testCase.fieldVariable)
        ));

        assertTrue(error.code().isPresent());
        assertEquals("Expected Error Code does not match!", INVALID_NUMBER_FORMAT.name(), error.code().get());
    };

    public static final AssertionsStrategy INVALID_BINARY_ASSERTION = (result, testCase, contentType) -> {
        final ValidationMessage error = result.error().get(0);
        assertTrue(error.field().isPresent());
        assertTrue(error.invalidValue().isPresent());

        assertEquals("Test expected required field label is present", testCase.fieldVariable, error.field().get());
        assertEquals("Test expected required field label is present", testCase.invalidValue, error.invalidValue().get());
        assertEquals("Test expected error message is present","URL is syntactically valid but returned a non-success HTTP response", error.message());

        assertTrue(error.code().isPresent());
        assertEquals("Expected Error Code does not match!", UNREACHABLE_URL_CONTENT.name(), error.code().get());
    };

    public static final AssertionsStrategy INVALID_IMAGE_PATH_ASSERTION = (result, testCase, contentType) -> {
        final ValidationMessage error = result.error().get(0);
        assertTrue(error.field().isPresent());
        assertTrue(error.invalidValue().isPresent());

        assertEquals("Test expected required field label is present",testCase.fieldVariable,error.field().get());
        assertEquals("Test expected required field label is present",testCase.invalidValue,error.invalidValue().get());

        assertEquals("","Unable to match the given path with a file stored in dotCMS",error.message());
        assertTrue(error.code().isPresent());
        assertEquals("Expected Error Code does not match!", INVALID_FILE_PATH.name(), error.code().get());
    };

    public static final AssertionsStrategy INVALID_IMAGE_URL_ASSERTION = (result, testCase, contentType) -> {
        final ValidationMessage error = result.error().get(0);
        assertTrue(error.field().isPresent());
        assertTrue(error.invalidValue().isPresent());

        assertEquals("Test expected required field label is present",testCase.fieldVariable,error.field().get());
        assertEquals("Test expected required field label is present",testCase.invalidValue,error.invalidValue().get());

        assertEquals("Test expected error is for the url","URL is syntactically valid but returned a non-success HTTP response", error.message());
        assertTrue(error.code().isPresent());
        assertEquals("Expected Error Code does not match!", UNREACHABLE_URL_CONTENT.name(), error.code().get());
    };

    public static final AssertionsStrategy INVALID_HOST_FOLDER_ASSERTION = (result, testCase, contentType) -> {
        final ValidationMessage error = result.error().get(0);
        assertTrue(error.field().isPresent());
        assertTrue(error.invalidValue().isPresent());

        assertEquals("Test expected required field label is present",testCase.fieldVariable,error.field().get());
        assertEquals("Test expected required field label is present",testCase.invalidValue,error.invalidValue().get());

        assertEquals("Test expected error is for the url","The provided inode/path does not exist or is not associated with a valid Site or Folder.", error.message());
        assertTrue(error.code().isPresent());
        assertEquals("Expected Error Code does not match!", INVALID_SITE_FOLDER_REF.name(), error.code().get());
    };

    public static final AssertionsStrategy INVALID_CATEGORY_ASSERTION = (result, testCase, contentType) -> {
        final ValidationMessage error = result.error().get(0);
        assertTrue(error.field().isPresent());
        assertTrue(error.invalidValue().isPresent());

        assertEquals("Test expected required field label is present",testCase.fieldVariable,error.field().get());
        assertEquals("Test expected required field label is present",testCase.invalidValue,error.invalidValue().get());

        assertThat(error.message().trim(), allOf(
                startsWith("Invalid category key found:"),
                containsString("It must exist and be a child of")
        ));
        assertTrue(error.code().isPresent());
        assertEquals("Expected Error Code does not match!", INVALID_CATEGORY_KEY.name(), error.code().get());
    };

    public static final AssertionsStrategy JSON_FIELD_ASSERTION = (result, testCase, contentType) -> {
        final ValidationMessage error = result.error().get(0);
        assertTrue(error.field().isPresent());
        assertTrue(error.invalidValue().isPresent());

        assertEquals("Test expected required field label is present",testCase.fieldVariable,error.field().get());
        assertEquals("Test expected required field label is present",testCase.invalidValue,error.invalidValue().get());

        assertThat(error.message().trim(), allOf(
                startsWith("Invalid JSON field provided."),
                endsWith(testCase.fieldVariable)
        ));

        assertThat(error.context().get("errorHint").toString().trim(), allOf(
                startsWith("Unexpected character (''' (code 39)): was expecting double-quote to start field name"),
                not(containsString("[Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled);")),
                containsString("line: 1, column: 2")
        ));

        assertTrue(error.code().isPresent());
        assertEquals("Expected Error Code does not match!", INVALID_JSON.name(), error.code().get());
    };

    public static final AssertionsStrategy RELATIONSHIP_FIELD_VALIDATION = (result, testCase, contentType) -> {
        final ValidationMessage error = result.error().get(0);
        assertTrue(error.field().isPresent());
        assertTrue(error.invalidValue().isPresent());
        assertThat(error.message().trim(), allOf(
             startsWith(String.format("The field has a value (%s) that is not an identifier nor a lucene query", testCase.invalidValue))
        ));
    };

    /**
     * Validates results specific to each field type
     */
    private void validateResults(ImportResult result, FieldTestCase testCase, ContentType contentType)
            throws DotDataException, DotSecurityException {

        assertTrue("Should have always stopped on the introduced error for:" + testCase, result.stoppedOnErrorAtLine().isPresent());

        assertEquals("All Errors landed in row #3", Integer.valueOf(3), result.stoppedOnErrorAtLine().get());

        assertNotNull("Result should not be null for " + testCase.fieldTypeName, result);

        // Should have exactly 1 error (from row 2)
        assertFalse("Should have errors for " + testCase.fieldTypeName, result.error().isEmpty());
        assertEquals("Should have exactly 1 error for " + testCase.fieldTypeName,
                1, result.error().size());

        // Validate import behavior (stopOnError=true)
        Optional<ResultData> resultData = result.data();
        assertTrue("Result data should be present for " + testCase.fieldTypeName, resultData.isPresent());

        // Should have processed 3 rows (header + 2 data rows) before stopping
        assertEquals("Should have processed 3 rows for " + testCase.fieldTypeName,
                3, resultData.get().processed().parsedRows());

        // Should have 1 failed row (row 2)
        assertEquals("Should have 1 failed row for " + testCase.fieldTypeName,
                1, resultData.get().processed().failedRows());

        // Should have created 1 content (row 1 only)
        assertEquals("Should have created 1 content for " + testCase.fieldTypeName,
                1, resultData.get().summary().createdContent());

        // Should have 0 rollbacks (stopOnError=true doesn't roll back)
        assertEquals("Should have 0 rollbacks for " + testCase.fieldTypeName,
                0, resultData.get().summary().rollbacks());

        // Verify exactly 1 contentlet was saved
        List<Contentlet> savedData = contentletAPI.findByStructure(contentType.inode(), user, false, 0, 0);
        assertEquals("Should have saved 1 contentlet for " + testCase.fieldTypeName,
                1, savedData.size());

        // Verify it's the first (valid) row
        assertEquals("Saved contentlet should be from Row1 for " + testCase.fieldTypeName,
                "Row1-Valid", savedData.get(0).getStringProperty("trackingInfo"));

        Logger.info(this, "✓ Field test passed for: " + testCase.fieldTypeName +
                " | Error: " + testCase.expectedErrorCode +
                " | Row processed: " + resultData.get().processed().parsedRows() +
                " | Content created: " + resultData.get().summary().createdContent());

        //Verify we have an error code and it matches the expected
        ValidationMessage error = result.error().get(0);
        assertTrue("Error code should be present for " + testCase.fieldTypeName,
                error.code().isPresent());
        assertEquals("Error code should match expected for " + testCase.fieldTypeName,
                testCase.expectedErrorCode, error.code().get());

        if(null != testCase.assertionsStrategy){
            testCase.assertionsStrategy.asserts(result,testCase,contentType);
        }
    }

    /**
     * Enhanced cleanup method that handles relationship-related objects
     */
    private void cleanupTestData(ContentType contentType, Category testCategory, Host testSite, Contentlet relatedContentlet, ContentType relatedContentType) {
        try {
            // Clean up main content type
            if (contentType != null) {
                List<Contentlet> contentlets = contentletAPI.findByStructure(contentType.inode(), user, false, 0, 0);
                for (Contentlet contentlet : contentlets) {
                    contentletAPI.archive(contentlet, user, false);
                    contentletAPI.delete(contentlet, user, false);
                }
                contentTypeApi.delete(contentType);
            }

            // Clean up related contentlet and content type (for relationship tests)
            if (relatedContentlet != null) {
                try {
                    contentletAPI.archive(relatedContentlet, user, false);
                    contentletAPI.delete(relatedContentlet, user, false);
                } catch (Exception e) {
                    Logger.warn(ImportUtil.class,"Error cleaning up related contentlet", e);
                }
            }

            if (relatedContentType != null) {
                try {
                    // Clean up any remaining related contentlets
                    List<Contentlet> relatedContentlets = contentletAPI.findByStructure(relatedContentType.inode(), user, false, 0, 0);
                    for (Contentlet contentlet : relatedContentlets) {
                        contentletAPI.archive(contentlet, user, false);
                        contentletAPI.delete(contentlet, user, false);
                    }
                    contentTypeApi.delete(relatedContentType);
                } catch (Exception e) {
                    Logger.warn(ImportUtil.class,"Error cleaning up related content type", e);
                }
            }

            // Clean up other test objects
            if (testCategory != null) {
                CategoryDataGen.delete(testCategory);
            }
            if (testSite != null) {
                APILocator.getHostAPI().archive(testSite, APILocator.systemUser(), false);
                APILocator.getHostAPI().delete(testSite, APILocator.systemUser(), false);
            }
        } catch (Exception e) {
            Logger.error("Error cleaning up test data", e);
        }
    }

    /**
     * Method to test: {@link ImportUtil#importFile(Long, String, String, String[], boolean, boolean, User, long, String[], CsvReader, int, int, Reader, String, HttpServletRequest)}
     * Given Scenario:
     * - ContentType with a text field (slug) and a HostFolderField (site)
     * - Single multilingual CSV file with 2 languages (default language + Spanish) using site NAME (not identifier) as key field
     * - Both "slug" and "site" fields are set as key fields
     * - Both CSV rows have the same slug value and same site name
     * Expected result:
     * - Create only ONE contentlet with the same identifier (2 language versions of same content)
     * - Create 2 different inodes (one per language version)
     * - Both versions should be published
     *
     * This test validates that the HostFolderField comparison works correctly when comparing
     * site names (from CSV) with site identifiers (stored in contentlets) during multilingual import.
     *
     * @throws DotSecurityException
     * @throws DotDataException
     * @throws IOException
     */
    @Test
    public void testImportMultilingualWithSiteNameAsKeyField() throws DotSecurityException, DotDataException, IOException {
        ContentType contentType = null;
        Host testSite = null;
        Language lang1 = null;
        Language lang2 = null;

        try {
            // Create a test site with a name
            String siteName = "testsite" + System.currentTimeMillis();
            testSite = new SiteDataGen()
                    .name(siteName)
                    .nextPersisted();

            lang1 = defaultLanguage;



            lang2 = APILocator.getLanguageAPI().getLanguage("es", "ES");
            if (lang2 == null) {
                lang2 = new LanguageDataGen().languageCode("es").countryCode("ES")
                        .languageName("Spanish").country("Spain").nextPersisted();
            }

            // Create ContentType with slug and site fields
            com.dotcms.contenttype.model.field.Field slugField = new FieldDataGen()
                    .name("slug")
                    .velocityVarName("slug")
                    .type(TextField.class)
                    .next();

            com.dotcms.contenttype.model.field.Field siteField = new FieldDataGen()
                    .name("site")
                    .velocityVarName("site")
                    .type(HostFolderField.class)
                    .next();

            contentType = new ContentTypeDataGen()
                    .field(slugField)
                    .field(siteField)
                    .nextPersisted();

            slugField = fieldAPI.byContentTypeAndVar(contentType, slugField.variable());
            siteField = fieldAPI.byContentTypeAndVar(contentType, siteField.variable());

            String csvContent = "languageCode,countryCode,slug,site\r\n" +
                    lang1.getLanguageCode().trim() + "," + lang1.getCountryCode().trim() + ",test-article," + testSite.getHostname().trim() + "\r\n" +
                    lang2.getLanguageCode().trim() + "," + lang2.getCountryCode().trim() + ",test-article," + testSite.getHostname().trim() + "\r\n";

            Logger.info(this, "CSV Content:\n" + csvContent);

            final Reader reader = createTempFile(csvContent);
            final CsvReader csvreader = new CsvReader(reader);
            csvreader.setSafetySwitch(false);

            final String[] csvHeaders = csvreader.getHeaders();

            final HashMap<String, List<String>> imported = ImportUtil.importFile(
                    0L,
                    testSite.getIdentifier(),
                    contentType.inode(),
                    new String[]{slugField.id(), siteField.id()}, // Key fields: slug + site
                    false,
                    true,
                    user,
                    -1,
                    csvHeaders,
                    csvreader,
                    0,
                    1,
                    reader,
                    schemeStepActionResult1.getAction().getId(),
                    getHttpRequest()
            );

            // Validate import results
            final List<String> results = imported.get("results");
            assertNotNull("Import results should not be null", results);
            assertFalse("Import results should not be empty", results.isEmpty());

            final List<String> errors = imported.get("errors");
            assertTrue("Import should have no errors: " + errors, errors == null || errors.isEmpty());


            final List<Contentlet> contentlets = contentletAPI.findByStructure(
                    contentType.inode(),
                    user,
                    false,
                    0,
                    -1
            );

            assertEquals("Should have 2 language versions", 2, contentlets.size());

            final String firstIdentifier = contentlets.get(0).getIdentifier();

            for (Contentlet contentlet : contentlets) {
                assertEquals("All contentlets should have the same identifier",
                        firstIdentifier,
                        contentlet.getIdentifier());
                
                assertEquals("Slug should be 'test-article'",
                        "test-article",
                        contentlet.getStringProperty(slugField.variable()));
            }


            final List<Long> languageIds = contentlets.stream()
                    .map(Contentlet::getLanguageId)
                    .sorted()
                    .collect(Collectors.toList());

            assertTrue("Should have default language version", languageIds.contains(lang1.getId()));
            assertTrue("Should have Spanish version", languageIds.contains(lang2.getId()));


            final List<String> inodes = contentlets.stream()
                    .map(Contentlet::getInode)
                    .distinct()
                    .collect(Collectors.toList());

            assertEquals("Both versions should have unique inodes", 2, inodes.size());

        } finally {
            // Cleanup
            if (contentType != null) {
                try {
                    List<Contentlet> contentlets = contentletAPI.findByStructure(
                            contentType.inode(),
                            user,
                            false,
                            0,
                            -1
                    );
                    for (Contentlet contentlet : contentlets) {
                        contentletAPI.archive(contentlet, user, false);
                        contentletAPI.delete(contentlet, user, false);
                    }
                    contentTypeApi.delete(contentType);
                } catch (Exception e) {
                    Logger.warn(ImportUtilTest.class, "Error cleaning up content type", e);
                }
            }

            if (testSite != null) {
                try {
                    APILocator.getHostAPI().archive(testSite, user, false);
                    APILocator.getHostAPI().delete(testSite, user, false);
                } catch (Exception e) {
                    Logger.warn(ImportUtilTest.class, "Error cleaning up test site", e);
                }
            }

            // Clean up languages if we created them (only if they didn't exist before)
            if (lang1 != null) {
                try {
                    // Check if this language has ID > 1 (meaning we created it, not a default language)
                    if (lang1.getId() > 1) {
                        APILocator.getLanguageAPI().deleteLanguage(lang1);
                    }
                } catch (Exception e) {
                    Logger.warn(ImportUtilTest.class, "Error cleaning up language 1", e);
                }
            }

            if (lang2 != null) {
                try {
                    // Spanish is typically ID 2, but if we created it, clean it up
                    // Check if this is not a system default language
                    if (lang2.getId() > 2) {
                        APILocator.getLanguageAPI().deleteLanguage(lang2);
                    }
                } catch (Exception e) {
                    Logger.warn(ImportUtilTest.class, "Error cleaning up language 2", e);
                }
            }
        }
    }

}
