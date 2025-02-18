package com.dotcms.jobs.business.processor.impl;

import static com.dotmarketing.portlets.workflows.business.SystemWorkflowConstants.WORKFLOW_PUBLISH_ACTION_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.TestDataUtils;
import com.dotcms.jobs.business.error.JobProcessingException;
import com.dotcms.jobs.business.error.JobValidationException;
import com.dotcms.jobs.business.job.Job;
import com.dotcms.jobs.business.job.JobState;
import com.dotcms.jobs.business.processor.DefaultProgressTracker;
import com.dotcms.jobs.business.util.JobUtil;
import com.dotcms.rest.api.v1.temp.DotTempFile;
import com.dotcms.rest.api.v1.temp.TempFileAPI;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.jboss.weld.junit5.EnableWeld;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
/**
 * Integration tests for the {@link ImportContentletsProcessor} class. These tests verify the
 * functionality of content import operations in a real database environment. The tests cover both
 * preview and publish modes, testing the complete workflow of content import including content type
 * creation, CSV file processing, and content verification.
 *
 * <p>The test suite creates temporary content types and files for testing,
 * and includes cleanup operations to maintain database integrity.
 */
@EnableWeld
public class ImportContentletsProcessorIntegrationTest extends com.dotcms.Junit5WeldBaseTest {

    private static Host defaultSite;
    private static User systemUser;
    private static HttpServletRequest request;

    /**
     * Sets up the test environment before all tests are run. This method:
     * <ul>
     *   <li>Initializes the dotCMS test environment</li>
     *   <li>Retrieves the system user for test operations</li>
     *   <li>Sets up the default site</li>
     *   <li>Creates a mock HTTP request for the import process</li>
     * </ul>
     *
     * @throws Exception if there's an error during setup
     */
    @BeforeAll
    static void setUp() throws Exception {

        // Initialize the test environment
        IntegrationTestInitService.getInstance().init();

        // Get system user
        systemUser = APILocator.getUserAPI().getSystemUser();

        // Get the default site
        defaultSite = APILocator.getHostAPI().findDefaultHost(systemUser, false);

        // Create a mock request
        request = JobUtil.generateMockRequest(systemUser, defaultSite.getHostname());
    }

    /**
     * Tests the preview mode of the content import process. This test:
     * <ul>
     *   <li>Creates a test content type</li>
     *   <li>Generates a test CSV file with sample content</li>
     *   <li>Processes the import in preview mode</li>
     *   <li>Verifies the preview results and metadata</li>
     *   <li>Verifies there is no content creation in the database</li>
     * </ul>
     *
     * <p>The test ensures that preview mode properly validates the content
     * without actually creating it in the system using the content type variable instead of the ID.
     *
     * @throws Exception if there's an error during the test execution
     */
    @Test
    void test_process_preview_using_content_type_variable() throws Exception {

        ContentType testContentType = null;

        try {
            // Initialize processor
            final var processor = new ImportContentletsProcessor();

            // Create test content type
            testContentType = createTestContentType();

            // Create test CSV file
            File csvFile = createTestCsvFile();

            // Create test job
            final var testJob = createTestJob(
                    csvFile, "preview", "1", testContentType.variable(),
                    WORKFLOW_PUBLISH_ACTION_ID
            );

            // Process the job in preview mode
            processor.process(testJob);

            // Verify preview results
            Map<String, Object> metadata = processor.getResultMetadata(testJob);
            assertNotNull(metadata, "Preview metadata should not be null");
            assertNotNull(metadata.get("error"), "Preview metadata errors should not be null");
            assertEquals(0, ((ArrayList) metadata.get("error")).size(),
                    "Preview metadata errors should be empty");

            // Verify no content was created
            final var importedContent = findImportedContent(testContentType.id());
            assertNotNull(importedContent, "Imported content should not be null");
            assertEquals(0, importedContent.size(), "Imported content should have no items");

        } finally {
            if (testContentType != null) {
                // Clean up test content type
                APILocator.getContentTypeAPI(systemUser).delete(testContentType);
            }
        }
    }

