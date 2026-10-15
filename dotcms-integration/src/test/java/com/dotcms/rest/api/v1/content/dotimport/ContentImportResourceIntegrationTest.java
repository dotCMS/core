package com.dotcms.rest.api.v1.content.dotimport;

import static com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants.WORKFLOW_PUBLISH_ACTION_ID;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.jobs.business.job.JobState;
import java.util.Map;
import com.dotcms.jobs.business.job.JobResult;
import com.dotcms.jobs.business.job.JobView;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.cache.FieldsCache;
import com.dotcms.contenttype.exception.NotFoundInDbException;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.BadRequestException;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.rest.exception.ForbiddenException;
import com.dotcms.datagen.SiteDataGen;
import java.util.stream.Collectors;
import java.util.Arrays;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.beans.Permission;
import com.dotcms.datagen.UserDataGen;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobViewPaginatedResult;
import com.dotcms.jobs.business.util.JobUtil;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ResponseEntityJobStatusView;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.job.SSEMonitorUtil;
import com.dotcms.rest.exception.ValidationException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DoesNotExistException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Integration test suite for content import functionality.
 * Tests the ContentImportResource API endpoints for various scenarios.
 */
@EnableWeld
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ContentImportResourceIntegrationTest extends Junit5WeldBaseTest {

    private static User adminUser;
    private static HttpServletRequest request;
    private static HttpServletResponse response;
    private static Host defaultSite;
    private static ObjectMapper mapper;
    private static ContentImportResource importResource;
    private static Language defaultLanguage;

    private static final String IMPORT_QUEUE_NAME = "importContentlets";
    private static final String CMD_PUBLISH = Constants.PUBLISH;
    private static final String CMD_PREVIEW = Constants.PREVIEW;

    private static File csvFile;
    private static ContentType contentType;
    private static String fieldId;

    @Inject
    ContentImportHelper contentImportHelper;

    @Inject
    SSEMonitorUtil sseMonitorUtil;

    @BeforeAll
    static void setUp() throws Exception {
        IntegrationTestInitService.getInstance().init();

        adminUser = TestUserUtils.getAdminUser();
        defaultSite = APILocator.getHostAPI().findDefaultHost(adminUser, false);
        request = JobUtil.generateMockRequest(adminUser, defaultSite.getHostname());
        response = new MockHttpResponse();
        mapper = new ObjectMapper();

        defaultLanguage = APILocator.getLanguageAPI().getDefaultLanguage();
        contentType = TestDataUtils.getRichTextLikeContentType();
        fieldId = contentType.fields().get(0).id();
        assert fieldId != null;
        csvFile = createTestCsvFile();
    }

    @BeforeEach
    void prepare() {
        importResource = new ContentImportResource(contentImportHelper, sseMonitorUtil);
    }

    @AfterAll
    static void cleanup() {
        // Clean up the test file
        if (csvFile != null && csvFile.exists()) {
            csvFile.delete();
        }
        // Clean up the test content type
        ContentTypeDataGen.remove(contentType);
    }


    /**
     * Given scenario: call cancel Job with an invalid job id
     * Expected result: we should get a DoesNotExistException
     */
    @Test
    @Order(1)
    void test_import_content_cancel_non_existing_job(){
        assertThrows(DoesNotExistException.class, () -> importResource.cancelJob(request, response, "nonExisting" ));
    }

    /**
     * Given scenario: call get Active Job
     * Expected result: A JobPaginatedResult is returned
     */
    @Test
    @Order(2)
    void test_import_content_get_active_jobs() {
        // Call the activeJobs endpoint
        ResponseEntityView<JobViewPaginatedResult> result = importResource.activeJobs(request, response, 1, 20);
        validateJobPaginatedResult(result, 0);
    }

    /**
     * Scenario: Create a valid content import job and then list active jobs.
     * <p>
     * This test creates a content import job using valid parameters and then calls the
     * activeJobs endpoint to verify that the job is listed as active. The expected result
     * is that the active jobs count should be 1.
     * </p>
     *
     * @throws DotDataException if there is an error with dotCMS data operations
     * @throws IOException if there is an error with file operations
     */
    @Test
    @Order(3)
    void test_import_content_and_list_active_jobs() throws DotDataException, IOException {
        //Create valid import job
        ContentImportForm form = createContentImportForm(contentType.name(), String.valueOf(defaultLanguage.getId()), WORKFLOW_PUBLISH_ACTION_ID, List.of(fieldId));
        ContentImportParams params = createContentImportParams(csvFile, form);
        Response importContentResponse = importResource.importContent(request, response, params);

        // Call the activeJobs endpoint
        ResponseEntityView<JobViewPaginatedResult> result = importResource.activeJobs(request, response, 1, 20);

        // Validate result
        validateJobPaginatedResult(result, 1);
    }

    /**
     * Scenario: Retrieve the list of canceled jobs.
     * <p>
     * This test calls the canceledJobs endpoint to verify that there are no canceled jobs
     * initially. The expected result is that the count of canceled jobs should be 0.
     * </p>
     */
    @Test
    @Order(4)
    void test_content_import_get_cancel_jobs() {
        // Call the activeJobs endpoint
        ResponseEntityView<JobViewPaginatedResult> result = importResource.canceledJobs(request, response, 1, 20);
        validateJobPaginatedResult(result, 0);
    }

    /**
     * Scenario: Create a valid content import job, cancel it, and then list canceled jobs.
     * <p>
     * This test creates a content import job using valid parameters, retrieves the job ID
     * from the response, cancels the job, and then calls the canceledJobs endpoint to verify
     * that the canceled job is listed. The expected result is that the count of canceled jobs
     * should be 1.
     * </p>
     *
     * @throws DotDataException if there is an error with dotCMS data operations
     * @throws IOException if there is an error with file operations
     */
    @Test
    @Order(5)
    void test_import_content_then_cancel_then_list_canceled_jobs() throws DotDataException, IOException {
        //Create valid import job
        ContentImportForm form = createContentImportForm(contentType.name(), String.valueOf(defaultLanguage.getId()), WORKFLOW_PUBLISH_ACTION_ID, List.of(fieldId));
        ContentImportParams params = createContentImportParams(csvFile, form);
        Response importContentResponse = importResource.importContent(request, response, params);

        // Retrieve the job ID from the response entity
        Object importContentResponseEntity = importContentResponse.getEntity();
        assertNotNull(importContentResponseEntity, "Response entity should not be null");
        assertInstanceOf(ResponseEntityJobStatusView.class, importContentResponseEntity, "Entity should be of type ResponseEntityJobStatusView");
        @SuppressWarnings("unchecked")
        ResponseEntityJobStatusView responseEntityJobStatusView = (ResponseEntityJobStatusView) importContentResponseEntity;

        // Cancel the job
        importResource.cancelJob(request, response, responseEntityJobStatusView.getEntity().jobId());

        // Call the canceledJobs endpoint
        ResponseEntityView<JobViewPaginatedResult> result = importResource.canceledJobs(request, response, 1, 20);
        // Validate result
        validateJobPaginatedResult(result, 1);
    }

    /**
     * Scenario: Import content with all parameters being passed (csv file, content type, language, workflow action, and fields).
     * <p>
     * Expected: A new import job should be created successfully with all parameters properly set.
     *
     * @throws IOException if there's an error with file operations
     * @throws DotDataException if there's an error with dotCMS data operations
     */
    @Test
    public void test_import_content_with_valid_params() throws IOException, DotDataException {
        ContentImportForm form = createContentImportForm(contentType.name(), String.valueOf(defaultLanguage.getId()), WORKFLOW_PUBLISH_ACTION_ID, List.of(fieldId));
        ContentImportParams params = createContentImportParams(csvFile, form);

        Response importContentResponse = importResource.importContent(request, response, params);
        validateSuccessfulResponse(importContentResponse, contentType.name(), String.valueOf(defaultLanguage.getId()), List.of(fieldId), WORKFLOW_PUBLISH_ACTION_ID, CMD_PUBLISH);
    }


    /**
     * Scenario: Validate content Import with all parameters being passed (csv file, content type, language, workflow action, and fields).
     * <p>
     * Expected: A new validate content import job should be created successfully with all parameters properly set.
     *
     * @throws IOException if there's an error with file operations
     * @throws DotDataException if there's an error with dotCMS data operations
     */
    @Test
    public void test_import_content_validate_with_valid_params() throws IOException, DotDataException {
        ContentImportForm form = createContentImportForm(contentType.name(), String.valueOf(defaultLanguage.getId()), WORKFLOW_PUBLISH_ACTION_ID, List.of(fieldId));
        ContentImportParams params = createContentImportParams(csvFile, form);

        Response importContentResponse = importResource.validateContentImport(request, response, params);
        validateSuccessfulResponse(importContentResponse, contentType.name(), String.valueOf(defaultLanguage.getId()), List.of(fieldId), WORKFLOW_PUBLISH_ACTION_ID, CMD_PREVIEW);
    }

    /**
     * Scenario: Validate content import with all parameters using the language ISO code
     * <p>
     * Expected: A new import job should be created successfully with all parameters properly set.
     *
     * @throws IOException if there's an error with file operations
     * @throws DotDataException if there's an error with dotCMS data operations
     */
    @Test
    public void test_import_content_with_language_iso_code() throws IOException, DotDataException {
        ContentImportForm form = createContentImportForm(contentType.name(), defaultLanguage.getIsoCode(), WORKFLOW_PUBLISH_ACTION_ID, List.of(fieldId));
        ContentImportParams params = createContentImportParams(csvFile, form);

        Response importContentResponse = importResource.importContent(request, response, params);
        validateSuccessfulResponse(importContentResponse, contentType.name(), defaultLanguage.getIsoCode(), List.of(fieldId), WORKFLOW_PUBLISH_ACTION_ID, CMD_PUBLISH);
    }


    /**
     * Scenario: Import content with all parameters using the language ISO code
     * <p>
     * Expected: A new validate content import job should be created successfully with all parameters properly set.
     *
     * @throws IOException if there's an error with file operations
     * @throws DotDataException if there's an error with dotCMS data operations
     */
    @Test
    public void test_import_content_validate_with_language_iso_code() throws IOException, DotDataException {
        ContentImportForm form = createContentImportForm(contentType.name(), defaultLanguage.getIsoCode(), WORKFLOW_PUBLISH_ACTION_ID, List.of(fieldId));
        ContentImportParams params = createContentImportParams(csvFile, form);

        Response importContentResponse = importResource.validateContentImport(request, response, params);
        validateSuccessfulResponse(importContentResponse, contentType.name(), defaultLanguage.getIsoCode(), List.of(fieldId), WORKFLOW_PUBLISH_ACTION_ID, CMD_PREVIEW);
    }

    /**
     * Scenario: Attempt to import content without specifying language and fields parameters.
     * <p>
     * Expected: The import request should fail with BAD_REQUEST (400) status code.
     * A key identifying the different Language versions of the same content must be defined
     * when importing multilingual files
     *
     * @throws IOException if there's an error with file operations
     * @throws DotDataException if there's an error with dotCMS data operations
     */
    @Test
    public void test_import_content_without_language_and_field_params() throws IOException, DotDataException {
        ContentImportForm form = createContentImportForm(contentType.name(), null, WORKFLOW_PUBLISH_ACTION_ID, null);
        ContentImportParams params = createContentImportParams(csvFile, form);

        // Assert that the response status is BAD_REQUEST (400)
        assertBadRequestResponse(importResource.importContent(request, response, params));
    }


    /**
     * Scenario: Attempt to validate content import without specifying language and fields parameters.
     * <p>
     * Expected: The validate import request should fail with BAD_REQUEST (400) status code.
     * A key identifying the different Language versions of the same content must be defined
     * when importing multilingual files
     *
     * @throws IOException if there's an error with file operations
     * @throws DotDataException if there's an error with dotCMS data operations
     */
    @Test
    public void test_import_content_validate_without_language_and_field_params() throws IOException, DotDataException {
        ContentImportForm form = createContentImportForm(contentType.name(), null, WORKFLOW_PUBLISH_ACTION_ID, null);
        ContentImportParams params = createContentImportParams(csvFile, form);

        // Assert that the response status is BAD_REQUEST (400)
        assertBadRequestResponse(importResource.validateContentImport(request, response, params));
    }

    /**
     * Scenario: Attempt to import content specifying a non-existing language.
     * <p>
     * Expected: The import request should fail with BAD_REQUEST (400) status code.
     *
     * @throws IOException if there's an error with file operations
     * @throws DotDataException if there's an error with dotCMS data operations
     */
    @Test
    public void test_import_content_with_invalid_language() throws IOException, DotDataException {
        ContentImportForm form = createContentImportForm(contentType.name(), "12345", WORKFLOW_PUBLISH_ACTION_ID, null);
        ContentImportParams params = createContentImportParams(csvFile, form);

        assertBadRequestResponse(importResource.importContent(request, response, params));
    }

    /**
     * Scenario: Attempt to validate content import specifying a non-existing language.
     * <p>
     * Expected: The validate content import request should fail with BAD_REQUEST (400) status code.
     *
     * @throws IOException if there's an error with file operations
     * @throws DotDataException if there's an error with dotCMS data operations
     */
    @Test
    public void test_import_content_validate_with_invalid_language() throws IOException, DotDataException {
        ContentImportForm form = createContentImportForm(contentType.name(), "12345", WORKFLOW_PUBLISH_ACTION_ID, null);
        ContentImportParams params = createContentImportParams(csvFile, form);

        assertBadRequestResponse(importResource.validateContentImport(request, response, params));
    }

    /**
     * Scenario: Attempt to import content specifying a non-existing content-type.
     * <p>
     * Expected: The import request should fail with BAD_REQUEST (400) status code since the content type is invalid.
     *
     * @throws IOException if there's an error with file operations
     * @throws DotDataException if there's an error with dotCMS data operations
     */
    @Test
    public void test_import_content_with_invalid_content_type() throws IOException, DotDataException {
        ContentImportForm form = createContentImportForm("doesNotExist", "12345", WORKFLOW_PUBLISH_ACTION_ID, null);
        ContentImportParams params = createContentImportParams(csvFile, form);

        assertBadRequestResponse(importResource.importContent(request, response, params));
    }

    /**
     * Scenario: Attempt to import content specifying a non-existing workflow action.
     * <p>
     * Expected: The import request should fail with BAD_REQUEST (400) status code since the workflow action is invalid.
     *
     * @throws IOException if there's an error with file operations
     * @throws DotDataException if there's an error with dotCMS data operations
     */
    @Test
    public void test_import_content_with_invalid_workflow_action() throws IOException, DotDataException {
        ContentImportForm form = createContentImportForm(contentType.name(), "12345", "workflow-action-2", null);
        ContentImportParams params = createContentImportParams(csvFile, form);

        assertBadRequestResponse(importResource.importContent(request, response, params));
    }

    /**
     * Scenario: Attempt to import content specifying an invalid key field.
     * <p>
     * Expected: The import request should fail with BAD_REQUEST (400) status code since the key field is invalid.
     *
     * @throws IOException if there's an error with file operations
     * @throws DotDataException if there's an error with dotCMS data operations
     */
    @Test
    public void test_import_content_with_invalid_key_field() throws IOException, DotDataException {
        ContentImportForm form = createContentImportForm(contentType.name(), "12345", "workflow-action-2", List.of("doesNotExist"));
        ContentImportParams params = createContentImportParams(csvFile, form);

        assertBadRequestResponse(importResource.importContent(request, response, params));
    }

    /**
     * Scenario: Attempt to validate content import specifying a non-existing content-type.
     * <p>
     * Expected: The validate content import request should fail with BAD_REQUEST (400) status code since the content type is invalid.
     *
     * @throws IOException if there's an error with file operations
     * @throws DotDataException if there's an error with dotCMS data operations
     */
    @Test
    public void test_import_content_validate_with_invalid_content_type() throws IOException, DotDataException {
        ContentImportForm form = createContentImportForm("doesNotExist", "12345", WORKFLOW_PUBLISH_ACTION_ID, null);
        ContentImportParams params = createContentImportParams(csvFile, form);

        assertBadRequestResponse(importResource.validateContentImport(request, response, params));
    }

    /**
     * Scenario: Attempt to create an import form without specifying the required content type parameter.
     * <p>
     * Expected: A ValidationException should be thrown since content type is a required parameter
     * for content import operations.
     * A Content Type id or variable is required.
     *
     * @throws ValidationException when attempting to create a form without content type
     */
    @Test
    public void test_import_content_without_content_type_in_form() {
        assertThrows(ValidationException.class, () -> createContentImportForm(null, null, WORKFLOW_PUBLISH_ACTION_ID, null));
    }

    /**
     * Scenario: Attempt to create an import form without specifying the required workflow action parameter.
     * <p>
     * Expected: A ValidationException should be thrown since workflow action is a required parameter
     * for content import operations.
     *
     * @throws ValidationException when attempting to create a form without workflow action
     */
    @Test
    public void test_import_content_without_workflow_action_in_form() {
        assertThrows(ValidationException.class, () -> createContentImportForm(contentType.name(), null, null, null));
    }

    /**
     * Scenario: Attempt to import content with valid form data but without providing the required CSV file.
     * <p>
     * Expected: A ValidationException should be thrown since the file is a required parameter
     * for content import operations.
     *
     * @throws JsonProcessingException if there's an error during JSON serialization
     * @throws ValidationException when attempting to import content without setting the file
     */
    @Test
    public void test_import_content_missing_file() throws IOException, DotDataException {
        ContentImportForm form = createContentImportForm(contentType.name(), String.valueOf(defaultLanguage.getId()), WORKFLOW_PUBLISH_ACTION_ID, null);

        ContentImportParams params = new ContentImportParams();
        params.setJsonForm(mapper.writeValueAsString(form));

        assertBadRequestResponse(importResource.importContent(request, response, params));
    }


    /**
     * Scenario: Attempt to validate content import with valid form data but without providing the required CSV file.
     * <p>
     * Expected: A ValidationException should be thrown since the file is a required parameter
     * for content import operations.
     *
     * @throws JsonProcessingException if there's an error during JSON serialization
     * @throws ValidationException when attempting to import content without setting the file
     */
    @Test
    public void test_import_content_validate_missing_file() throws IOException, DotDataException {
        ContentImportForm form = createContentImportForm(contentType.name(), String.valueOf(defaultLanguage.getId()), WORKFLOW_PUBLISH_ACTION_ID, null);

        ContentImportParams params = new ContentImportParams();
        params.setJsonForm(mapper.writeValueAsString(form));

        assertBadRequestResponse(importResource.validateContentImport(request, response, params));
    }


    /**
     * Scenario: Attempt to validate content import with valid form data but providing a txt file.
     * <p>
     * Expected: A ValidationException should be thrown since the file must be a CSV file
     * for content import operations.
     *
     * @throws JsonProcessingException if there's an error during JSON serialization
     * @throws ValidationException when attempting to import content without setting the file
     */
    @Test
    public void test_import_content_validate_txt_file() throws IOException, DotDataException {
        File txtFile = null;
        try{
            ContentImportForm form = createContentImportForm(contentType.name(), "12345", WORKFLOW_PUBLISH_ACTION_ID, null);
            txtFile = File.createTempFile("test", ".txt");
            ContentImportParams params = createContentImportParams(txtFile, form);

            // Assert that the response status is BAD_REQUEST (400)
            assertBadRequestResponse(importResource.validateContentImport(request, response, params));
        }finally {
            if(txtFile != null && txtFile.exists()){
                txtFile.delete();
            }
        }

    }

    /**
     * Scenario: Attempt to import content with a valid CSV file but without providing the required form data.
     * <p>
     * Expected: A ValidationException should be thrown since form data is a required parameter
     * for content import operations.
     *
     * @throws IOException if there's an error during file operations
     * @throws ValidationException when attempting to import content without setting form data
     */
    @Test
    public void test_import_content_missing_form() throws IOException, DotDataException {
        ContentImportParams params = new ContentImportParams();
        params.setFileInputStream(new FileInputStream(csvFile));
        params.setContentDisposition(createContentDisposition(csvFile.getName()));

        assertBadRequestResponse(importResource.importContent(request, response, params));
    }


    /**
     * Scenario: Attempt to validate content import with a valid CSV file but without providing the required form data.
     * <p>
     * Expected: A ValidationException should be thrown since form data is a required parameter
     * for content import operations.
     *
     * @throws IOException if there's an error during file operations
     * @throws ValidationException when attempting to import content without setting form data
     */
    @Test
    public void test_import_content_validate_missing_form() throws IOException, DotDataException {
        ContentImportParams params = new ContentImportParams();
        params.setFileInputStream(new FileInputStream(csvFile));
        params.setContentDisposition(createContentDisposition(csvFile.getName()));

        assertBadRequestResponse(importResource.validateContentImport(request, response, params));
    }

    /**
     * Given scenario: a limited backend user has READ and EDIT on one new content type and only READ
     * on another, and asks for the content types they can import into, searching for both.
     * Expected result: the list holds the first type, not the read-only one and never Host, and is
     * sorted by name. Each item carries id, variable, name and base type (#37896, FR-004).
     */
    @Test
    void test_writable_content_types_only_lists_types_the_user_can_write() throws Exception {
        final String stamp = "ImportPerm" + System.currentTimeMillis();
        final ContentType writable = new ContentTypeDataGen()
                .name(stamp + "Writable").nextPersisted();
        final ContentType readOnly = new ContentTypeDataGen()
                .name(stamp + "ReadOnly").nextPersisted();
        try {
            final User limitedUser = new UserDataGen()
                    .roles(TestUserUtils.getBackendRole()).nextPersisted();
            addPermission(writable, limitedUser, PermissionAPI.PERMISSION_READ,
                    PermissionAPI.PERMISSION_EDIT);
            addPermission(readOnly, limitedUser, PermissionAPI.PERMISSION_READ);
            final HttpServletRequest limitedRequest =
                    JobUtil.generateMockRequest(limitedUser, defaultSite.getHostname());

            final List<ImportContentTypeView> types = importResource
                    .getWritableContentTypes(limitedRequest, response, stamp, null, null)
                    .getEntity();

            assertNotNull(types, "The list of writable content types should not be null");
            final List<String> variables = types.stream()
                    .map(ImportContentTypeView::variable).collect(Collectors.toList());
            assertTrue(variables.contains(writable.variable()),
                    "A type the user can edit should be listed");
            assertFalse(variables.contains(readOnly.variable()),
                    "A type the user can only read should not be listed");
            assertFalse(variables.stream().anyMatch(Host.HOST_VELOCITY_VAR_NAME::equalsIgnoreCase),
                    "Host should never be listed");

            final ImportContentTypeView item = types.stream()
                    .filter(type -> writable.variable().equals(type.variable()))
                    .findFirst().orElseThrow();
            assertEquals(writable.id(), item.id());
            assertEquals(writable.name(), item.name());
            assertEquals(writable.baseType().name(), item.baseType());

            final List<String> names = types.stream()
                    .map(ImportContentTypeView::name).collect(Collectors.toList());
            final List<String> sorted = names.stream()
                    .sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
            assertEquals(sorted, names, "Types should be sorted by name");
        } finally {
            ContentTypeDataGen.remove(writable);
            ContentTypeDataGen.remove(readOnly);
        }
    }

    /**
     * Given scenario: an admin searches the content types they can import into, first for the
     * suite's own content type and then for "host".
     * Expected result: the suite's type is found, and Host is never listed, not even when searched
     * for by name (#37896, FR-004).
     */
    @Test
    void test_writable_content_types_for_admin_excludes_host() throws Exception {
        final List<String> variables = importResource
                .getWritableContentTypes(request, response, contentType.variable(), null, null)
                .getEntity().stream()
                .map(ImportContentTypeView::variable).collect(Collectors.toList());
        assertTrue(variables.contains(contentType.variable()),
                "The admin should be able to import into the test content type");

        final List<String> hostSearch = importResource
                .getWritableContentTypes(request, response, "host", null, null)
                .getEntity().stream()
                .map(ImportContentTypeView::variable).collect(Collectors.toList());
        assertFalse(hostSearch.stream().anyMatch(Host.HOST_VELOCITY_VAR_NAME::equalsIgnoreCase),
                "Host should never be listed");
    }

    /**
     * Given scenario: three writable content types share a name prefix; the admin pages through
     * them two at a time, searching by that prefix.
     * Expected result: page 1 holds the first two by name and page 2 the third, and both carry the
     * page, the page size and the total of 3, so the shared content type selector can lazy load
     * them (#37896, FR-004).
     */
    @Test
    void test_writable_content_types_are_paged() throws Exception {
        final String stamp = "ImportPaged" + System.currentTimeMillis();
        final ContentType first = new ContentTypeDataGen().name(stamp + "A").nextPersisted();
        final ContentType second = new ContentTypeDataGen().name(stamp + "B").nextPersisted();
        final ContentType third = new ContentTypeDataGen().name(stamp + "C").nextPersisted();
        try {
            final ResponseEntityImportContentTypeListView pageOne =
                    importResource.getWritableContentTypes(request, response, stamp, 1, 2);
            assertEquals(List.of(first.variable(), second.variable()),
                    pageOne.getEntity().stream().map(ImportContentTypeView::variable)
                            .collect(Collectors.toList()),
                    "Page 1 should hold the first two types by name");
            assertEquals(1, pageOne.getPagination().getCurrentPage());
            assertEquals(2, pageOne.getPagination().getPerPage());
            assertEquals(3L, pageOne.getPagination().getTotalEntries());

            final ResponseEntityImportContentTypeListView pageTwo =
                    importResource.getWritableContentTypes(request, response, stamp, 2, 2);
            assertEquals(List.of(third.variable()),
                    pageTwo.getEntity().stream().map(ImportContentTypeView::variable)
                            .collect(Collectors.toList()),
                    "Page 2 should hold the third type");
            assertEquals(2, pageTwo.getPagination().getCurrentPage());
            assertEquals(3L, pageTwo.getPagination().getTotalEntries());
        } finally {
            ContentTypeDataGen.remove(first);
            ContentTypeDataGen.remove(second);
            ContentTypeDataGen.remove(third);
        }
    }

    /**
     * Given scenario: a writable content type whose name and variable differ, searched for in a
     * different letter case by part of its name, and then by part of its variable.
     * Expected result: both searches find it, so the selector's search box behaves like the
     * general content type search (#37896, FR-004).
     */
    @Test
    void test_writable_content_types_search_by_name_or_variable() throws Exception {
        final String stamp = String.valueOf(System.currentTimeMillis());
        final ContentType type = new ContentTypeDataGen()
                .name("Import Search Name " + stamp)
                .velocityVarName("importSearchVar" + stamp)
                .nextPersisted();
        try {
            final List<String> byName = importResource
                    .getWritableContentTypes(request, response, "SEARCH NAME " + stamp, null, null)
                    .getEntity().stream().map(ImportContentTypeView::variable)
                    .collect(Collectors.toList());
            assertEquals(List.of(type.variable()), byName, "A search by name should find it");

            final List<String> byVariable = importResource
                    .getWritableContentTypes(request, response, "searchvar" + stamp, null, null)
                    .getEntity().stream().map(ImportContentTypeView::variable)
                    .collect(Collectors.toList());
            assertEquals(List.of(type.variable()), byVariable,
                    "A search by variable should find it");
        } finally {
            ContentTypeDataGen.remove(type);
        }
    }

    /**
     * Given scenario: the content types are asked for without paging parameters.
     * Expected result: the first page of 40 is returned, as the general content type listing does
     * (#37896, FR-004).
     */
    @Test
    void test_writable_content_types_default_to_the_first_page_of_40() throws Exception {
        final ResponseEntityImportContentTypeListView types =
                importResource.getWritableContentTypes(request, response, null, null, null);

        assertEquals(1, types.getPagination().getCurrentPage());
        assertEquals(40, types.getPagination().getPerPage());
        assertTrue(types.getEntity().size() <= 40, "No more than one page should be returned");
    }

    /**
     * Given scenario: a page below 1, or a page size below 1, is asked for.
     * Expected result: the request is refused as a bad request (#37896, FR-004).
     */
    @Test
    void test_writable_content_types_refuse_invalid_paging() {
        assertThrows(BadRequestException.class,
                () -> importResource.getWritableContentTypes(request, response, null, 0, 10));
        assertThrows(BadRequestException.class,
                () -> importResource.getWritableContentTypes(request, response, null, 1, 0));
    }

    /**
     * Given scenario: a preview is requested with {@code site} set to another site's identifier.
     * Expected result: the job runs against that site, not the session's (#37896, FR-023).
     */
    @Test
    void test_validate_with_site_id_uses_that_site() throws Exception {
        final Host otherSite = new SiteDataGen().nextPersisted();
        final ContentImportForm form = createContentImportForm(contentType.name(),
                String.valueOf(defaultLanguage.getId()), WORKFLOW_PUBLISH_ACTION_ID,
                List.of(fieldId), otherSite.getIdentifier());

        final Response result = importResource.validateContentImport(request, response,
                createContentImportParams(csvFile, form));

        final Job job = jobOf(result);
        assertEquals(otherSite.getIdentifier(), job.parameters().get("siteIdentifier"));
        assertEquals(otherSite.getHostname(), job.parameters().get("siteName"));
    }

    /**
     * Given scenario: an import is requested with {@code site} set to another site's hostname.
     * Expected result: the hostname resolves, and the job runs against that site (#37896, FR-023).
     */
    @Test
    void test_import_with_site_hostname_uses_that_site() throws Exception {
        final Host otherSite = new SiteDataGen().nextPersisted();
        final ContentImportForm form = createContentImportForm(contentType.name(),
                String.valueOf(defaultLanguage.getId()), WORKFLOW_PUBLISH_ACTION_ID,
                List.of(fieldId), otherSite.getHostname());

        final Response result = importResource.importContent(request, response,
                createContentImportParams(csvFile, form));

        assertEquals(otherSite.getIdentifier(), jobOf(result).parameters().get("siteIdentifier"));
    }

    /**
     * Given scenario: {@code site} names a site that doesn't exist.
     * Expected result: the request is refused with 400 and no job runs (#37896, FR-023).
     */
    @Test
    void test_validate_with_unknown_site_is_refused() throws Exception {
        final ContentImportForm form = createContentImportForm(contentType.name(),
                String.valueOf(defaultLanguage.getId()), WORKFLOW_PUBLISH_ACTION_ID,
                List.of(fieldId), "no-such-site-" + System.currentTimeMillis() + ".example");

        assertBadRequestResponse(importResource.validateContentImport(request, response,
                createContentImportParams(csvFile, form)));
    }

    /**
     * Given scenario: a limited backend user names a site they have no permission to read.
     * Expected result: the request is refused as forbidden, which the REST layer answers with 403
     * (#37896, FR-023).
     */
    @Test
    void test_validate_with_unreadable_site_is_forbidden() throws Exception {
        final Host privateSite = new SiteDataGen().nextPersisted();
        final User limitedUser = new UserDataGen()
                .roles(TestUserUtils.getBackendRole()).nextPersisted();
        final HttpServletRequest limitedRequest =
                JobUtil.generateMockRequest(limitedUser, defaultSite.getHostname());
        final ContentImportForm form = createContentImportForm(contentType.name(),
                String.valueOf(defaultLanguage.getId()), WORKFLOW_PUBLISH_ACTION_ID,
                List.of(fieldId), privateSite.getIdentifier());

        assertThrows(ForbiddenException.class, () -> importResource.validateContentImport(
                limitedRequest, response, createContentImportParams(csvFile, form)));
    }

    /**
     * Given scenario: an import job is created from an uploaded CSV.
     * Expected result: the job records the CSV's original file name, so Content Drive can name the
     * import in its status toast after a reload (#37896).
     */
    @Test
    void test_import_job_records_the_file_name() throws Exception {
        final ContentImportForm form = createContentImportForm(contentType.name(),
                String.valueOf(defaultLanguage.getId()), WORKFLOW_PUBLISH_ACTION_ID,
                List.of(fieldId));

        final Response result = importResource.importContent(request, response,
                createContentImportParams(csvFile, form));

        assertEquals(csvFile.getName(), jobOf(result).parameters().get("fileName"));
    }

    /**
     * Given scenario: the sample CSV for the suite's content type is requested.
     * Expected result: 200 {@code text/csv} named {@code CSV_Template.csv}, whose body is what the
     * template generator writes for that type's fields — the same file Content Search offers
     * (#37896, FR-024).
     */
    @Test
    void test_sample_csv_is_the_content_search_template() throws Exception {
        final Response result =
                importResource.downloadSample(request, response, contentType.variable());

        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        assertTrue(String.valueOf(result.getMediaType()).startsWith("text/csv"));
        assertEquals("attachment; filename=\"CSV_Template.csv\"",
                result.getHeaderString("Content-Disposition"));

        final ByteArrayOutputStream body = new ByteArrayOutputStream();
        ((StreamingOutput) result.getEntity()).write(body);
        assertEquals(ContentImportTemplateGenerator.generate(
                        FieldsCache.getFieldsByStructureInode(contentType.inode())),
                body.toString(StandardCharsets.UTF_8));
    }

    /**
     * Given scenario: the sample CSV is requested with no content type.
     * Expected result: the request is refused as a bad request (400).
     */
    @Test
    void test_sample_csv_without_content_type_is_refused() {
        assertThrows(BadRequestException.class,
                () -> importResource.downloadSample(request, response, null));
    }

    /**
     * Given scenario: the sample CSV is requested for a content type that doesn't exist.
     * Expected result: not found (404).
     */
    @Test
    void test_sample_csv_for_unknown_type_is_not_found() {
        assertThrows(NotFoundInDbException.class, () -> importResource.downloadSample(request,
                response, "noSuchType" + System.currentTimeMillis()));
    }

    /**
     * Given scenario: a limited backend user asks for the sample of a type they can't read.
     * Expected result: refused (403), so the endpoint doesn't reveal a type's fields.
     */
    @Test
    void test_sample_csv_for_unreadable_type_is_forbidden() throws Exception {
        final ContentType privateType = new ContentTypeDataGen()
                .name("ImportPrivate" + System.currentTimeMillis()).nextPersisted();
        // Individual permissions for the admin only, so the type stops inheriting the site's
        // permissions and the limited user really can't read it
        APILocator.getPermissionAPI().save(new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                        privateType.getPermissionId(),
                        APILocator.getRoleAPI().loadRoleByKey(adminUser.getUserId()).getId(),
                        PermissionAPI.PERMISSION_READ, true),
                privateType, APILocator.systemUser(), false);
        try {
            final User limitedUser = new UserDataGen()
                    .roles(TestUserUtils.getBackendRole()).nextPersisted();
            final HttpServletRequest limitedRequest =
                    JobUtil.generateMockRequest(limitedUser, defaultSite.getHostname());

            assertThrows(DotSecurityException.class, () -> importResource.downloadSample(
                    limitedRequest, response, privateType.variable()));
        } finally {
            ContentTypeDataGen.remove(privateType);
        }
    }

    /**
     * Given scenario: a preview is requested for a CSV that has a header and no data rows.
     * Expected result: 400 with a failed job whose result carries an error message the Drive's
     * import dialog can show (#37896, US5).
     */
    @Test
    @SuppressWarnings("unchecked")
    void test_validate_empty_csv_explains_the_failure() throws Exception {
        final File headerOnly = File.createTempFile("header-only", ".csv");
        try {
            Files.write(headerOnly.toPath(), "title,body\n".getBytes());
            final ContentImportForm form = createContentImportForm(contentType.name(),
                    String.valueOf(defaultLanguage.getId()), WORKFLOW_PUBLISH_ACTION_ID,
                    List.of(fieldId));

            final Response result = importResource.validateContentImport(request, response,
                    createContentImportParams(headerOnly, form));

            assertBadRequestResponse(result);
            final JobView job = ((ResponseEntityView<JobView>) result.getEntity()).getEntity();
            final Map<String, Object> metadata =
                    job.result().flatMap(JobResult::metadata).orElseThrow();
            final List<Map<String, Object>> errors =
                    (List<Map<String, Object>>) metadata.get("error");
            assertFalse(errors.isEmpty(), "The failed job should explain why");
            assertFalse(String.valueOf(errors.get(0).get("message")).isBlank(),
                    "The first error should carry a message");
        } finally {
            headerOnly.delete();
        }
    }

    /**
     * Given scenario: a preview is requested through the synchronous preview endpoint.
     * Expected result: 200 with an already finished job — state SUCCESS, type PREVIEW and the
     * summary of the two CSV rows — answered in the same request, with no job left in the queue
     * (#37896: Content Search's preview is instantaneous, and the Drive's must be too).
     */
    @Test
    @SuppressWarnings("unchecked")
    void test_preview_runs_in_the_request() throws Exception {
        final int activeBefore = importResource.activeJobs(request, response, 1, 100)
                .getEntity().jobs().size();
        final ContentImportForm form = createContentImportForm(contentType.name(),
                String.valueOf(defaultLanguage.getId()), WORKFLOW_PUBLISH_ACTION_ID,
                List.of(fieldId));

        final Response result = importResource.previewContentImport(request, response,
                createContentImportParams(csvFile, form));

        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        final JobView job = ((ResponseEntityView<JobView>) result.getEntity()).getEntity();
        assertEquals(JobState.SUCCESS, job.state());
        final Map<String, Object> metadata =
                job.result().flatMap(JobResult::metadata).orElseThrow();
        assertEquals("PREVIEW", String.valueOf(metadata.get("type")));
        final Map<String, Object> summary = (Map<String, Object>)
                ((Map<String, Object>) metadata.get("data")).get("summary");
        assertEquals(2, ((Number) summary.get("created")).intValue(),
                "Both CSV rows would be created");
        assertEquals(activeBefore, importResource.activeJobs(request, response, 1, 100)
                .getEntity().jobs().size(), "A synchronous preview leaves no job in the queue");
    }

    /**
     * Given scenario: the synchronous preview is requested without a file.
     * Expected result: 400, as the job-based validate endpoint answers.
     */
    @Test
    void test_preview_without_file_is_refused() throws Exception {
        final ContentImportParams params = new ContentImportParams();
        params.setJsonForm(mapper.writeValueAsString(createContentImportForm(contentType.name(),
                String.valueOf(defaultLanguage.getId()), WORKFLOW_PUBLISH_ACTION_ID,
                List.of(fieldId))));

        assertBadRequestResponse(importResource.previewContentImport(request, response, params));
    }

    /**
     * Given scenario: the synchronous preview names a site that doesn't exist.
     * Expected result: 400, with the reason in the failed job's errors.
     */
    @Test
    void test_preview_with_unknown_site_is_refused() throws Exception {
        final ContentImportForm form = createContentImportForm(contentType.name(),
                String.valueOf(defaultLanguage.getId()), WORKFLOW_PUBLISH_ACTION_ID,
                List.of(fieldId), "no-such-site-" + System.currentTimeMillis() + ".example");

        assertBadRequestResponse(importResource.previewContentImport(request, response,
                createContentImportParams(csvFile, form)));
    }

    /**
     * Reads the job a successful create/validate response refers to.
     *
     * @param result the endpoint's response
     * @return the job
     */
    private Job jobOf(final Response result) throws DotDataException {
        assertEquals(Response.Status.OK.getStatusCode(), result.getStatus());
        final ResponseEntityJobStatusView view = (ResponseEntityJobStatusView) result.getEntity();
        return contentImportHelper.getJob(view.getEntity().jobId());
    }

    /**
     * Grants a limited user's individual role the given permissions on a permissionable.
     *
     * @param permissionable what the permissions apply to
     * @param limitedUser    the user whose personal role receives them
     * @param permissions    the permission bits to add together
     * @throws Exception if the permission can't be saved
     */
    private static void addPermission(final Permissionable permissionable, final User limitedUser,
            final int... permissions) throws Exception {
        final Permission permission = new Permission(PermissionAPI.INDIVIDUAL_PERMISSION_TYPE,
                permissionable.getPermissionId(),
                APILocator.getRoleAPI().loadRoleByKey(limitedUser.getUserId()).getId(),
                Arrays.stream(permissions).sum(), true);
        APILocator.getPermissionAPI().save(permission, permissionable, APILocator.systemUser(), false);
    }

    private void validateJobPaginatedResult(ResponseEntityView<JobViewPaginatedResult> result, long expectedTotalJobs) {
        assertNotNull(result, "Response should not be null");

        JobViewPaginatedResult entity = result.getEntity();
        assertNotNull(entity, "JobPaginatedResult should not be null");

        // Validate the properties of JobViewPaginatedResult
        assertEquals(1, entity.page(), "Current page should be 1");
        assertEquals(20, entity.pageSize(), "Page size should be 20");

        // Check that the total number of jobs is as expected (this can be adjusted based on your test setup)
        //assertTrue(expectedTotalJobs <= entity.jobs().size(), "Total number of jobs should match expected value");
        assertEquals(expectedTotalJobs, entity.jobs().size(), "Total number of jobs should match expected value");

        // Check that the jobs list is not null and has the expected number of jobs
        var jobs = entity.jobs();
        assertNotNull(jobs, "Jobs list should not be null");
        assertTrue(jobs.size() <= entity.pageSize(), "Number of jobs should not exceed page size");
    }

    /**
     * Validates the response and job parameters from a content import operation.
     * <p>
     * Performs the following validations:
     * - Response status is OK (200)
     * - Response entity is properly formatted
     * - Job exists in the queue
     * - All job parameters match expected values
     * - Optional fields are properly set when provided
     *
     * @param response The Response object from the import operation
     * @param expectedContentType The content type that should be set in the job
     * @param expectedLanguage The language ID that should be set in the job
     * @param expectedFields List of fields that should be included in the job, or null if no fields expected
     * @param expectedWorkflowActionId The workflow action ID that should be set in the job
     * @param expectedCommand The command that should be set in the job (usually 'publish')
     * @throws DotDataException if there's an error retrieving the job from the queue
     * @throws AssertionError if any validation fails
     */
    private void validateSuccessfulResponse(Response response, String expectedContentType, String expectedLanguage, List<String> expectedFields, String expectedWorkflowActionId, String expectedCommand) throws DotDataException {
        // Validate Response object
        assertNotNull(response, "Import response should not be null");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus(), "Response status should be OK");

        // Check and cast the entity safely
        Object entity = response.getEntity();
        assertNotNull(entity, "Response entity should not be null");
        assertInstanceOf(ResponseEntityJobStatusView.class, entity, "Entity should be of type ResponseEntityJobStatusView");

        @SuppressWarnings("unchecked")
        ResponseEntityJobStatusView responseEntityJobStatusView = (ResponseEntityJobStatusView) entity;

        // Validate response object and job ID existence
        assertNotNull(responseEntityJobStatusView, "ResponseEntityJobStatusView should not be null");
        assertNotNull(responseEntityJobStatusView.getEntity(), "JobStatusResponse should not be null");
        assertNotNull(responseEntityJobStatusView.getEntity().jobId(), "Job ID should not be null");
        assertNotNull(responseEntityJobStatusView.getEntity().statusUrl(), "Job Status URL should not be null");
        assertFalse(responseEntityJobStatusView.getEntity().jobId().isEmpty(), "Job ID should be a non-empty string");
        assertFalse(responseEntityJobStatusView.getEntity().statusUrl().isEmpty(), "Job Status URL should be a non-empty string");

        // Retrieve and validate job exists in the queue
        Job job = contentImportHelper.getJob(responseEntityJobStatusView

                .getEntity().jobId());
        assertNotNull(job, "Job should exist in queue");

        // Validate core import parameters
        assertEquals(expectedContentType, job.parameters().get("contentType"), "Job should contain correct content type");
        assertEquals(expectedLanguage, job.parameters().get("language"), "Job should contain correct language");
        assertEquals(expectedWorkflowActionId, job.parameters().get("workflowActionId"), "Job should contain correct workflow action");

        // Validate job configuration and metadata
        assertEquals(IMPORT_QUEUE_NAME, job.queueName(), "Job should be in the correct queue");
        assertEquals(expectedCommand, job.parameters().get("cmd").toString(), "Job command should be correct");
        assertEquals(defaultSite.getIdentifier(), job.parameters().get("siteIdentifier"), "Job should contain correct site identifier");
        assertEquals(adminUser.getUserId(), job.parameters().get("userId"), "Job should contain correct user ID");

        // Validate optional fields parameter
        if (expectedFields != null) {
            assertTrue(job.parameters().containsKey("fields"), "Job should contain fields");
            assertEquals(expectedFields, job.parameters().get("fields"), "Job should contain correct fields");
        } else {
            assertFalse(job.parameters().containsKey("fields"), "Job should not contain fields");
        }
    }

    /**
     * Creates a temporary CSV file for testing purposes.
     * The file contains two rows of test data with 'title' and 'body' columns.
     *
     * @return A temporary File object containing test CSV data
     * @throws IOException if there's an error creating or writing to the temporary file
     */
    private static File createTestCsvFile() throws IOException {
        String csv = "title,body\nTest Title 1,Test Body 1\nTest Title 2,Test Body 2\n";
        File csvFile = File.createTempFile("test", ".csv");
        Files.write(csvFile.toPath(), csv.getBytes());
        return csvFile;
    }

    /**
     * Creates a FormDataContentDisposition object for file upload testing.
     * Sets up the basic metadata required for a file upload including name and size.
     *
     * @param filename The name of the file to be included in the content disposition
     * @return A FormDataContentDisposition object configured for testing
     */
    private FormDataContentDisposition createContentDisposition(String filename) {
        return FormDataContentDisposition
                .name("file")
                .fileName(filename)
                .size(100L)
                .build();
    }

    /**
     * Creates a ContentImportParams object with all required parameters for content import.
     * Includes file input stream, content disposition, and JSON form data.
     *
     * @param file The CSV file to be imported
     * @param form The form containing import configuration parameters
     * @return A fully configured ContentImportParams object
     * @throws IOException if there's an error reading the file or serializing the form to JSON
     */
    private ContentImportParams createContentImportParams(File file, ContentImportForm form) throws IOException {
        ContentImportParams params = new ContentImportParams();
        params.setFileInputStream(new FileInputStream(file));
        params.setContentDisposition(createContentDisposition(file.getName()));
        params.setJsonForm(mapper.writeValueAsString(form));
        return params;
    }

    /**
     * Creates a ContentImportForm with the specified parameters for content import configuration.
     *
     * @param contentType The type of content to be imported
     * @param language The language ID for the imported content
     * @param workflowActionId The ID of the workflow action to be applied
     * @param fields List of fields to be included in the import
     * @return A ContentImportForm configured with the specified parameters
     * @throws ValidationException if required parameters (contentType or workflowActionId) are missing
     */
    private ContentImportForm createContentImportForm(String contentType, String language,
            String workflowActionId, List<String> fields) {
        return new ContentImportForm(contentType, language, workflowActionId, fields, null, null);
    }

    /**
     * Creates a ContentImportForm that names the site the import runs against (#37896).
     *
     * @param site the site's identifier or hostname
     */
    private ContentImportForm createContentImportForm(String contentType, String language,
            String workflowActionId, List<String> fields, String site) {
        return new ContentImportForm(contentType, language, workflowActionId, fields, null, null,
                site);
    }

    /**
     * Asserts that the given response has a status of BAD_REQUEST (400).
     *
     * <p>This method checks that the HTTP response status code is 400 (BAD_REQUEST).
     * It is commonly used in test cases where the expected response is an error due to invalid input or request.</p>
     *
     * @param importContentResponse the HTTP response to check
     * @throws AssertionError if the response status is not BAD_REQUEST
     */
    private void assertBadRequestResponse(Response importContentResponse) {
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), importContentResponse.getStatus(), "Expected BAD_REQUEST status");
    }
}
