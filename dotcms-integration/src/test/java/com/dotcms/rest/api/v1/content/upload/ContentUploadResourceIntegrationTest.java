package com.dotcms.rest.api.v1.content.upload;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.dotcms.JUnit4WeldRunner;
import com.dotcms.Junit5WeldBaseTest;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.datagen.TestUserUtils;
import com.dotcms.jobs.business.api.JobQueueManagerAPI;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.processor.impl.ImportContentletsProcessor;
import com.dotcms.jobs.business.util.JobUtil;
import com.dotcms.mock.response.MockHttpResponse;
import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.JobQueueManagerHelper;
import com.dotcms.rest.api.v1.contentImport.ContentImportForm;
import com.dotcms.rest.api.v1.contentImport.ContentImportHelper;
import com.dotcms.rest.api.v1.contentImport.ContentImportParams;
import com.dotcms.rest.api.v1.contentImport.ContentImportResource;
import com.dotcms.rest.exception.ValidationException;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liferay.portal.model.User;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Integration test suite for content import functionality.
 * Tests the ContentImportResource API endpoints for various scenarios.
 */
@ApplicationScoped
@EnableWeld
@RunWith(JUnit4WeldRunner.class)
public class ContentUploadResourceIntegrationTest extends Junit5WeldBaseTest {

    private static User adminUser;
    private static HttpServletRequest request;
    private static HttpServletResponse response;
    private static Host defaultSite;
    private static ObjectMapper mapper;
    private static ContentImportResource importResource;

    //TODO move to a common place
    private final static String IMPORT_QUEUE_NAME = "importContentlets";
    private final static String CMD_PUBLISH = "publish";
    private final static String CMD_PREVIEW = "preview";

    @BeforeAll
    static void setUp() throws Exception {
        IntegrationTestInitService.getInstance().init();

        adminUser = TestUserUtils.getAdminUser();
        defaultSite = APILocator.getHostAPI().findDefaultHost(adminUser, false);
        request = JobUtil.generateMockRequest(adminUser, defaultSite.getHostname());
        response = new MockHttpResponse();
        mapper = new ObjectMapper();

        JobQueueManagerHelper jobQueueManagerHelper = mock(JobQueueManagerHelper.class);
        JobQueueManagerAPI jobQueueManagerAPI = APILocator.getJobQueueManagerAPI();
        jobQueueManagerAPI.registerProcessor(IMPORT_QUEUE_NAME, ImportContentletsProcessor.class);

        ContentImportHelper helper = new ContentImportHelper(jobQueueManagerAPI, jobQueueManagerHelper);
        importResource = new ContentImportResource(helper);
    }

    /**
     * Given: A valid CSV file and all required import parameters
     * When: Importing content with valid content type, language, workflow action, and fields
     * Then: The import job should be created successfully with all parameters properly set
     */
    @Test
    public void test_import_content_with_valid_params() throws IOException, DotDataException {
        ContentType contentType = TestDataUtils.getRichTextLikeContentType();
        File csvFile = createTestCsvFile();

        ContentImportForm form = createContentImportForm(contentType.name(), "1", "workflow-action-id", List.of("title"));
        ContentImportParams params = createContentImportParams(csvFile, form);

        ResponseEntityView<String> importContentResponse = importResource.importContent(request, response, params);
        validateSuccessfulResponse(importContentResponse, contentType.name(), "1", List.of("title"), "workflow-action-id", CMD_PUBLISH);
    }

    /**
     * Given: A valid CSV file with only required parameters
     * When: Importing content without optional parameters (language and fields)
     * Then: The import job should be created successfully with only required parameters set
     */
    @Test
    public void test_import_content_without_optional_params() throws IOException, DotDataException {
        ContentType contentType = TestDataUtils.getRichTextLikeContentType();
        File csvFile = createTestCsvFile();

        ContentImportForm form = createContentImportForm(contentType.name(), null, "workflow-action-id-2", null);
        ContentImportParams params = createContentImportParams(csvFile, form);

        ResponseEntityView<String> importContentResponse = importResource.importContent(request, response, params);
        validateSuccessfulResponse(importContentResponse, contentType.name(), null, null, "workflow-action-id-2", CMD_PUBLISH);
    }