    /**
     * Scenario: Test the preview mode of the content import process with an invalid content type.
     * <p>
     * Expected: A JobValidationException should be thrown.
     *
     * @throws Exception if there's an error during the test execution
     */
    @Test
    void test_process_preview_invalid_content_type_variable() throws Exception {

        // Initialize processor
        final var processor = new ImportContentletsProcessor();

        // Create test CSV file
        File csvFile = createTestCsvFile();

        // Create test job
        final var testJob = createTestJob(
                csvFile, "preview", "1", "doesNotExist",
                WORKFLOW_PUBLISH_ACTION_ID
        );

        try {
            // Process the job in preview mode
            processor.validate(testJob.parameters());
            Assertions.fail("A JobValidationException should have been thrown here.");
        } catch (Exception e) {
            Assertions.assertInstanceOf(JobValidationException.class, e);
        }
    }

    /**
     * Tests the preview mode of the content import process. This test:
     * <ul>
     *   <li>Creates a test content type</li>
     *   <li>Generates a test CSV file with sample content</li>
     *   <li>Processes the import in preview mode</li>
     *   <li>Verifies the preview results and metadata</li>
     *   <li>Verifies there is no content creation in the database</li>
     * </ul>
     *
     * <p>The test ensures that preview mode properly validates the content
     * without actually creating it in the system using the language ISO code instead of the ID.
     *
     * @throws Exception if there's an error during the test execution
     */
    @Test
    void test_process_preview_using_language_iso_code() throws Exception {

        ContentType testContentType = null;

        try {
            // Initialize processor
            final var processor = new ImportContentletsProcessor();

            // Create test content type
            testContentType = createTestContentType();

            // Create test CSV file
            File csvFile = createTestCsvFile();

            // Create test job
            final var testJob = createTestJob(
                    csvFile, "preview", "en-us", testContentType.variable(),
                    WORKFLOW_PUBLISH_ACTION_ID
            );

            // Process the job in preview mode
            processor.process(testJob);

            // Verify preview results
            Map<String, Object> metadata = processor.getResultMetadata(testJob);
            assertNotNull(metadata, "Preview metadata should not be null");
            assertNotNull(metadata.get("error"), "Preview metadata errors should not be null");
            assertEquals(0, ((ArrayList) metadata.get("error")).size(),
                    "Preview metadata errors should be empty");

            // Verify no content was created
            final var importedContent = findImportedContent(testContentType.id());
            assertNotNull(importedContent, "Imported content should not be null");
            assertEquals(0, importedContent.size(), "Imported content should have no items");

        } finally {
            if (testContentType != null) {
                // Clean up test content type
                APILocator.getContentTypeAPI(systemUser).delete(testContentType);
            }
        }
    }

    /**
     * Scenario: Test the preview mode of the content import process with an invalid language.
     * <p>
     * Expected: A JobValidationException should be thrown.
     *
     * @throws Exception if there's an error during the test execution
     */
    @Test
    void test_process_preview_invalid_language() throws Exception {

        // Initialize processor
        final var processor = new ImportContentletsProcessor();

        // Create test CSV file
        File csvFile = createTestCsvFile();

        // Create test job
        final var testJob = createTestJob(
                csvFile, "preview", "12345", "doesNotExist",
                WORKFLOW_PUBLISH_ACTION_ID
        );

        try {
            processor.validate(testJob.parameters());
            Assertions.fail("A JobValidationException should have been thrown here.");
        } catch (Exception e) {
            Assertions.assertInstanceOf(JobValidationException.class, e);
        }
    }


    /**
     * Scenario: Test the preview mode of the content import process with an invalid workflow action.
     * <p>
     * Expected: A JobValidationException should be thrown.
     *
     * @throws Exception if there's an error during the test execution
     */
    @Test
    void test_process_preview_invalid_workflow_action() throws Exception {
        ContentType testContentType = null;

        try {
            // Initialize processor
            final var processor = new ImportContentletsProcessor();

            // Create test content type
            testContentType = createTestContentType();

            // Create test CSV file
            File csvFile = createTestCsvFile();

            // Create test job
            final var testJob = createTestJob(
                    csvFile, "preview", "en-us", testContentType.variable(),
                    "doesNotExist"
            );

            // Process the job in preview mode
            assertThrows(JobValidationException.class, ()-> processor.validate((testJob.parameters())));
        } finally {
            if (testContentType != null) {
                // Clean up test content type
                APILocator.getContentTypeAPI(systemUser).delete(testContentType);
            }
        }
    }


