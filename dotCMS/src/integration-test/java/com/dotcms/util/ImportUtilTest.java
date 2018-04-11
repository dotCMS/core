package com.dotcms.util;

import com.dotcms.contenttype.business.ContentTypeAPIImpl;
import com.dotcms.contenttype.business.FieldAPI;
import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.field.HostFolderField;
import com.dotcms.contenttype.model.field.ImmutableTextAreaField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.dotcms.contenttype.model.field.TextField;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.repackage.com.csvreader.CsvReader;
import com.dotcms.repackage.org.apache.commons.io.FileUtils;
import com.dotcms.uuid.shorty.ShortyIdAPI;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.Role;
import com.dotmarketing.business.RoleAPI;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.workflows.actionlet.PublishContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentActionlet;
import com.dotmarketing.portlets.workflows.actionlet.SaveContentAsDraftActionlet;
import com.dotmarketing.portlets.workflows.business.BaseWorkflowIntegrationTest;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.model.WorkflowAction;
import com.dotmarketing.portlets.workflows.model.WorkflowState;
import com.dotmarketing.portlets.workflows.model.WorkflowStep;
import com.dotmarketing.portlets.workflows.model.WorkflowTask;
import com.dotmarketing.util.ImportUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UUIDGenerator;
import com.liferay.portal.model.User;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Verifies that the Content Importer/Exporter feature is working as expected.
 * Users can import and export contents from the Content Search page.
 * 
 * @author Jonathan Gamba Date: 3/10/14
 */
public class ImportUtilTest extends BaseWorkflowIntegrationTest {

    private static User user;
    private static Host defaultSite;
    private static Language defaultLanguage;
    private static ContentTypeAPIImpl contentTypeApi;
    private static FieldAPI fieldAPI;
    private static BaseWorkflowIntegrationTest.CreateSchemeStepActionResult schemeStepActionResult1 = null;
    private static BaseWorkflowIntegrationTest.CreateSchemeStepActionResult schemeStepActionResult2 = null;
    private static ContentletAPI contentletAPI;
    private static ShortyIdAPI shortyIdAPI;
    private static WorkflowAPI workflowAPI;
    private static WorkflowAction action1;
    private static WorkflowAction action2;
    private static WorkflowAction action3;
    private static RoleAPI roleAPI;
    private static PermissionAPI permissionAPI;
    private static final String TITLE_FIELD_NAME = "testTitle";
    private static final String BODY_FIELD_NAME = "testBody";
    private static final String STEP_BY_USING_ACTION1 = "StepByUsingAction1";
    private static final String STEP_BY_USING_ACTION2 = "StepByUsingAction2";
    private static final String STEP_BY_USING_ACTION3 = "StepByUsingAction3";

    private static final int editPermission =
            PermissionAPI.PERMISSION_READ + PermissionAPI.PERMISSION_EDIT;
    private static final int publishPermission = editPermission + PermissionAPI.PERMISSION_PUBLISH;

    private static User joeContributor;
    private static User janeReviewer;
    private static User chrisPublisher;

    private static Role reviewerRole;
    private static Role contributorRole;
    private static Role publisherRole;
    private static Role anyWhoCanEditRole;
    private static Role anyWhoCanPublishRole;

    private static WorkflowStep step1;
    private static WorkflowStep step2;
    private static WorkflowStep step3;

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        user = APILocator.getUserAPI().getSystemUser();
        defaultSite = APILocator.getHostAPI().findDefaultHost(user, false);
        defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        contentTypeApi = (ContentTypeAPIImpl) APILocator.getContentTypeAPI(user);
        fieldAPI = APILocator.getContentTypeFieldAPI();
        workflowAPI = APILocator.getWorkflowAPI();
        permissionAPI = APILocator.getPermissionAPI();
        roleAPI = APILocator.getRoleAPI();
        contentletAPI = APILocator.getContentletAPI();
        shortyIdAPI = APILocator.getShortyAPI();

        // creates the scheme, step1 and action1
        schemeStepActionResult1 = createSchemeStepActionActionlet
                ("ImportUtilScheme" + UUIDGenerator.generateUuid(), "step1", "action1",
                        SaveContentActionlet.class);

