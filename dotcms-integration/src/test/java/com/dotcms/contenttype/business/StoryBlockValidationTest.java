package com.dotcms.contenttype.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldVariable;
import com.dotcms.contenttype.model.field.ImmutableFieldVariable;
import com.dotcms.contenttype.model.field.ImmutableStoryBlockField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.DotContentletValidationException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.portal.model.User;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Integration tests for Story Block field validation logic.
 * Tests the isEmptyStoryBlock validation method in ESContentletAPIImpl.
 */
public class StoryBlockValidationTest extends IntegrationTestBase {

    private static User systemUser;
    private static ContentType testContentType;
    private static Field storyBlockField;

    private static ContentType charLimitContentType;
    private static Field charLimitStoryBlockField;

    /**
     * Functional interface for operations that can throw checked exceptions
     */
    @FunctionalInterface
    private interface CheckedOperation {
        void run() throws DotDataException, DotSecurityException;
    }

    /**
     * Helper method to handle DotContentletValidationException that might be wrapped in DotRuntimeException
     * for REQUIRED field validation errors
     */
    private void expectRequiredFieldValidationException(CheckedOperation operation, String expectedErrorMessage) {
        try {
            operation.run();
            fail(expectedErrorMessage);
        } catch (DotContentletValidationException e) {
            // Direct exception - validation logic working correctly
            assertTrue("Should have required field errors", e.hasRequiredErrors());
            assertEquals("Should have one required field error", 1,
                    e.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_REQUIRED).size());
        } catch (DotRuntimeException e) {
            // Handle wrapped DotContentletValidationException or direct DotRuntimeException with validation error
            if (e.getCause() instanceof DotContentletValidationException) {
                DotContentletValidationException ve = (DotContentletValidationException) e.getCause();
                assertTrue("Should have required field errors", ve.hasRequiredErrors());
                assertEquals("Should have one required field error", 1,
                        ve.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_REQUIRED).size());
            } else {
                // Check if this is a validation error by examining the message
                final String message = e.getMessage();
                if (message != null && (message.contains("[REQUIRED]") || message.contains("has invalid/missing field"))) {
                    // DotRuntimeException thrown directly with validation error message
                    // Verify it's about our field
                    assertTrue("Exception message should indicate required field error: " + message,
                            message.contains("Story Block Field") || message.contains("storyBlockField"));
                } else {
                    fail("Unexpected wrapped exception: " + e.getClass().getSimpleName() + " - " + message);
                }
            }
        } catch (DotDataException | DotSecurityException e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    /**
     * Helper method to handle DotContentletValidationException that might be wrapped in DotRuntimeException
     * for BADTYPE field validation errors
     */
    private void expectBadTypeValidationException(CheckedOperation operation, String expectedErrorMessage) {
        try {
            operation.run();
            fail(expectedErrorMessage);
        } catch (DotContentletValidationException e) {
            // Direct exception - validation logic working correctly
            assertTrue("Should contain bad type error", e.hasBadTypeErrors());
        } catch (DotRuntimeException e) {
            // Handle wrapped DotContentletValidationException or direct DotRuntimeException with validation error
            if (e.getCause() instanceof DotContentletValidationException) {
                DotContentletValidationException ve = (DotContentletValidationException) e.getCause();
                assertTrue("Should contain bad type error", ve.hasBadTypeErrors());
            } else {
                // Check if this is a validation error by examining the message
                final String message = e.getMessage();
                if (message != null && (message.contains("[BADTYPE]") || message.contains("has invalid/missing field"))) {
                    // DotRuntimeException thrown directly with validation error message
                    // Verify it's about our field
                    assertTrue("Exception message should indicate bad type error: " + message,
                            message.contains("Story Block Field") || message.contains("storyBlockField"));
                } else {
                    fail("Unexpected exception type: " + e.getClass().getSimpleName() + " - " + message);
                }
            }
        } catch (DotDataException | DotSecurityException e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    /**
     * Helper method to handle DotContentletValidationException for CHAR_LIMIT (character limit exceeded) errors
     */
    private void expectCharLimitValidationException(CheckedOperation operation, String expectedErrorMessage) {
        try {
            operation.run();
            fail(expectedErrorMessage);
        } catch (DotContentletValidationException e) {
            assertTrue("Should contain char limit error", e.hasCharLimitErrors());
        } catch (DotRuntimeException e) {
            if (e.getCause() instanceof DotContentletValidationException) {
                DotContentletValidationException ve = (DotContentletValidationException) e.getCause();
                assertTrue("Should contain char limit error", ve.hasCharLimitErrors());
            } else {
                final String message = e.getMessage();
                assertTrue("Exception message should indicate char limit error: " + message,
                        message != null && (message.contains("charLimitExceeded") || message.contains("character limit") || message.contains("has invalid/missing field")));
            }
        } catch (DotDataException | DotSecurityException e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    @BeforeClass
    public static void prepare() throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        systemUser = APILocator.getUserAPI().getSystemUser();

        // Create a content type with a required story block field for testing
        testContentType = new ContentTypeDataGen()
                .name("StoryBlockValidationTest")
                .velocityVarName("storyBlockValidationTest")
                .nextPersisted();

        storyBlockField = ImmutableStoryBlockField.builder()
                .name("Story Block Field")
                .variable("storyBlockField")
                .contentTypeId(testContentType.id())
                .required(true)
                .build();

        storyBlockField = APILocator.getContentTypeFieldAPI().save(storyBlockField, systemUser);

        // Create a content type with a story block field that has a charLimit field variable
        charLimitContentType = new ContentTypeDataGen()
                .name("StoryBlockCharLimitTest")
                .velocityVarName("storyBlockCharLimitTest")
                .nextPersisted();

        charLimitStoryBlockField = ImmutableStoryBlockField.builder()
                .name("Story Block With Limit")
                .variable("storyBlockWithLimit")
                .contentTypeId(charLimitContentType.id())
                .required(false)
                .build();

        charLimitStoryBlockField = APILocator.getContentTypeFieldAPI().save(charLimitStoryBlockField, systemUser);

        // Add charLimit field variable with a limit of 25 characters
        final FieldVariable charLimitVar = ImmutableFieldVariable.builder()
                .key("charLimit")
                .value("25")
                .fieldId(charLimitStoryBlockField.id())
                .build();
        APILocator.getContentTypeFieldAPI().save(charLimitVar, systemUser);
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A required Story Block field contains an empty story block (only an empty paragraph node with no text content)
     * Expected Result: Validation should fail with a REQUIRED field error since the story block has no meaningful content
     */
    @Test
    public void test_empty_story_block_required_validation_fails() {
        final String emptyStoryBlock = "{\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"attrs\": {\n" +
                "        \"indent\": 0,\n" +
                "        \"textAlign\": null\n" +
                "      },\n" +
                "      \"type\": \"paragraph\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", emptyStoryBlock)
                .next();

        expectRequiredFieldValidationException(
                () -> APILocator.getContentletAPI().checkin(contentlet, systemUser, false),
                "Expected DotContentletValidationException for empty required story block"
        );
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A required Story Block field contains a paragraph with text content ("Hello World!")
     * Expected Result: Validation should pass and the contentlet should be saved successfully with the story block content preserved
     */
    @Test
    public void test_story_block_with_text_passes_validation() throws DotDataException, DotSecurityException {
        final String storyBlockWithText = "{\n" +
                "  \"attrs\": {\n" +
                "    \"charCount\": 12,\n" +
                "    \"readingTime\": 1,\n" +
                "    \"wordCount\": 2\n" +
                "  },\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"attrs\": {\n" +
                "        \"indent\": 0,\n" +
                "        \"textAlign\": null\n" +
                "      },\n" +
                "      \"content\": [\n" +
                "        {\n" +
                "          \"text\": \"Hello World!\",\n" +
                "          \"type\": \"text\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"type\": \"paragraph\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", storyBlockWithText)
                .next();

        // Should not throw validation exception - save should succeed
        final Contentlet savedContentlet = APILocator.getContentletAPI().checkin(contentlet, systemUser, false);

        // Assert that the contentlet was successfully saved
        assertNotNull("Saved contentlet should not be null", savedContentlet);
        assertTrue("Saved contentlet should have a valid inode", UtilMethods.isSet(savedContentlet.getInode()));
        assertEquals("Story block content should be preserved", storyBlockWithText, savedContentlet.get("storyBlockField"));
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A required Story Block field contains a dotImage node (structural content with an image identifier)
     * Expected Result: Validation should pass since a dotImage node represents meaningful content even without text
     */
    @Test
    public void test_story_block_with_image_passes_validation() throws DotDataException, DotSecurityException {
        final String storyBlockWithImage = "{\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"attrs\": {\n" +
                "        \"data\": {\n" +
                "          \"identifier\": \"test-image-identifier\",\n" +
                "          \"languageId\": 1\n" +
                "        }\n" +
                "      },\n" +
                "      \"type\": \"dotImage\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", storyBlockWithImage)
                .next();

        // Should not throw validation exception - save should succeed
        final Contentlet savedContentlet = APILocator.getContentletAPI().checkin(contentlet, systemUser, false);

        // Assert that the contentlet was successfully saved
        assertNotNull("Saved contentlet should not be null", savedContentlet);
        assertTrue("Saved contentlet should have a valid inode", UtilMethods.isSet(savedContentlet.getInode()));
        assertEquals("Story block content should be preserved", storyBlockWithImage, savedContentlet.get("storyBlockField"));
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A required Story Block field contains a dotVideo node (structural content with a video identifier)
     * Expected Result: Validation should pass since a dotVideo node represents meaningful content even without text
     */
    @Test
    public void test_story_block_with_video_passes_validation() throws DotDataException, DotSecurityException {
        final String storyBlockWithVideo = "{\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"attrs\": {\n" +
                "        \"data\": {\n" +
                "          \"identifier\": \"test-video-identifier\",\n" +
                "          \"languageId\": 1\n" +
                "        }\n" +
                "      },\n" +
                "      \"type\": \"dotVideo\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", storyBlockWithVideo)
                .next();

        // Should not throw validation exception - save should succeed
        final Contentlet savedContentlet = APILocator.getContentletAPI().checkin(contentlet, systemUser, false);

        // Assert that the contentlet was successfully saved
        assertNotNull("Saved contentlet should not be null", savedContentlet);
        assertTrue("Saved contentlet should have a valid inode", UtilMethods.isSet(savedContentlet.getInode()));
        assertEquals("Story block content should be preserved", storyBlockWithVideo, savedContentlet.get("storyBlockField"));
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A required Story Block field contains a bulletList node with a listItem (structural content)
     * Expected Result: Validation should pass since a bulletList node represents structural content even if the list items are empty
     */
    @Test
    public void test_story_block_with_list_passes_validation() throws DotDataException, DotSecurityException {
        final String storyBlockWithList = "{\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"content\": [\n" +
                "        {\n" +
                "          \"content\": [\n" +
                "            {\n" +
                "              \"attrs\": {\n" +
                "                \"indent\": 0,\n" +
                "                \"textAlign\": null\n" +
                "              },\n" +
                "              \"type\": \"paragraph\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"type\": \"listItem\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"type\": \"bulletList\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", storyBlockWithList)
                .next();

        // Should not throw validation exception - empty list still represents structure/content
        final Contentlet savedContentlet = APILocator.getContentletAPI().checkin(contentlet, systemUser, false);

        // Assert that the contentlet was successfully saved
        assertNotNull("Saved contentlet should not be null", savedContentlet);
        assertTrue("Saved contentlet should have a valid inode", UtilMethods.isSet(savedContentlet.getInode()));
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A required Story Block field contains a table node with rows and cells (structural content)
     * Expected Result: Validation should pass since a table node represents structural content even if cells are empty
     */
    @Test
    public void test_story_block_with_table_passes_validation() throws DotDataException, DotSecurityException {
        final String storyBlockWithTable = "{\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"content\": [\n" +
                "        {\n" +
                "          \"content\": [\n" +
                "            {\n" +
                "              \"attrs\": {\n" +
                "                \"colspan\": 1,\n" +
                "                \"colwidth\": null,\n" +
                "                \"rowspan\": 1\n" +
                "              },\n" +
                "              \"content\": [\n" +
                "                {\n" +
                "                  \"attrs\": {\n" +
                "                    \"indent\": 0,\n" +
                "                    \"textAlign\": null\n" +
                "                  },\n" +
                "                  \"type\": \"paragraph\"\n" +
                "                }\n" +
                "              ],\n" +
                "              \"type\": \"tableCell\"\n" +
                "            }\n" +
                "          ],\n" +
                "          \"type\": \"tableRow\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"type\": \"table\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", storyBlockWithTable)
                .next();

        // Should not throw validation exception - empty table still represents structure/content
        final Contentlet savedContentlet = APILocator.getContentletAPI().checkin(contentlet, systemUser, false);

        // Assert that the contentlet was successfully saved
        assertNotNull("Saved contentlet should not be null", savedContentlet);
        assertTrue("Saved contentlet should have a valid inode", UtilMethods.isSet(savedContentlet.getInode()));
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A required Story Block field contains a blockquote node (structural content wrapping an empty paragraph)
     * Expected Result: Validation should pass since a blockquote node represents structural content
     */
    @Test
    public void test_story_block_with_blockquote_passes_validation() throws DotDataException, DotSecurityException {
        final String storyBlockWithBlockquote = "{\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"content\": [\n" +
                "        {\n" +
                "          \"attrs\": {\n" +
                "            \"indent\": 0,\n" +
                "            \"textAlign\": null\n" +
                "          },\n" +
                "          \"type\": \"paragraph\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"type\": \"blockquote\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", storyBlockWithBlockquote)
                .next();

        // Should not throw validation exception - blockquote structure represents content
        final Contentlet savedContentlet = APILocator.getContentletAPI().checkin(contentlet, systemUser, false);

        // Assert that the contentlet was successfully saved
        assertNotNull("Saved contentlet should not be null", savedContentlet);
        assertTrue("Saved contentlet should have a valid inode", UtilMethods.isSet(savedContentlet.getInode()));
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A required Story Block field contains a codeBlock node with text content ("console.log('Hello World');")
     * Expected Result: Validation should pass and the contentlet should be saved successfully with the code block content preserved
     */
    @Test
    public void test_story_block_with_code_text_passes_validation() throws DotDataException, DotSecurityException {
        final String storyBlockWithCode = "{\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"attrs\": {\n" +
                "        \"language\": \"javascript\"\n" +
                "      },\n" +
                "      \"content\": [\n" +
                "        {\n" +
                "          \"text\": \"console.log('Hello World');\",\n" +
                "          \"type\": \"text\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"type\": \"codeBlock\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", storyBlockWithCode)
                .next();

        // Should not throw validation exception - save should succeed
        final Contentlet savedContentlet = APILocator.getContentletAPI().checkin(contentlet, systemUser, false);

        // Assert that the contentlet was successfully saved
        assertNotNull("Saved contentlet should not be null", savedContentlet);
        assertTrue("Saved contentlet should have a valid inode", UtilMethods.isSet(savedContentlet.getInode()));
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A required Story Block field contains a codeBlock node with an empty text string
     * Expected Result: Validation should fail with a REQUIRED field error since the code block has no meaningful content
     */
    @Test
    public void test_empty_code_block_required_validation_fails() {
        final String emptyCodeBlock = "{\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"attrs\": {\n" +
                "        \"language\": \"javascript\"\n" +
                "      },\n" +
                "      \"content\": [\n" +
                "        {\n" +
                "          \"text\": \"\",\n" +
                "          \"type\": \"text\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"type\": \"codeBlock\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", emptyCodeBlock)
                .next();

        expectRequiredFieldValidationException(
                () -> APILocator.getContentletAPI().checkin(contentlet, systemUser, false),
                "Expected DotContentletValidationException for empty required code block"
        );
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A required Story Block field contains a horizontalRule node (structural content)
     * Expected Result: Validation should pass since a horizontalRule node represents meaningful structural content
     */
    @Test
    public void test_story_block_with_horizontal_rule_passes_validation() throws DotDataException, DotSecurityException {
        final String storyBlockWithHR = "{\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"type\": \"horizontalRule\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", storyBlockWithHR)
                .next();

        // Should not throw validation exception - horizontal rule represents content
        final Contentlet savedContentlet = APILocator.getContentletAPI().checkin(contentlet, systemUser, false);

        // Assert that the contentlet was successfully saved
        assertNotNull("Saved contentlet should not be null", savedContentlet);
        assertTrue("Saved contentlet should have a valid inode", UtilMethods.isSet(savedContentlet.getInode()));
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A required Story Block field contains malformed JSON ("{ invalid json }") that looks like a JSON attempt
     * Expected Result: Validation should pass treating malformed JSON as legacy WYSIWYG content during migration
     */
    @Test
    public void test_malformed_json_passes_as_legacy_content() throws DotDataException, DotSecurityException {
        final String invalidJson = "{ invalid json }";

        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", invalidJson)
                .next();

        // Should not throw validation exception - malformed JSON treated as legacy WYSIWYG content
        final Contentlet savedContentlet = APILocator.getContentletAPI().checkin(contentlet, systemUser, false);

        // Assert that the contentlet was successfully saved
        assertNotNull("Saved contentlet should not be null", savedContentlet);
        assertTrue("Saved contentlet should have a valid inode", UtilMethods.isSet(savedContentlet.getInode()));
        assertEquals("Malformed JSON should be preserved as legacy content", invalidJson, savedContentlet.get("storyBlockField"));
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A Story Block field is set with an Integer value (12345) instead of a String
     * Expected Result: Validation should fail with a BADTYPE field error since Story Block fields must be String values
     */
    @Test
    public void test_story_block_integer_type_validation_fails() {
        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", 12345) // Integer instead of String
                .next();

        expectBadTypeValidationException(
                () -> APILocator.getContentletAPI().checkin(contentlet, systemUser, false),
                "Expected DotContentletValidationException for Integer field type"
        );
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A Story Block field is set with a Boolean value (true) instead of a String
     * Expected Result: Validation should fail with a BADTYPE field error since Story Block fields must be String values
     */
    @Test
    public void test_story_block_boolean_type_validation_fails() {
        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", Boolean.TRUE) // Boolean instead of String
                .next();

        expectBadTypeValidationException(
                () -> APILocator.getContentletAPI().checkin(contentlet, systemUser, false),
                "Expected DotContentletValidationException for Boolean field type"
        );
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A Story Block field is set with a plain Object value instead of a String
     * Expected Result: Validation should fail with a BADTYPE field error since Story Block fields must be String values
     */
    @Test
    public void test_story_block_object_type_validation_fails() {
        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", new Object()) // Object instead of String
                .next();

        expectBadTypeValidationException(
                () -> APILocator.getContentletAPI().checkin(contentlet, systemUser, false),
                "Expected DotContentletValidationException for Object field type"
        );
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A Story Block field is set with an Integer value (42) to trigger type validation
     * Expected Result: The BADTYPE validation error should contain field information in the notValidFields map
     */
    @Test
    public void test_story_block_type_validation_error_details() {
        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", 42) // Integer
                .next();

        try {
            APILocator.getContentletAPI().checkin(contentlet, systemUser, false);
            fail("Expected DotContentletValidationException for wrong field type");
        } catch (DotContentletValidationException e) {
            assertTrue("Should contain bad type error", e.hasBadTypeErrors());

            // Verify we get the field information
            assertTrue("Should have bad type fields map",
                e.getNotValidFields().containsKey(DotContentletValidationException.VALIDATION_FAILED_BADTYPE));

            assertFalse("Bad type fields list should not be empty",
                e.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_BADTYPE).isEmpty());

            Logger.info(this, "Type validation error details: " + e.getMessage());
        } catch (DotRuntimeException e) {
            // Handle wrapped DotContentletValidationException or direct DotRuntimeException with validation error
            if (e.getCause() instanceof DotContentletValidationException) {
                DotContentletValidationException ve = (DotContentletValidationException) e.getCause();
                assertTrue("Should contain bad type error", ve.hasBadTypeErrors());
                // Verify we get the field information
                assertTrue("Should have bad type fields map",
                    ve.getNotValidFields().containsKey(DotContentletValidationException.VALIDATION_FAILED_BADTYPE));
                assertFalse("Bad type fields list should not be empty",
                    ve.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_BADTYPE).isEmpty());
            } else if (e.getMessage() != null && e.getMessage().contains("[BADTYPE]")) {
                // DotRuntimeException thrown directly with validation error message
                assertTrue("Exception message should indicate bad type error",
                        e.getMessage().contains("Story Block Field"));
            } else {
                fail("Unexpected exception type: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            }
        } catch (DotDataException | DotSecurityException e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A required Story Block field is not set at all (null value) on the contentlet
     * Expected Result: Validation should fail with a REQUIRED field error since the field is mandatory
     */
    @Test
    public void test_null_story_block_required_validation_fails() {
        // Create contentlet without setting the storyBlockField property at all
        final Contentlet contentlet = new ContentletDataGen(testContentType)
                // Intentionally NOT setting storyBlockField - it will be null
                .next();

        expectRequiredFieldValidationException(
                () -> APILocator.getContentletAPI().checkin(contentlet, systemUser, false),
                "Expected DotContentletValidationException for null required story block field"
        );
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A required Story Block field contains legacy WYSIWYG content (plain text) with actual content
     * Expected Result: Validation should pass to support backward compatibility during WYSIWYG to Story Block migration
     */
    @Test
    public void test_legacy_wysiwyg_content_passes_validation() throws DotDataException, DotSecurityException {
        final String legacyWysiwygContent = "This is legacy WYSIWYG content from before migration";

        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", legacyWysiwygContent)
                .next();

        // Should not throw validation exception - legacy content should pass during migration
        final Contentlet savedContentlet = APILocator.getContentletAPI().checkin(contentlet, systemUser, false);

        // Assert that the contentlet was successfully saved
        assertNotNull("Saved contentlet should not be null", savedContentlet);
        assertTrue("Saved contentlet should have a valid inode", UtilMethods.isSet(savedContentlet.getInode()));
        assertEquals("Legacy content should be preserved", legacyWysiwygContent, savedContentlet.get("storyBlockField"));
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A required Story Block field contains legacy WYSIWYG content with HTML tags (<p>, <strong>, <em>)
     * Expected Result: Validation should pass to support HTML content from legacy WYSIWYG fields
     */
    @Test
    public void test_legacy_wysiwyg_html_content_passes_validation() throws DotDataException, DotSecurityException {
        final String legacyWysiwygHtml = "<p>This is <strong>legacy WYSIWYG</strong> content with <em>HTML tags</em></p>";

        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", legacyWysiwygHtml)
                .next();

        // Should not throw validation exception - legacy HTML content should pass
        final Contentlet savedContentlet = APILocator.getContentletAPI().checkin(contentlet, systemUser, false);

        // Assert that the contentlet was successfully saved
        assertNotNull("Saved contentlet should not be null", savedContentlet);
        assertTrue("Saved contentlet should have a valid inode", UtilMethods.isSet(savedContentlet.getInode()));
        assertEquals("Legacy HTML content should be preserved", legacyWysiwygHtml, savedContentlet.get("storyBlockField"));
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A required Story Block field contains empty legacy WYSIWYG content (empty string)
     * Expected Result: Validation should fail since empty content doesn't satisfy the required field constraint
     */
    @Test
    public void test_empty_legacy_wysiwyg_content_fails_validation() {
        final String emptyLegacyContent = "";

        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", emptyLegacyContent)
                .next();

        expectRequiredFieldValidationException(
                () -> APILocator.getContentletAPI().checkin(contentlet, systemUser, false),
                "Expected DotContentletValidationException for empty legacy WYSIWYG content"
        );
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A required Story Block field contains whitespace-only legacy WYSIWYG content (spaces, newlines, tabs)
     * Expected Result: Validation should fail since whitespace-only content doesn't satisfy the required field constraint
     */
    @Test
    public void test_whitespace_legacy_wysiwyg_content_fails_validation() {
        final String whitespaceOnlyContent = "   \n\t   ";

        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", whitespaceOnlyContent)
                .next();

        expectRequiredFieldValidationException(
                () -> APILocator.getContentletAPI().checkin(contentlet, systemUser, false),
                "Expected DotContentletValidationException for whitespace-only legacy content"
        );
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A required Story Block field contains valid Story Block JSON with a paragraph and text content
     * Expected Result: Validation should pass and the contentlet should be saved with the Story Block JSON content preserved
     */
    @Test
    public void test_story_block_json_still_validates_correctly() throws DotDataException, DotSecurityException {
        final String validStoryBlockJson = "{\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"type\": \"paragraph\",\n" +
                "      \"content\": [\n" +
                "        {\n" +
                "          \"type\": \"text\",\n" +
                "          \"text\": \"Valid Story Block content\"\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", validStoryBlockJson)
                .next();

        // Should not throw validation exception - proper Story Block JSON should pass
        final Contentlet savedContentlet = APILocator.getContentletAPI().checkin(contentlet, systemUser, false);

        // Assert that the contentlet was successfully saved
        assertNotNull("Saved contentlet should not be null", savedContentlet);
        assertTrue("Saved contentlet should have a valid inode", UtilMethods.isSet(savedContentlet.getInode()));
        assertEquals("Story Block JSON should be preserved", validStoryBlockJson, savedContentlet.get("storyBlockField"));
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A required Story Block field contains empty Story Block JSON (a doc with only an empty paragraph node)
     * Expected Result: Validation should fail with a REQUIRED field error to maintain data quality
     */
    @Test
    public void test_empty_story_block_json_still_fails_validation() {
        final String emptyStoryBlockJson = "{\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"type\": \"paragraph\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", emptyStoryBlockJson)
                .next();

        expectRequiredFieldValidationException(
                () -> APILocator.getContentletAPI().checkin(contentlet, systemUser, false),
                "Expected DotContentletValidationException for empty Story Block JSON"
        );
    }

    // =========================================================================
    // charLimit validation tests
    // =========================================================================

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A Story Block field with a charLimit field variable set to 25 contains content with charCount of 32
     * Expected Result: Validation should fail with a CHAR_LIMIT error since the content exceeds the configured character limit
     */
    @Test
    public void test_story_block_exceeding_char_limit_fails_validation() {
        final String storyBlockExceedingLimit = "{\n" +
                "  \"attrs\": {\n" +
                "    \"charCount\": 32,\n" +
                "    \"readingTime\": 1,\n" +
                "    \"wordCount\": 1\n" +
                "  },\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"attrs\": {\n" +
                "        \"indent\": 0,\n" +
                "        \"textAlign\": null\n" +
                "      },\n" +
                "      \"content\": [\n" +
                "        {\n" +
                "          \"text\": \"adasdasdasdasdasdasdasdasdasdads\",\n" +
                "          \"type\": \"text\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"type\": \"paragraph\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        final Contentlet contentlet = new ContentletDataGen(charLimitContentType)
                .setProperty("storyBlockWithLimit", storyBlockExceedingLimit)
                .next();

        expectCharLimitValidationException(
                () -> APILocator.getContentletAPI().checkin(contentlet, systemUser, false),
                "Expected DotContentletValidationException for story block exceeding charLimit"
        );
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A Story Block field with a charLimit field variable set to 25 contains content with charCount of 12
     * Expected Result: Validation should pass since the content is within the configured character limit
     */
    @Test
    public void test_story_block_within_char_limit_passes_validation() throws DotDataException, DotSecurityException {
        final String storyBlockWithinLimit = "{\n" +
                "  \"attrs\": {\n" +
                "    \"charCount\": 12,\n" +
                "    \"readingTime\": 1,\n" +
                "    \"wordCount\": 2\n" +
                "  },\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"attrs\": {\n" +
                "        \"indent\": 0,\n" +
                "        \"textAlign\": null\n" +
                "      },\n" +
                "      \"content\": [\n" +
                "        {\n" +
                "          \"text\": \"Hello World!\",\n" +
                "          \"type\": \"text\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"type\": \"paragraph\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        final Contentlet contentlet = new ContentletDataGen(charLimitContentType)
                .setProperty("storyBlockWithLimit", storyBlockWithinLimit)
                .next();

        // Should not throw validation exception - within the limit
        final Contentlet savedContentlet = APILocator.getContentletAPI().checkin(contentlet, systemUser, false);

        assertNotNull("Saved contentlet should not be null", savedContentlet);
        assertTrue("Saved contentlet should have a valid inode", UtilMethods.isSet(savedContentlet.getInode()));
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A Story Block field with a charLimit field variable set to 25 contains content with charCount of exactly 25
     * Expected Result: Validation should pass since the content is at exactly the configured character limit (boundary case)
     */
    @Test
    public void test_story_block_at_exact_char_limit_passes_validation() throws DotDataException, DotSecurityException {
        final String storyBlockAtLimit = "{\n" +
                "  \"attrs\": {\n" +
                "    \"charCount\": 25,\n" +
                "    \"readingTime\": 1,\n" +
                "    \"wordCount\": 1\n" +
                "  },\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"attrs\": {\n" +
                "        \"indent\": 0,\n" +
                "        \"textAlign\": null\n" +
                "      },\n" +
                "      \"content\": [\n" +
                "        {\n" +
                "          \"text\": \"abcdefghijklmnopqrstuvwxy\",\n" +
                "          \"type\": \"text\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"type\": \"paragraph\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        final Contentlet contentlet = new ContentletDataGen(charLimitContentType)
                .setProperty("storyBlockWithLimit", storyBlockAtLimit)
                .next();

        // Should not throw validation exception - exactly at the limit
        final Contentlet savedContentlet = APILocator.getContentletAPI().checkin(contentlet, systemUser, false);

        assertNotNull("Saved contentlet should not be null", savedContentlet);
        assertTrue("Saved contentlet should have a valid inode", UtilMethods.isSet(savedContentlet.getInode()));
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A Story Block field with a charLimit field variable set to 25 contains JSON without a charCount attribute in attrs (legacy content)
     * Expected Result: Validation should pass gracefully, skipping char limit check when charCount is absent from the JSON attrs
     */
    @Test
    public void test_story_block_without_char_count_attr_passes_validation() throws DotDataException, DotSecurityException {
        final String storyBlockNoCharCount = "{\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"attrs\": {\n" +
                "        \"indent\": 0,\n" +
                "        \"textAlign\": null\n" +
                "      },\n" +
                "      \"content\": [\n" +
                "        {\n" +
                "          \"text\": \"Some content without charCount in attrs\",\n" +
                "          \"type\": \"text\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"type\": \"paragraph\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        final Contentlet contentlet = new ContentletDataGen(charLimitContentType)
                .setProperty("storyBlockWithLimit", storyBlockNoCharCount)
                .next();

        // Should not throw - gracefully skips validation when charCount is not in JSON attrs
        final Contentlet savedContentlet = APILocator.getContentletAPI().checkin(contentlet, systemUser, false);

        assertNotNull("Saved contentlet should not be null", savedContentlet);
        assertTrue("Saved contentlet should have a valid inode", UtilMethods.isSet(savedContentlet.getInode()));
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A Story Block field without a charLimit field variable contains content with a very high charCount (9999)
     * Expected Result: Validation should pass since no charLimit field variable is configured on this field
     */
    @Test
    public void test_story_block_without_char_limit_variable_passes_validation() throws DotDataException, DotSecurityException {
        final String storyBlockHighCharCount = "{\n" +
                "  \"attrs\": {\n" +
                "    \"charCount\": 9999,\n" +
                "    \"readingTime\": 1,\n" +
                "    \"wordCount\": 1\n" +
                "  },\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"attrs\": {\n" +
                "        \"indent\": 0,\n" +
                "        \"textAlign\": null\n" +
                "      },\n" +
                "      \"content\": [\n" +
                "        {\n" +
                "          \"text\": \"Some text content\",\n" +
                "          \"type\": \"text\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"type\": \"paragraph\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", storyBlockHighCharCount)
                .next();

        // Should not throw - no charLimit field variable on this content type's field
        final Contentlet savedContentlet = APILocator.getContentletAPI().checkin(contentlet, systemUser, false);

        assertNotNull("Saved contentlet should not be null", savedContentlet);
        assertTrue("Saved contentlet should have a valid inode", UtilMethods.isSet(savedContentlet.getInode()));
    }

    /**
     * Method to test: {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, java.util.List, boolean)}
     * Given Scenario: A required Story Block field contains multiple empty paragraph nodes (no text content in any of them)
     * Expected Result: Validation should fail with a REQUIRED field error since multiple empty paragraphs still represent no meaningful content
     */
    @Test
    public void test_multiple_empty_paragraphs_fail_validation() {
        final String multipleEmptyParagraphs = "{\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"attrs\": {\n" +
                "        \"indent\": 0,\n" +
                "        \"textAlign\": null\n" +
                "      },\n" +
                "      \"type\": \"paragraph\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"attrs\": {\n" +
                "        \"indent\": 0,\n" +
                "        \"textAlign\": null\n" +
                "      },\n" +
                "      \"type\": \"paragraph\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", multipleEmptyParagraphs)
                .next();

        expectRequiredFieldValidationException(
                () -> APILocator.getContentletAPI().checkin(contentlet, systemUser, false),
                "Expected DotContentletValidationException for multiple empty paragraphs"
        );
    }
}