    /**
     * Scenario: Test the validating of the content import process with an invalid key field.
     * <p>
     * Expected: A JobValidationException should be thrown.
     *
     * @throws Exception if there's an error during the test execution
     */
    @Test
    void test_process_validate_invalid_key_fields() throws Exception {

        ContentType testContentType = null;

        try {
            // Initialize processor
            final var processor = new ImportContentletsProcessor();

            // Create test content type
            testContentType = createTestContentType();

            // Create test CSV file
            File csvFile = createTestCsvFile();

            // Create test job
            final var testJob = createTestJob(
                    csvFile, "preview", "en-us", testContentType.variable(),
                    WORKFLOW_PUBLISH_ACTION_ID, List.of("doesNotExist")
            );

            assertThrows(JobValidationException.class, ()-> processor.validate((testJob.parameters())));

        } finally {
            if (testContentType != null) {
                // Clean up test content type
                APILocator.getContentTypeAPI(systemUser).delete(testContentType);
            }
        }
    }

    /**
     * Scenario: Test the validating of the content import process with a list of fields that includes
     * an invalid key field.
     * <p>
     * Expected: A JobValidationException should be thrown.
     *
     * @throws Exception if there's an error during the test execution
     */
    @Test
    void test_process_validate_invalid_key_fields2() throws Exception {

        ContentType testContentType = null;

        try {
            // Initialize processor
            final var processor = new ImportContentletsProcessor();

            // Create test content type
            testContentType = createTestContentType();

            // Prepare test content type fields to use in the job
            Field titleField = null;
            Field bodyField = null;
            for (Field field : testContentType.fields()) {
                if (field.variable().equals("title")) {
                    titleField = field;
                } else if (field.variable().equals("body")) {
                    bodyField = field;
                }
            }
            assertNotNull(titleField);
            assertNotNull(bodyField);

            // Create test CSV file
            File csvFile = createTestCsvFile();

            // Create test job
            final var testJob = createTestJob(
                    csvFile, "preview", "en-us", testContentType.variable(),
                    WORKFLOW_PUBLISH_ACTION_ID,
                    List.of("doesNotExist", titleField.variable(), bodyField.variable())
            );

            assertThrows(JobValidationException.class,
                    () -> processor.validate((testJob.parameters())));

        } finally {
            if (testContentType != null) {
                // Clean up test content type
                APILocator.getContentTypeAPI(systemUser).delete(testContentType);
            }
        }
    }

    /**
     * Scenario: Test the validating of the content import process with a list of fields that includes
     * only variable names instead of field IDs.
     * <p>
     * Expected: A JobValidationException should be thrown.
     *
     * @throws Exception if there's an error during the test execution
     */
    @Test
    void test_process_validate_key_fields_with_variable_names() throws Exception {

        ContentType testContentType = null;

        try {
            // Initialize processor
            final var processor = new ImportContentletsProcessor();

            // Create test content type
            testContentType = createTestContentType();

            // Prepare test content type fields to use in the job
            Field titleField = null;
            Field bodyField = null;
            for (Field field : testContentType.fields()) {
                if (field.variable().equals("title")) {
                    titleField = field;
                } else if (field.variable().equals("body")) {
                    bodyField = field;
                }
            }
            assertNotNull(titleField);
            assertNotNull(bodyField);

            // Create test CSV file
            File csvFile = createTestCsvFile();

            // Create test job
            final var testJob = createTestJob(
                    csvFile, "preview", "en-us", testContentType.variable(),
                    WORKFLOW_PUBLISH_ACTION_ID,
                    List.of(titleField.variable(), bodyField.variable())
            );

            // Validate the test job, no exception should be thrown
            processor.validate(testJob.parameters());
        } finally {
            if (testContentType != null) {
                // Clean up test content type
                APILocator.getContentTypeAPI(systemUser).delete(testContentType);
            }
        }
    }