        //Second Workflow
        anyWhoCanEditRole = roleAPI.loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_EDIT_ROLE_KEY);
        anyWhoCanPublishRole = roleAPI.loadRoleByKey(RoleAPI.WORKFLOW_ANY_WHO_CAN_PUBLISH_ROLE_KEY);
        publisherRole = roleAPI.findRoleByName("Publisher / Legal", null);
        reviewerRole = roleAPI.findRoleByName("Reviewer", publisherRole);
        contributorRole = roleAPI.findRoleByName("Contributor", reviewerRole);

        schemeStepActionResult2 = createSchemeStepActionActionlet(
                "ImportUtilScheme_2_" + UUIDGenerator.generateUuid(), "initialStep", "Save",
                SaveContentActionlet.class);
        step1 = createNewWorkflowStep(STEP_BY_USING_ACTION1,
                schemeStepActionResult2.getScheme().getId());
        step1.setMyOrder(1);
        workflowAPI.saveStep(step1, user);

        step2 = createNewWorkflowStep(STEP_BY_USING_ACTION2,
                schemeStepActionResult2.getScheme().getId());
        step2.setMyOrder(2);
        workflowAPI.saveStep(step2, user);

        step3 = createNewWorkflowStep(STEP_BY_USING_ACTION3,
                schemeStepActionResult2.getScheme().getId());
        step3.setMyOrder(3);
        workflowAPI.saveStep(step3, user);

        final List<String> rolesIds = new ArrayList<>();

        //Setting Action 1 configuration
        action1 = schemeStepActionResult2.getAction();
        rolesIds.add(anyWhoCanEditRole.getId());
        action1.setNextStep(step1.getId());
        action1.setShowOn(WorkflowState.LOCKED, WorkflowState.UNLOCKED, WorkflowState.NEW,
                WorkflowState.PUBLISHED, WorkflowState.UNPUBLISHED);
        addWhoCanUseToAction(action1, rolesIds);
        rolesIds.remove(anyWhoCanEditRole.getId());

        //Setting Action 2 configuration
        BaseWorkflowIntegrationTest.CreateSchemeStepActionResult schemeResultTemp = createActionActionlet(
                schemeStepActionResult2.getScheme().getId(),
                schemeStepActionResult2.getStep().getId(), "Save & Publish",
                SaveContentActionlet.class);
        action2 = schemeResultTemp.getAction();
        addActionletToAction(action2.getId(), PublishContentActionlet.class, 1);

        action2.setNextStep(step2.getId());
        rolesIds.add(anyWhoCanPublishRole.getId());
        action2.setShowOn(WorkflowState.LOCKED, WorkflowState.UNLOCKED, WorkflowState.NEW,
                WorkflowState.PUBLISHED, WorkflowState.UNPUBLISHED);
        addWhoCanUseToAction(action2, rolesIds);
        rolesIds.remove(anyWhoCanPublishRole.getId());

        //Setting action 3 configuration
        schemeResultTemp = createActionActionlet(schemeStepActionResult2.getScheme().getId(),
                schemeStepActionResult2.getStep().getId(), "Save as Draft",
                SaveContentAsDraftActionlet.class);
        action3 = schemeResultTemp.getAction();
        action3.setNextStep(step3.getId());
        rolesIds.add(reviewerRole.getId());
        action3.setShowOn(WorkflowState.LOCKED, WorkflowState.UNLOCKED, WorkflowState.NEW,
                WorkflowState.PUBLISHED, WorkflowState.UNPUBLISHED);
        addWhoCanUseToAction(action3, rolesIds);
        rolesIds.remove(reviewerRole.getId());

        //Special Users
        joeContributor = APILocator.getUserAPI().loadUserById("dotcms.org.2789");
        janeReviewer = APILocator.getUserAPI().loadUserById("dotcms.org.2787");
        chrisPublisher = APILocator.getUserAPI().loadUserById("dotcms.org.2795");

    }

    /**
     * Testing the {@link ImportUtil#importFile(Long, String, String, String[], boolean, boolean,
     * com.liferay.portal.model.User, long, String[], com.dotcms.repackage.com.csvreader.CsvReader,
     * int, int, java.io.Reader, String)} method
     */
    @Test
    @Ignore("Temporarily disabled")
    public void importFile()
            throws DotDataException, DotSecurityException, IOException, InterruptedException {

        ContentletAPI contentletAPI = APILocator.getContentletAPI();

        //Create a test Content Type
        String contentTypeSuffix = String.valueOf(new Date().getTime());
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
                            -1, reader, schemeStepActionResult1.getAction().getId());
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
                            -1, reader, schemeStepActionResult1.getAction().getId());
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
                            -1, reader, schemeStepActionResult1.getAction().getId());
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
                    schemeStepActionResult1.getAction().getId());
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
                            -1, reader, schemeStepActionResult1.getAction().getId());
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
                    schemeStepActionResult1.getAction().getId());
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
            contentTypeApi.delete(new StructureTransformer(contentType).from());
        }
    }

    /**
     * Creates a temporal file using a given content
     */
    private Reader createTempFile(String content) throws IOException {

        File tempTestFile = File
                .createTempFile("csvTest_" + String.valueOf(new Date().getTime()), ".txt");
        FileUtils.writeStringToFile(tempTestFile, content);
        byte[] bytes = com.liferay.util.FileUtil.getBytes(tempTestFile);

        return new InputStreamReader(new ByteArrayInputStream(bytes), Charset.forName("UTF-8"));
    }

    /**
     * Validates a given result generated by the ImportUtil.importFile method
     */
    private void validate(HashMap<String, List<String>> results, Boolean preview,
            Boolean expectingErrors, Boolean expectingWarnings) {

        //Reading the results

        if (expectingErrors) {
            List<String> errors = results.get("errors");
            assertNotNull(errors);//Expected warnings as no key fields were chosen
            assertTrue(!errors.isEmpty());
        } else {
            List<String> errors = results.get("errors");
            assertTrue(errors == null || errors.isEmpty());//No errors should be found
        }

        if (expectingWarnings) {
            List<String> warnings = results.get("warnings");
            assertNotNull(warnings);//Expected warnings as no key fields were chosen
            Logger.info(this, "List WARNINGS: " + warnings.size());
            assertTrue(!warnings.isEmpty());
        } else {
            List<String> warnings = results.get("warnings");
            assertTrue(warnings == null || warnings.isEmpty());
        }

        List<String> finalResults = results.get("results");
        assertNotNull(finalResults);//Expected final results messages
        assertTrue(!finalResults.isEmpty());

        List<String> messages = results.get("messages");
        assertNotNull(messages);//Expected return messages
        assertTrue(!messages.isEmpty());

        if (!preview) {

            List<String> lastInode = results.get("lastInode");
            assertNotNull(lastInode);
            assertTrue(!lastInode.isEmpty());

            List<String> counters = results.get("counters");
            assertNotNull(counters);
            assertTrue(!counters.isEmpty());
        }
    }

    @Test
    public void importFile_success_when_twoLinesHaveSameUniqueKeysButDifferentLanguage()
            throws DotSecurityException, DotDataException, IOException {

        ContentType type;
        CsvReader csvreader;
        com.dotcms.contenttype.model.field.Field titleField, hostField;
        HashMap<String, List<String>> results;
        Reader reader;
        String[] csvHeaders;
        long time;

        //Creating new content type with one unique field
        time = System.currentTimeMillis();

        type = ContentTypeBuilder
                .builder(BaseContentType.getContentTypeClass(BaseContentType.CONTENT.getType()))
                .description("description" + time).folder(FolderAPI.SYSTEM_FOLDER)
                .host(Host.SYSTEM_HOST)
                .name("ContentTypeTestingWithFields" + time).owner("owner")
                .variable("velocityVarNameTesting" + time)
                .build();

        type = contentTypeApi.save(type);

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
                    "es, ES, UniqueTitle, " + defaultSite.getIdentifier() + "\r\n" +
                    "en, US, UniqueTitle, " + defaultSite.getIdentifier() + "\r\n");
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
                                    schemeStepActionResult1.getAction().getId());
            //Validations
            validate(results, true, false, true);

            assertTrue(results.get("warnings").size() == 1);
            assertEquals(results.get("warnings").get(0), "the-structure-field testTitle is-unique");


        } finally {
            contentTypeApi.delete(type);
        }
    }

    @Test
    public void importFile_fails_when_twoLinesHaveSameUniqueKeys()
            throws DotSecurityException, DotDataException, IOException {

        ContentType type;
        CsvReader csvreader;
        com.dotcms.contenttype.model.field.Field titleField, hostField;
        HashMap<String, List<String>> results;
        Reader reader;
        String[] csvHeaders;
        long time;

        //Creating new content type with one unique field
        time = System.currentTimeMillis();

        type = ContentTypeBuilder
                .builder(BaseContentType.getContentTypeClass(BaseContentType.CONTENT.getType()))
                .description("description" + time).folder(FolderAPI.SYSTEM_FOLDER)
                .host(Host.SYSTEM_HOST)
                .name("ContentTypeTestingWithFields" + time).owner("owner")
                .variable("velocityVarNameTesting" + time)
                .build();

        type = contentTypeApi.save(type);

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
                    "en, US, UniqueTitle, " + defaultSite.getIdentifier() + "\r\n" +
                    "en, US, UniqueTitle, " + defaultSite.getIdentifier() + "\r\n");
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
                                    schemeStepActionResult1.getAction().getId());
            //Validations
            validate(results, true, false, true);

            assertTrue(results.get("warnings").size() == 2);
            assertEquals(results.get("warnings").get(0), "the-structure-field testTitle is-unique");
            assertEquals(results.get("warnings").get(1),
                    "Line-- 3 contains-duplicate-values-for-structure-unique-field testTitle and-will-be-ignored");

        } finally {
            contentTypeApi.delete(type);
        }
    }

    /**
     * Import file saving the content with old checking method if :
     *
     * a) on testA, the CSV file doesn't have set a wfActionId
     * b) on testB, the User doesn't have permission to execute the wfActionId set on the CSV and/or
     * c) ont testC, the User doesn' have permisssion to set execute the action Id set on the dropdown
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
                    testA + ", Test without WF Action ID set on CSV, " + "\r\n" +
                    testB + ", Test with WF Action ID set on CSV but no permissions, " + action3
                    .getId() + "\r\n" +
                    testC + ", Test with WF Action ID set on dropdown but not permission, "
                    + action2.getId());
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
                                    action3.getId());
            //Validations
            validate(results, false, false, false);
            assertEquals(results.get("warnings").size(), 0);
            assertEquals(results.get("errors").size(), 0);

            final List<Contentlet> savedData = contentletAPI
                    .findByStructure(contentType.inode(), user, false, 0, 0);
            assertNotNull(savedData);
            assertTrue(savedData.size() == 3);
            for (Contentlet cont : savedData) {
                assertNull(workflowAPI.findTaskByContentlet(cont));
            }

        } finally {
            if (null != contentType) {
                contentTypeApi.delete(contentType);
            }
        }
    }

    /**
     * Import file saving the content with the right action except for the second one that should
     * use the dropdown action. In the following way:
     *
     * a) the testD should be saved with the rigth action and go to step 1
     * b) on testE, the User doesn' have permisssion to execute the action Id and should be using the
     * dropdown action and go to step 3
     * c) the testD should be saved with the rigth action and go to step 3
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
                    + Contentlet.WORKFLOW_ACTION_KEY + "\r\n" +
                    testD + ", Test without WF Action ID set on CSV, " + action1
                    .getId() + "\r\n" +
                    testE
                    + ", Test with WF Action ID set on CSV (but no permission) and using dropdown action, "
                    + action2
                    .getId() + "\r\n" +
                    testF + ", Test with WF Action ID set on CSV, "
                    + action3.getId());
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
                                    action3.getId());
            //Validations
            validate(results, false, false, false);
            assertEquals(results.get("warnings").size(), 0);
            assertEquals(results.get("errors").size(), 0);

            final List<Contentlet> savedData = contentletAPI
                    .findByStructure(contentType.inode(), user, false, 0, 0);
            assertNotNull(savedData);
            assertTrue(savedData.size() == 3);
            for (Contentlet cont : savedData) {
                WorkflowTask task = workflowAPI.findTaskByContentlet(cont);
                if (cont.getStringProperty(TITLE_FIELD_NAME).startsWith(testD)) {
                    assertNotNull(task);
                    assertEquals(task.getStatus(), step1.getId());
                } else {
                    assertNotNull(task);
                    assertEquals(task.getStatus(), step3.getId());
                }
            }

        } finally {
            if (null != contentType) {
                contentTypeApi.delete(contentType);
            }
        }
    }

    /**
     * Import file saving the content with the right action except for the second one that should
     * use the old checking. In the following way:
     *
     * a) the testG should be saved with the rigth action and go to step 1
     * b) on testH, the User doesn't have permisssion to execute the action Id and should be using
     * the old checkin and no step associated
     * c) the testI should be saved with the rigth action and go to step 3
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
                    + Contentlet.WORKFLOW_ACTION_KEY + "\r\n" +
                    testG + ", Test without WF Action ID set on CSV, " + action1
                    .getId() + "\r\n" +
                    testH + ", Test with WF Action ID set on CSV but no permissions, " + action2
                    .getId() + "\r\n" +
                    testI + ", Test with WF Action ID set on CSV, "
                    + action3.getId());
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
                                    null);
            //Validations
            validate(results, false, false, false);
            assertEquals(results.get("warnings").size(), 0);
            assertEquals(results.get("errors").size(), 0);

            final List<Contentlet> savedData = contentletAPI
                    .findByStructure(contentType.inode(), user, false, 0, 0);
            assertNotNull(savedData);
            assertTrue(savedData.size() == 3);
            for (Contentlet cont : savedData) {
                WorkflowTask task = workflowAPI.findTaskByContentlet(cont);
                if (cont.getStringProperty(TITLE_FIELD_NAME).startsWith(testG)) {
                    assertNotNull(task);
                    assertEquals(task.getStatus(), step1.getId());
                } else if (cont.getStringProperty(TITLE_FIELD_NAME).startsWith(testH)) {
                    assertNull(task);
                } else {
                    assertNotNull(task);
                    assertEquals(task.getStatus(), step3.getId());
                }
            }

        } finally {
            if (null != contentType) {
                contentTypeApi.delete(contentType);
            }
        }
    }

    /**
     * Import file saving with the right action Id using actions with shortIds. In the
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
                    testJ + ", Test with WF Action ID set on CSV, " + shortyIdAPI.shortify(action1
                    .getId()) + "\r\n" +
                    testK + ", Test with WF Action ID set on CSV, " + shortyIdAPI.shortify(action2
                    .getId()) + "\r\n" +
                    testL + ", Test with WF Action ID set on CSV, " + shortyIdAPI.shortify(action3
                    .getId()));
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
                                    action2.getId());
            //Validations
            validate(results, false, false, false);
            assertEquals(results.get("warnings").size(), 0);
            assertEquals(results.get("errors").size(), 0);

            final List<Contentlet> savedData = contentletAPI
                    .findByStructure(contentType.inode(), user, false, 0, 0);
            assertNotNull(savedData);
            assertTrue(savedData.size() == 3);
            for (Contentlet cont : savedData) {
                WorkflowTask task = workflowAPI.findTaskByContentlet(cont);
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
            if (null != contentType) {
                contentTypeApi.delete(contentType);
            }
        }
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

        //Creating new content type for impor
        com.dotcms.contenttype.model.field.Field titleField, bodyField;

        ContentType contentType = ContentTypeBuilder
                .builder(BaseContentType.getContentTypeClass(BaseContentType.CONTENT.getType()))
                .description("description").folder(FolderAPI.SYSTEM_FOLDER)
                .host(Host.SYSTEM_HOST)
                .name(contentTypeName).owner("owner")
                .variable(contentTypeVarName)
                .build();

        contentType = contentTypeApi.save(contentType);

        titleField =
                ImmutableTextField.builder()
                        .name(TITLE_FIELD_NAME)
                        .variable(TITLE_FIELD_NAME)
                        .required(true)
                        .listed(true)
                        .indexed(true)
                        .sortOrder(1)
                        .contentTypeId(contentType.id())
                        .fixed(true)
                        .searchable(true)
                        .values("")
                        .build();

        fieldAPI.save(titleField, user);

        bodyField =
                ImmutableTextAreaField.builder()
                        .name(BODY_FIELD_NAME)
                        .variable(BODY_FIELD_NAME)
                        .required(false)
                        .listed(true)
                        .indexed(true)
                        .sortOrder(1)
                        .contentTypeId(contentType.id())
                        .fixed(true)
                        .searchable(true)
                        .values("")
                        .build();

        fieldAPI.save(bodyField, user);

        //refresh Content Type
        contentType = contentTypeApi.find(contentType.id());

        //Setting permissions permissions
        Permission p = new Permission(contentType.getPermissionId(), contributorRole.getId(),
                editPermission, true);
        permissionAPI.save(p, contentType, user, true);

        p = new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(),
                contributorRole.getId(), editPermission, true);
        permissionAPI.save(p, contentType, user, true);

        //Publish permission for any publisher
        p = new Permission(contentType.getPermissionId(), publisherRole.getId(),
                publishPermission, true);
        permissionAPI.save(p, contentType, user, true);

        p = new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(),
                publisherRole.getId(), publishPermission, true);
        permissionAPI.save(p, contentType, user, true);

        //Edit Prmission for reviewer
        p = new Permission(contentType.getPermissionId(), reviewerRole.getId(),
                editPermission, true);
        permissionAPI.save(p, contentType, user, true);

        p = new Permission(Contentlet.class.getCanonicalName(), contentType.getPermissionId(),
                reviewerRole.getId(), editPermission, true);
        permissionAPI.save(p, contentType, user, true);

        workflowAPI.saveSchemesForStruct(new StructureTransformer(contentType).asStructure(),
                Arrays.asList(schemeStepActionResult2.getScheme()));

        return contentType;
    }
}
