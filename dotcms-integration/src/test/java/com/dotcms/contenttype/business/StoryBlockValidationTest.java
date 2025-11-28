package com.dotcms.contenttype.business;

import com.dotcms.IntegrationTestBase;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableStoryBlockField;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.datagen.ContentTypeDataGen;
import com.dotcms.datagen.ContentletDataGen;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
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
    }

    /**
     * Test that an empty story block (only empty paragraph) fails validation when required
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

        try {
            // Test the full save process - validation happens automatically during save
            APILocator.getContentletAPI().checkin(contentlet, systemUser, false);
            fail("Expected DotContentletValidationException for empty required story block");
        } catch (DotContentletValidationException e) {
            assertTrue("Should have required field errors", e.hasRequiredErrors());
            assertEquals("Should have one required field error", 1,
                    e.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_REQUIRED).size());
        } catch (DotDataException | DotSecurityException e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    /**
     * Test that a story block with text content passes validation
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
     * Test that a story block with image passes validation (structure = content)
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
     * Test that a story block with video passes validation (structure = content)
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
     * Test that a story block with list passes validation (structure = content)
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
     * Test that a story block with table passes validation (structure = content)
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
     * Test that a story block with blockquote passes validation (structure = content)
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
     * Test that a story block with code block containing text passes validation
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
     * Test that an empty code block fails validation when required
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

        try {
            // Test the full save process - validation happens automatically during save
            APILocator.getContentletAPI().checkin(contentlet, systemUser, false);
            fail("Expected DotContentletValidationException for empty required code block");
        } catch (DotContentletValidationException e) {
            assertTrue("Should have required field errors", e.hasRequiredErrors());
        } catch (DotDataException | DotSecurityException e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    /**
     * Test that a story block with horizontal rule passes validation (structure = content)
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
     * Tested method {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, List, boolean)}
     *
     * Given scenario: Story Block field contains malformed JSON that looks like a JSON attempt
     *
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
     * Test that a story block field with wrong type (Integer) fails validation
     */
    @Test
    public void test_story_block_integer_type_validation_fails() {
        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", 12345) // Integer instead of String
                .next();

        try {
            APILocator.getContentletAPI().checkin(contentlet, systemUser, false);
            fail("Expected DotContentletValidationException for Integer field type");
        } catch (DotContentletValidationException e) {
            assertTrue("Should contain bad type error", e.hasBadTypeErrors());
            Logger.info(this, "Expected validation failure for Integer type: " + e.getMessage());
        } catch (Exception e) {
            fail("Unexpected exception type: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    /**
     * Test that a story block field with wrong type (Boolean) fails validation
     */
    @Test
    public void test_story_block_boolean_type_validation_fails() {
        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", Boolean.TRUE) // Boolean instead of String
                .next();

        try {
            APILocator.getContentletAPI().checkin(contentlet, systemUser, false);
            fail("Expected DotContentletValidationException for Boolean field type");
        } catch (DotContentletValidationException e) {
            assertTrue("Should contain bad type error", e.hasBadTypeErrors());
            Logger.info(this, "Expected validation failure for Boolean type: " + e.getMessage());
        } catch (Exception e) {
            fail("Unexpected exception type: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    /**
     * Test that a story block field with wrong type (Object) fails validation
     */
    @Test
    public void test_story_block_object_type_validation_fails() {
        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", new Object()) // Object instead of String
                .next();

        try {
            APILocator.getContentletAPI().checkin(contentlet, systemUser, false);
            fail("Expected DotContentletValidationException for Object field type");
        } catch (DotContentletValidationException e) {
            assertTrue("Should contain bad type error", e.hasBadTypeErrors());
            Logger.info(this, "Expected validation failure for Object type: " + e.getMessage());
        } catch (Exception e) {
            fail("Unexpected exception type: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    /**
     * Test that type validation provides helpful error information
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
        } catch (Exception e) {
            fail("Unexpected exception type: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    /**
     * Test that a null story block field (not set at all) fails validation when required
     */
    @Test
    public void test_null_story_block_required_validation_fails() {
        // Create contentlet without setting the storyBlockField property at all
        final Contentlet contentlet = new ContentletDataGen(testContentType)
                // Intentionally NOT setting storyBlockField - it will be null
                .next();

        try {
            // Test the full save process - validation happens automatically during save
            APILocator.getContentletAPI().checkin(contentlet, systemUser, false);
            fail("Expected DotContentletValidationException for null required story block field");
        } catch (DotContentletValidationException e) {
            assertTrue("Should have required field errors", e.hasRequiredErrors());
            assertEquals("Should have one required field error", 1,
                    e.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_REQUIRED).size());
        } catch (DotDataException | DotSecurityException e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    /**
     * Tested method {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, List, boolean)}
     *
     * Given scenario: Story Block field contains legacy WYSIWYG content (plain text) with actual content
     *
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
     * Tested method {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, List, boolean)}
     *
     * Given scenario: Story Block field contains legacy WYSIWYG content with HTML tags
     *
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
     * Tested method {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, List, boolean)}
     *
     * Given scenario: Required Story Block field contains empty legacy WYSIWYG content (empty string)
     *
     * Expected Result: Validation should fail since empty content doesn't satisfy required field constraint
     */
    @Test
    public void test_empty_legacy_wysiwyg_content_fails_validation() {
        final String emptyLegacyContent = "";

        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", emptyLegacyContent)
                .next();

        try {
            APILocator.getContentletAPI().checkin(contentlet, systemUser, false);
            fail("Expected DotContentletValidationException for empty legacy WYSIWYG content");
        } catch (DotContentletValidationException e) {
            assertTrue("Should have required field errors", e.hasRequiredErrors());
            assertEquals("Should have one required field error", 1,
                    e.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_REQUIRED).size());
        } catch (DotDataException | DotSecurityException e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    /**
     * Tested method {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, List, boolean)}
     *
     * Given scenario: Required Story Block field contains whitespace-only legacy WYSIWYG content
     *
     * Expected Result: Validation should fail since whitespace-only content doesn't satisfy required field constraint
     */
    @Test
    public void test_whitespace_legacy_wysiwyg_content_fails_validation() {
        final String whitespaceOnlyContent = "   \n\t   ";

        final Contentlet contentlet = new ContentletDataGen(testContentType)
                .setProperty("storyBlockField", whitespaceOnlyContent)
                .next();

        try {
            APILocator.getContentletAPI().checkin(contentlet, systemUser, false);
            fail("Expected DotContentletValidationException for whitespace-only legacy content");
        } catch (DotContentletValidationException e) {
            assertTrue("Should have required field errors", e.hasRequiredErrors());
            assertEquals("Should have one required field error", 1,
                    e.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_REQUIRED).size());
        } catch (DotDataException | DotSecurityException e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    /**
     * Tested method {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, List, boolean)}
     *
     * Given scenario: Story Block field contains both valid Story Block JSON and passes validation
     *
     * Expected Result: Validation should continue to work correctly for proper Story Block JSON content
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
     * Tested method {@link com.dotcms.content.elasticsearch.business.ESContentletAPIImpl#validateContentlet(Contentlet, List, boolean)}
     *
     * Given scenario: Story Block field contains empty Story Block JSON (should still fail validation)
     *
     * Expected Result: Validation should still fail for empty Story Block JSON to maintain data quality
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

        try {
            APILocator.getContentletAPI().checkin(contentlet, systemUser, false);
            fail("Expected DotContentletValidationException for empty Story Block JSON");
        } catch (DotContentletValidationException e) {
            assertTrue("Should have required field errors", e.hasRequiredErrors());
            assertEquals("Should have one required field error", 1,
                    e.getNotValidFields().get(DotContentletValidationException.VALIDATION_FAILED_REQUIRED).size());
        } catch (DotDataException | DotSecurityException e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    /**
     * Test that multiple empty paragraphs still fail validation
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

        try {
            // Test the full save process - validation happens automatically during save
            APILocator.getContentletAPI().checkin(contentlet, systemUser, false);
            fail("Expected DotContentletValidationException for multiple empty paragraphs");
        } catch (DotContentletValidationException e) {
            assertTrue("Should have required field errors", e.hasRequiredErrors());
        } catch (DotDataException | DotSecurityException e) {
            fail("Unexpected exception: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }
}