    /**
     * Scenario: Test the validating of the content import process with a list of fields that includes
     * variable names and IDs.
     * <p>
     * Expected: A JobValidationException should be thrown.
     *
     * @throws Exception if there's an error during the test execution
     */
    @Test
    void test_process_validate_key_fields_with_variable_names2() throws Exception {

        ContentType testContentType = null;

        try {
            // Initialize processor
            final var processor = new ImportContentletsProcessor();

            // Create test content type
            testContentType = createTestContentType();

            // Prepare test content type fields to use in the job
            Field titleField = null;
            Field bodyField = null;
            for (Field field : testContentType.fields()) {
                if (field.variable().equals("title")) {
                    titleField = field;
                } else if (field.variable().equals("body")) {
                    bodyField = field;
                }
            }
            assertNotNull(titleField);
            assertNotNull(bodyField);

            // Create test CSV file
            File csvFile = createTestCsvFile();

            // Create test job
            final var testJob = createTestJob(
                    csvFile, "preview", "en-us", testContentType.variable(),
                    WORKFLOW_PUBLISH_ACTION_ID,
                    List.of(titleField.variable(), bodyField.id())
            );

            // Validate the test job, no exception should be thrown
            processor.validate(testJob.parameters());
        } finally {
            if (testContentType != null) {
                // Clean up test content type
                APILocator.getContentTypeAPI(systemUser).delete(testContentType);
            }
        }
    }

    /**
     * Scenario: Test the validating of the content import process with a list of fields using field
     * IDs.
     * <p>
     * Expected: A JobValidationException should be thrown.
     *
     * @throws Exception if there's an error during the test execution
     */
    @Test
    void test_process_validate_key_fields_with_field_ids() throws Exception {

        ContentType testContentType = null;

        try {
            // Initialize processor
            final var processor = new ImportContentletsProcessor();

            // Create test content type
            testContentType = createTestContentType();

            // Prepare test content type fields to use in the job
            Field titleField = null;
            Field bodyField = null;
            for (Field field : testContentType.fields()) {
                if (field.variable().equals("title")) {
                    titleField = field;
                } else if (field.variable().equals("body")) {
                    bodyField = field;
                }
            }
            assertNotNull(titleField);
            assertNotNull(bodyField);

            // Create test CSV file
            File csvFile = createTestCsvFile();

            // Create test job
            final var testJob = createTestJob(
                    csvFile, "preview", "en-us", testContentType.variable(),
                    WORKFLOW_PUBLISH_ACTION_ID,
                    List.of(titleField.id(), bodyField.id())
            );

            // Validate the test job, no exception should be thrown
            processor.validate(testJob.parameters());
        } finally {
            if (testContentType != null) {
                // Clean up test content type
                APILocator.getContentTypeAPI(systemUser).delete(testContentType);
            }
        }
    }

    /**
     * Tests the preview mode of the content import process with an invalid key field.
     *
     * <p>The test ensures that preview mode properly validates the key fields and throws an
     * exception if the fields are not valid.
     *
     * @throws Exception if there's an error during the test execution
     */
    @Test
    void test_process_preview_with_invalid_key_fields() throws Exception {

        ContentType testContentType = null;

        try {
            // Initialize processor
            final var processor = new ImportContentletsProcessor();

            // Create test content type
            testContentType = createTestContentType();

            // Create test CSV file
            File csvFile = createTestCsvFile();

            // Create test job
            final var testJob = createTestJob(
                    csvFile, "preview", "1", testContentType.id(),
                    WORKFLOW_PUBLISH_ACTION_ID, List.of("doesNotExist")
            );

            // Process the job in preview mode, an exception should be thrown as the key field
            // is invalid
            assertThrows(JobProcessingException.class, () -> processor.process(testJob));

        } finally {
            if (testContentType != null) {
                // Clean up test content type
                APILocator.getContentTypeAPI(systemUser).delete(testContentType);
            }
        }
    }