    /**
     * Given: A valid CSV file but missing content type in form
     * When: Attempting to import content without specifying content type
     * Then: A ValidationException should be thrown
     */
    @Test
    public void test_import_content_without_content_type_in_form() throws IOException {
        File csvFile = createTestCsvFile();
        ContentImportForm form = createContentImportForm(null, null, "workflow-action-id", null);
        ContentImportParams params = createContentImportParams(csvFile, form);

        assertThrows(ValidationException.class, () -> importResource.importContent(request, response, params));
    }

    /**
     * Given: A valid CSV file but missing workflow action in form
     * When: Attempting to import content without specifying workflow action
     * Then: A ValidationException should be thrown
     */
    @Test
    public void test_import_content_without_workflow_action_in_form() throws IOException {
        ContentType contentType = TestDataUtils.getRichTextLikeContentType();
        File csvFile = createTestCsvFile();

        ContentImportForm form = createContentImportForm(contentType.name(), null, null, null);
        ContentImportParams params = createContentImportParams(csvFile, form);

        assertThrows(ValidationException.class, () -> importResource.importContent(request, response, params));
    }

    /**
     * Given: Valid form data but no CSV file
     * When: Attempting to import content without providing a file
     * Then: A ValidationException should be thrown
     */
    @Test
    public void test_import_content_missing_file() throws Exception {
        ContentType contentType = TestDataUtils.getRichTextLikeContentType();
        ContentImportForm form = createContentImportForm(contentType.name(), "1", "workflow-action-id", null);

        ContentImportParams params = new ContentImportParams();
        params.setJsonForm(mapper.writeValueAsString(form));

        assertThrows(ValidationException.class, () -> importResource.importContent(request, response, params));
    }

    /**
     * Given: A valid CSV file but no form data
     * When: Attempting to import content without providing form data
     * Then: A ValidationException should be thrown
     */
    @Test
    public void test_import_content_missing_form() throws Exception {
        File csvFile = createTestCsvFile();

        ContentImportParams params = new ContentImportParams();
        params.setFileInputStream(new FileInputStream(csvFile));
        params.setContentDisposition(createContentDisposition(csvFile.getName()));

        assertThrows(ValidationException.class, () -> importResource.importContent(request, response, params));
    }

    /**
     * Helper method to validate successful import response.
     * Given: A response from a successful content import
     * When: Validating the job parameters
     * Then: All expected parameters should match the provided values
     */
    private static void validateSuccessfulResponse(ResponseEntityView<String> response, String expectedContentType, String expectedLanguage, List<String> expectedFields, String expectedWorkflowActionId, String expectedCommand) throws DotDataException {
        // Validate response object and job ID existence
        assertNotNull(response, "Response should not be null");
        assertNotNull(response.getEntity(), "Job ID should not be null");
        assertFalse(response.getEntity().isEmpty(), "Job ID should be a non-empty string");

        // Retrieve and validate job exists in the queue
        Job job = APILocator.getJobQueueManagerAPI().getJob(response.getEntity());
        assertNotNull(job, "Job should exist in queue");

        // Validate core import parameters
        assertEquals(expectedContentType, job.parameters().get("contentType"), "Job should contain correct content type");
        assertEquals(expectedLanguage, job.parameters().get("language"), "Job should contain correct language");
        assertEquals(expectedWorkflowActionId, job.parameters().get("workflowActionId"), "Job should contain correct workflow action");

        // Validate job configuration and metadata
        assertEquals(IMPORT_QUEUE_NAME, job.queueName(), "Job should be in the correct queue");
        assertEquals(expectedCommand, job.parameters().get("cmd").toString(), "Job command should be 'publish'");
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

    //TODO move to a common place
    private static File createTestCsvFile() throws IOException {
        String csv = "title,body\nTest Title 1,Test Body 1\nTest Title 2,Test Body 2\n";
        File csvFile = File.createTempFile("test", ".csv");
        Files.write(csvFile.toPath(), csv.getBytes());
        return csvFile;
    }

    private static FormDataContentDisposition createContentDisposition(String filename) {
        return FormDataContentDisposition
                .name("file")
                .fileName(filename)
                .size(100L)
                .build();
    }

    private static ContentImportParams createContentImportParams(File file, ContentImportForm form) throws IOException {
        ContentImportParams params = new ContentImportParams();
        params.setFileInputStream(new FileInputStream(file));
        params.setContentDisposition(createContentDisposition(file.getName()));
        params.setJsonForm(mapper.writeValueAsString(form));
        return params;
    }

    private static ContentImportForm createContentImportForm(String contentType, String language, String workflowActionId, List<String> fields) {
        return new ContentImportForm(contentType, language, workflowActionId, fields);
    }
}