    /**
     * Tests the preview mode of the content import process with invalid key fields.
     * <p>The test contains in the list of fields an invalid key field, a valid key field by ID
     * and a valid key field by variable name.
     *
     * <p>The test ensures that preview mode properly validates the key fields and throws an
     * exception if the fields are not valid.
     *
     * @throws Exception if there's an error during the test execution
     */
    @Test
    void test_process_preview_with_invalid_key_fields2() throws Exception {

        ContentType testContentType = null;

        try {
            // Initialize processor
            final var processor = new ImportContentletsProcessor();

            // Create test content type
            testContentType = createTestContentType();

            // Prepare test content type fields to use in the job
            Field titleField = null;
            Field bodyField = null;
            for (Field field : testContentType.fields()) {
                if (field.variable().equals("title")) {
                    titleField = field;
                } else if (field.variable().equals("body")) {
                    bodyField = field;
                }
            }
            assertNotNull(titleField);
            assertNotNull(bodyField);

            // Create test CSV file
            File csvFile = createTestCsvFile();

            // Create test job
            final var testJob = createTestJob(
                    csvFile, "preview", "1", testContentType.id(),
                    WORKFLOW_PUBLISH_ACTION_ID,
                    List.of("doesNotExist", titleField.id(), bodyField.variable())
            );

            // Process the job in preview mode, an exception should be thrown as a key field
            // is invalid
            assertThrows(JobProcessingException.class, () -> processor.process(testJob));

        } finally {
            if (testContentType != null) {
                // Clean up test content type
                APILocator.getContentTypeAPI(systemUser).delete(testContentType);
            }
        }
    }

    /**
     * Tests the preview mode of the content import process. This test:
     * <ul>
     *   <li>Creates a test content type</li>
     *   <li>Generates a test CSV file with sample content</li>
     *   <li>Processes the import in preview mode using key fields with a mix of field IDs and
     *   variable names</li>
     *   <li>Verifies the preview results and metadata</li>
     *   <li>Verifies there is no content creation in the database</li>
     * </ul>
     *
     * @throws Exception if there's an error during the test execution
     */
    @Test
    void test_process_preview_with_key_fields() throws Exception {

        ContentType testContentType = null;

        try {
            // Initialize processor
            final var processor = new ImportContentletsProcessor();

            // Create test content type
            testContentType = createTestContentType();

            // Prepare test content type fields to use in the job
            Field titleField = null;
            Field bodyField = null;
            for (Field field : testContentType.fields()) {
                if (field.variable().equals("title")) {
                    titleField = field;
                } else if (field.variable().equals("body")) {
                    bodyField = field;
                }
            }
            assertNotNull(titleField);
            assertNotNull(bodyField);

            // Create test CSV file
            File csvFile = createTestCsvFile();

            // Create test job
            final var testJob = createTestJob(
                    csvFile, "preview", "1", testContentType.id(),
                    WORKFLOW_PUBLISH_ACTION_ID,
                    List.of(titleField.id(), bodyField.variable())
            );

            // Process the job in preview mode
            processor.process(testJob);

            // Verify preview results
            Map<String, Object> metadata = processor.getResultMetadata(testJob);
            assertNotNull(metadata, "Preview metadata should not be null");
            assertNotNull(metadata.get("error"), "Preview metadata errors should not be null");
            assertEquals(0, ((ArrayList) metadata.get("error")).size(),
                    "Preview metadata errors should be empty");

            // Verify no content was created
            final var importedContent = findImportedContent(testContentType.id());
            assertNotNull(importedContent, "Imported content should not be null");
            assertEquals(0, importedContent.size(), "Imported content should have no items");

        } finally {
            if (testContentType != null) {
                // Clean up test content type
                APILocator.getContentTypeAPI(systemUser).delete(testContentType);
            }
        }
    }

    /**
     * Tests the preview mode of the content import process. This test:
     * <ul>
     *   <li>Creates a test content type</li>
     *   <li>Generates a test CSV file with sample content</li>
     *   <li>Processes the import in preview mode using key fields with just variable names</li>
     *   <li>Verifies the preview results and metadata</li>
     *   <li>Verifies there is no content creation in the database</li>
     * </ul>
     *
     * @throws Exception if there's an error during the test execution
     */
    @Test
    void test_process_preview_with_key_fields2() throws Exception {

        ContentType testContentType = null;

        try {
            // Initialize processor
            final var processor = new ImportContentletsProcessor();

            // Create test content type
            testContentType = createTestContentType();

            // Prepare test content type fields to use in the job
            Field titleField = null;
            Field bodyField = null;
            for (Field field : testContentType.fields()) {
                if (field.variable().equals("title")) {
                    titleField = field;
                } else if (field.variable().equals("body")) {
                    bodyField = field;
                }
            }
            assertNotNull(titleField);
            assertNotNull(bodyField);

            // Create test CSV file
            File csvFile = createTestCsvFile();

            // Create test job
            final var testJob = createTestJob(
                    csvFile, "preview", "1", testContentType.id(),
                    WORKFLOW_PUBLISH_ACTION_ID,
                    List.of(titleField.variable(), bodyField.variable())
            );

            // Process the job in preview mode
            processor.process(testJob);

            // Verify preview results
            Map<String, Object> metadata = processor.getResultMetadata(testJob);
            assertNotNull(metadata, "Preview metadata should not be null");
            assertNotNull(metadata.get("error"), "Preview metadata errors should not be null");
            assertEquals(0, ((ArrayList) metadata.get("error")).size(),
                    "Preview metadata errors should be empty");

            // Verify no content was created
            final var importedContent = findImportedContent(testContentType.id());
            assertNotNull(importedContent, "Imported content should not be null");
            assertEquals(0, importedContent.size(), "Imported content should have no items");

        } finally {
            if (testContentType != null) {
                // Clean up test content type
                APILocator.getContentTypeAPI(systemUser).delete(testContentType);
            }
        }
    }

    /**
     * Tests the preview mode of the content import process. This test:
     * <ul>
     *   <li>Creates a test content type</li>
     *   <li>Generates a test CSV file with sample content</li>
     *   <li>Processes the import in preview mode using key fields with just field ids</li>
     *   <li>Verifies the preview results and metadata</li>
     *   <li>Verifies there is no content creation in the database</li>
     * </ul>
     *
     * @throws Exception if there's an error during the test execution
     */
    @Test
    void test_process_preview_with_key_fields3() throws Exception {

        ContentType testContentType = null;

        try {
            // Initialize processor
            final var processor = new ImportContentletsProcessor();

            // Create test content type
            testContentType = createTestContentType();

            // Prepare test content type fields to use in the job
            Field titleField = null;
            Field bodyField = null;
            for (Field field : testContentType.fields()) {
                if (field.variable().equals("title")) {
                    titleField = field;
                } else if (field.variable().equals("body")) {
                    bodyField = field;
                }
            }
            assertNotNull(titleField);
            assertNotNull(bodyField);

            // Create test CSV file
            File csvFile = createTestCsvFile();

            // Create test job
            final var testJob = createTestJob(
                    csvFile, "preview", "1", testContentType.id(),
                    WORKFLOW_PUBLISH_ACTION_ID,
                    List.of(titleField.id(), bodyField.id())
            );

            // Process the job in preview mode
            processor.process(testJob);

            // Verify preview results
            Map<String, Object> metadata = processor.getResultMetadata(testJob);
            assertNotNull(metadata, "Preview metadata should not be null");
            assertNotNull(metadata.get("error"), "Preview metadata errors should not be null");
            assertEquals(0, ((ArrayList) metadata.get("error")).size(),
                    "Preview metadata errors should be empty");

            // Verify no content was created
            final var importedContent = findImportedContent(testContentType.id());
            assertNotNull(importedContent, "Imported content should not be null");
            assertEquals(0, importedContent.size(), "Imported content should have no items");

        } finally {
            if (testContentType != null) {
                // Clean up test content type
                APILocator.getContentTypeAPI(systemUser).delete(testContentType);
            }
        }
    }

    /**
     * Tests the preview mode of the content import process. This test:
     * <ul>
     *   <li>Creates a test content type</li>
     *   <li>Generates a test CSV file with sample content</li>
     *   <li>Processes the import in preview mode</li>
     *   <li>Verifies the preview results and metadata</li>
     *   <li>Verifies there is no content creation in the database</li>
     * </ul>
     *
     * <p>The test ensures that preview mode properly validates the content
     * without actually creating it in the system.
     *
     * @throws Exception if there's an error during the test execution
     */
    @Test
    void test_process_preview() throws Exception {

        ContentType testContentType = null;

        try {
            // Initialize processor
            final var processor = new ImportContentletsProcessor();

            // Create test content type
            testContentType = createTestContentType();

            // Create test CSV file
            File csvFile = createTestCsvFile();

            // Create test job
            final var testJob = createTestJob(
                    csvFile, "preview", "1", testContentType.id(),
                    WORKFLOW_PUBLISH_ACTION_ID
            );

            // Process the job in preview mode
            processor.process(testJob);

            // Verify preview results
            Map<String, Object> metadata = processor.getResultMetadata(testJob);
            assertNotNull(metadata, "Preview metadata should not be null");
            assertNotNull(metadata.get("error"), "Preview metadata errors should not be null");
            assertEquals(0, ((ArrayList) metadata.get("error")).size(),
                    "Preview metadata errors should be empty");

            // Verify no content was created
            final var importedContent = findImportedContent(testContentType.id());
            assertNotNull(importedContent, "Imported content should not be null");
            assertEquals(0, importedContent.size(), "Imported content should have no items");

        } finally {
            if (testContentType != null) {
                // Clean up test content type
                APILocator.getContentTypeAPI(systemUser).delete(testContentType);
            }
        }
    }

    /**
     * Tests the publish mode of the content import process. This test:
     * <ul>
     *   <li>Creates a test content type</li>
     *   <li>Generates a test CSV file with sample content</li>
     *   <li>Processes the import in publish mode</li>
     *   <li>Verifies the actual content creation in the database</li>
     * </ul>
     *
     * <p>The test confirms that content is properly created in the system
     * and matches the data provided in the CSV file.
     *
     * @throws Exception if there's an error during the test execution
     */
    @Test
    void test_process_publish() throws Exception {

        ContentType testContentType = null;

        try {
            // Initialize processor
            final var processor = new ImportContentletsProcessor();

            // Create test content type
            testContentType = createTestContentType();

            // Create test CSV file
            File csvFile = createTestCsvFile();

            // Create test job
            final var testJob = createTestJob(
                    csvFile, "publish", "1", testContentType.id(),
                    WORKFLOW_PUBLISH_ACTION_ID
            );

            // Process the job in preview mode
            processor.process(testJob);

            // Verify preview results
            Map<String, Object> metadata = processor.getResultMetadata(testJob);
            assertNotNull(metadata, "Publish metadata should not be null");
            assertNotNull(metadata.get("error"), "Publish metadata errors should not be null");
            assertEquals(0, ((ArrayList) metadata.get("error")).size(),
                    "Publish metadata errors should be empty");

            // Verify the content was actually created
            final var importedContent = findImportedContent(testContentType.id());
            assertNotNull(importedContent, "Imported content should not be null");
            assertEquals(2, importedContent.size(), "Imported content should have 2 items");

        } finally {
            if (testContentType != null) {
                // Clean up test content type
                APILocator.getContentTypeAPI(systemUser).delete(testContentType);
            }
        }
    }

    /**
     * Creates a test content type for import operations. The content type is designed to support
     * rich text content and is suitable for testing import functionality.
     *
     * @return A newly created {@link ContentType} instance
     */
    private ContentType createTestContentType() {
        return TestDataUtils.getRichTextLikeContentType();
    }

    /**
     * Creates a test job for the import process.
     *
     * @param csvFile          The CSV file containing the content to be imported
     * @param cmd              The command to execute ('preview' or 'publish')
     * @param contentType      The content type for the imported content
     * @param language         The language of the imported content
     * @param workflowActionId The ID of the workflow action to be applied
     * @return A configured {@link Job} instance ready for processing
     * @throws IOException          if there's an error reading the CSV file
     * @throws DotSecurityException if there's a security violation during job creation
     */
    private Job createTestJob(final File csvFile, final String cmd, final String language,
                              final String contentType, final String workflowActionId)
            throws IOException, DotSecurityException {
        return createTestJob(csvFile, cmd, language, contentType, workflowActionId, null);
    }

    /**
     * Creates a test job for the import process with optional fields.
     *
     * @param csvFile          The CSV file containing the content to be imported
     * @param cmd              The command to execute ('preview' or 'publish')
     * @param contentType      The content type for the imported content
     * @param language         The language of the imported content
     * @param workflowActionId The ID of the workflow action to be applied
     * @param fields           Additional fields to include in the job parameters
     * @return A configured {@link Job} instance ready for processing
     * @throws IOException          if there's an error reading the CSV file
     * @throws DotSecurityException if there's a security violation during job creation
     */
    private Job createTestJob(final File csvFile, final String cmd, final String language,
                              final String contentType, final String workflowActionId, final List<String> fields)
            throws IOException, DotSecurityException {

        final Map<String, Object> jobParameters = buildJobParameters(cmd, contentType, language, workflowActionId, fields);
        addTempFileToJobParameters(csvFile, jobParameters);

        return buildJob(jobParameters);
    }

    /**
     * Builds the base job parameters.
     *
     * @param cmd              The command to execute ('preview' or 'publish')
     * @param contentType      The content type for the imported content
     * @param language         The language of the imported content
     * @param workflowActionId The ID of the workflow action to be applied
     * @param fields           Additional fields to include in the job parameters
     * @return A map containing the base job parameters
     */
    private Map<String, Object> buildJobParameters(final String cmd, final String contentType,
                                                   final String language, final String workflowActionId,
                                                   final List<String> fields) {
        final Map<String, Object> jobParameters = new HashMap<>();
        jobParameters.put("cmd", cmd);
        jobParameters.put("userId", systemUser.getUserId());
        jobParameters.put("siteName", defaultSite.getHostname());
        jobParameters.put("siteIdentifier", defaultSite.getIdentifier());
        jobParameters.put("contentType", contentType);
        jobParameters.put("workflowActionId", workflowActionId);

        if (language != null) {
            jobParameters.put("language", language);
        }
        if (fields != null) {
            jobParameters.put("fields", fields);
        }

        return jobParameters;
    }

    /**
     * Adds a temporary file to the job parameters.
     *
     * @param csvFile       The CSV file to be used
     * @param jobParameters The job parameters map to update
     * @throws IOException          if there's an error reading the CSV file
     * @throws DotSecurityException if there's a security violation during temporary file creation
     */
    private void addTempFileToJobParameters(final File csvFile, final Map<String, Object> jobParameters)
            throws IOException, DotSecurityException {

        final TempFileAPI tempFileAPI = APILocator.getTempFileAPI();
        try (final var fileInputStream = new FileInputStream(csvFile)) {
            final DotTempFile tempFile = tempFileAPI.createTempFile(csvFile.getName(), request, fileInputStream);
            jobParameters.put("tempFileId", tempFile.id);
            jobParameters.put("requestFingerPrint", tempFileAPI.getRequestFingerprint(request));
        }
    }

    /**
     * Builds the final job instance.
     *
     * @param jobParameters The parameters to configure the job
     * @return A configured {@link Job} instance
     */
    private Job buildJob(final Map<String, Object> jobParameters) {
        return Job.builder()
                .id("test-job-id")
                .queueName("Test Job")
                .state(JobState.RUNNING)
                .parameters(jobParameters)
                .progressTracker(new DefaultProgressTracker())
                .build();
    }


    /**
     * Creates a test CSV file with sample content. The file includes a header row and two content
     * rows with title and body fields.
     *
     * @return A temporary {@link File} containing the CSV data
     * @throws IOException if there's an error creating or writing to the file
     */
    private File createTestCsvFile() throws IOException {

        // Create a CSV file that matches your content type structure
        StringBuilder csv = new StringBuilder();
        csv.append("title,body\n");
        csv.append("Test Title 1,Test Body 1\n");
        csv.append("Test Title 2,Test Body 2\n");

        File csvFile = File.createTempFile("test", ".csv");
        Files.write(csvFile.toPath(), csv.toString().getBytes());

        return csvFile;
    }

    /**
     * Retrieves the list of content that was imported during the test.
     *
     * @param contentTypeId The ID of the content type to search for
     * @return A list of {@link Contentlet} objects that were imported
     * @throws Exception if there's an error retrieving the content
     */
    private List<Contentlet> findImportedContent(final String contentTypeId) throws Exception {
        return APILocator.getContentletAPI().findByStructure(
                contentTypeId, systemUser, false, -1, 0
        );
    }

}