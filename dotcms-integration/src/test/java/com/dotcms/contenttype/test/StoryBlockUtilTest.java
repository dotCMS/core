package com.dotcms.contenttype.test;

import com.dotcms.contenttype.util.StoryBlockUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.dotcms.util.JsonUtil;
import org.junit.Test;

import java.util.OptionalInt;

import static org.junit.Assert.*;

/**
 * Unit tests for StoryBlockUtil utility methods.
 * These tests focus on testing the utility methods in isolation.
 */
public class StoryBlockUtilTest {

    /**
     * Method to test: {@link StoryBlockUtil#isEmptyStoryBlock(String)}
     * Given Scenario: Input parameter is null
     * Expected Result: Method should return true, treating null as empty
     */
    @Test
    public void test_isEmptyStoryBlock_null_input_returns_true() {
        assertTrue("Null input should return true", StoryBlockUtil.isEmptyStoryBlock(null));
    }

    /**
     * Method to test: {@link StoryBlockUtil#isEmptyStoryBlock(String)}
     * Given Scenario: Input contains various whitespace-only strings (spaces, tabs, newlines)
     * Expected Result: All whitespace-only inputs should return true, being treated as empty
     */
    @Test
    public void test_isEmptyStoryBlock_handles_string_edge_cases() {
        // Test various string edge cases that could cause issues
        assertTrue("Empty string should return true", StoryBlockUtil.isEmptyStoryBlock(""));
        assertTrue("Only spaces should return true", StoryBlockUtil.isEmptyStoryBlock("   "));
        assertTrue("Only tabs should return true", StoryBlockUtil.isEmptyStoryBlock("\t\t"));
        assertTrue("Only newlines should return true", StoryBlockUtil.isEmptyStoryBlock("\n\n"));
        assertTrue("Mixed whitespace should return true", StoryBlockUtil.isEmptyStoryBlock(" \t\n "));
    }

    /**
     * Method to test: {@link StoryBlockUtil#isEmptyStoryBlock(String)}
     * Given Scenario: Input is empty string or contains only whitespace characters
     * Expected Result: Method should return true for both empty and whitespace-only strings
     */
    @Test
    public void test_isEmptyStoryBlock_empty_string_returns_true() {
        assertTrue("Empty string should return true", StoryBlockUtil.isEmptyStoryBlock(""));
        assertTrue("Whitespace string should return true", StoryBlockUtil.isEmptyStoryBlock("   "));
    }

    /**
     * Method to test: {@link StoryBlockUtil#isEmptyStoryBlock(String)}
     * Given Scenario: Input contains invalid or malformed JSON strings
     * Expected Result: Method should return true to force validation failure for invalid JSON
     */
    @Test
    public void test_isEmptyStoryBlock_invalid_json_returns_true() {
        // Invalid JSON should be treated as empty to force validation failure
        assertTrue("Invalid JSON should return true (fail validation)", StoryBlockUtil.isEmptyStoryBlock("invalid json"));
        assertTrue("Malformed JSON should return true (fail validation)", StoryBlockUtil.isEmptyStoryBlock("{invalid}"));
    }

    /**
     * Method to test: {@link StoryBlockUtil#isEmptyStoryBlock(String)}
     * Given Scenario: StoryBlock JSON contains only an empty paragraph element with no text content
     * Expected Result: Method should return true as empty paragraphs contain no meaningful content
     */
    @Test
    public void test_isEmptyStoryBlock_empty_paragraph_returns_true() {
        final String emptyParagraph = "{\n" +
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

        assertTrue("Empty paragraph should return true", StoryBlockUtil.isEmptyStoryBlock(emptyParagraph));
    }

    /**
     * Method to test: {@link StoryBlockUtil#isEmptyStoryBlock(String)}
     * Given Scenario: StoryBlock JSON contains a paragraph element with actual text content
     * Expected Result: Method should return false as the block contains meaningful text content
     */
    @Test
    public void test_isEmptyStoryBlock_paragraph_with_text_returns_false() {
        final String paragraphWithText = "{\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"attrs\": {\n" +
                "        \"indent\": 0,\n" +
                "        \"textAlign\": null\n" +
                "      },\n" +
                "      \"content\": [\n" +
                "        {\n" +
                "          \"text\": \"Hello World\",\n" +
                "          \"type\": \"text\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"type\": \"paragraph\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        assertFalse("Paragraph with text should return false", StoryBlockUtil.isEmptyStoryBlock(paragraphWithText));
    }

    /**
     * Method to test: {@link StoryBlockUtil#isEmptyStoryBlock(String)}
     * Given Scenario: StoryBlock JSON contains an image block element with identifier data
     * Expected Result: Method should return false as image blocks represent meaningful content
     */
    @Test
    public void test_isEmptyStoryBlock_image_block_returns_false() {
        final String imageBlock = "{\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"attrs\": {\n" +
                "        \"data\": {\n" +
                "          \"identifier\": \"test-image-id\",\n" +
                "          \"languageId\": 1\n" +
                "        }\n" +
                "      },\n" +
                "      \"type\": \"dotImage\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        assertFalse("Image block should return false", StoryBlockUtil.isEmptyStoryBlock(imageBlock));
    }

    /**
     * Method to test: {@link StoryBlockUtil#isEmptyStoryBlock(String)}
     * Given Scenario: StoryBlock JSON document structure has no content property defined
     * Expected Result: Method should return true as missing content indicates empty block
     */
    @Test
    public void test_isEmptyStoryBlock_no_content_property_returns_true() {
        final String noContent = "{\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        assertTrue("Story block without content property should return true", StoryBlockUtil.isEmptyStoryBlock(noContent));
    }

    /**
     * Method to test: {@link StoryBlockUtil#isEmptyStoryBlock(String)}
     * Given Scenario: StoryBlock JSON has content property defined as an empty array
     * Expected Result: Method should return true as empty content array indicates no meaningful content
     */
    @Test
    public void test_isEmptyStoryBlock_empty_content_array_returns_true() {
        final String emptyContentArray = "{\n" +
                "  \"content\": [],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        assertTrue("Story block with empty content array should return true", StoryBlockUtil.isEmptyStoryBlock(emptyContentArray));
    }

    /**
     * Method to test: {@link StoryBlockUtil#isEmptyBlock(JsonNode)}
     * Given Scenario: JsonNode block has no type property defined
     * Expected Result: Method should return true as blocks without type are considered empty
     */
    @Test
    public void test_isEmptyBlock_no_type_returns_true() throws Exception {
        final JsonNode blockWithoutType = JsonUtil.JSON_MAPPER.readTree("{}");
        assertTrue("Block without type should return true", StoryBlockUtil.isEmptyBlock(blockWithoutType));
    }

    /**
     * Method to test: {@link StoryBlockUtil#isEmptyBlock(JsonNode)}
     * Given Scenario: JsonNode represents a paragraph block without content property
     * Expected Result: Method should return true as paragraphs without content are empty
     */
    @Test
    public void test_isEmptyBlock_paragraph_without_content_returns_true() throws Exception {
        final JsonNode emptyParagraph = JsonUtil.JSON_MAPPER.readTree(
                "{\n" +
                "  \"type\": \"paragraph\",\n" +
                "  \"attrs\": {\n" +
                "    \"indent\": 0,\n" +
                "    \"textAlign\": null\n" +
                "  }\n" +
                "}"
        );
        assertTrue("Empty paragraph should return true", StoryBlockUtil.isEmptyBlock(emptyParagraph));
    }

    /**
     * Method to test: {@link StoryBlockUtil#isEmptyBlock(JsonNode)}
     * Given Scenario: JsonNode represents a paragraph block containing text content
     * Expected Result: Method should return false as the paragraph contains meaningful text
     */
    @Test
    public void test_isEmptyBlock_paragraph_with_text_returns_false() throws Exception {
        final JsonNode paragraphWithText = JsonUtil.JSON_MAPPER.readTree(
                "{\n" +
                "  \"type\": \"paragraph\",\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"type\": \"text\",\n" +
                "      \"text\": \"Hello\"\n" +
                "    }\n" +
                "  ]\n" +
                "}"
        );
        assertFalse("Paragraph with text should return false", StoryBlockUtil.isEmptyBlock(paragraphWithText));
    }

    /**
     * Method to test: {@link StoryBlockUtil#isEmptyBlock(JsonNode)}
     * Given Scenario: JsonNode represents an image block with identifier data
     * Expected Result: Method should return false as image blocks represent meaningful content
     */
    @Test
    public void test_isEmptyBlock_image_returns_false() throws Exception {
        final JsonNode imageBlock = JsonUtil.JSON_MAPPER.readTree(
                "{\n" +
                "  \"type\": \"dotImage\",\n" +
                "  \"attrs\": {\n" +
                "    \"data\": {\n" +
                "      \"identifier\": \"test-id\"\n" +
                "    }\n" +
                "  }\n" +
                "}"
        );
        assertFalse("Image block should return false", StoryBlockUtil.isEmptyBlock(imageBlock));
    }

    /**
     * Method to test: {@link StoryBlockUtil#isEmptyBlock(JsonNode)}
     * Given Scenario: JsonNode represents a list block element (bulletList type)
     * Expected Result: Method should return false as list structures are considered content even when empty
     */
    @Test
    public void test_isEmptyBlock_list_returns_false() throws Exception {
        final JsonNode listBlock = JsonUtil.JSON_MAPPER.readTree(
                "{\n" +
                "  \"type\": \"bulletList\",\n" +
                "  \"content\": []\n" +
                "}"
        );
        assertFalse("List block should return false (structure = content)", StoryBlockUtil.isEmptyBlock(listBlock));
    }

    /**
     * Method to test: {@link StoryBlockUtil#isTextContentEmpty(JsonNode)}
     * Given Scenario: JsonNode block has no content property defined
     * Expected Result: Method should return true as missing content property indicates empty text
     */
    @Test
    public void test_isTextContentEmpty_no_content_property_returns_true() throws Exception {
        final JsonNode blockWithoutContent = JsonUtil.JSON_MAPPER.readTree(
                "{\n" +
                "  \"type\": \"paragraph\"\n" +
                "}"
        );
        assertTrue("Block without content property should return true", StoryBlockUtil.isTextContentEmpty(blockWithoutContent));
    }

    /**
     * Method to test: {@link StoryBlockUtil#isTextContentEmpty(JsonNode)}
     * Given Scenario: JsonNode block has content property defined as an empty array
     * Expected Result: Method should return true as empty content array contains no text
     */
    @Test
    public void test_isTextContentEmpty_empty_content_array_returns_true() throws Exception {
        final JsonNode blockWithEmptyContent = JsonUtil.JSON_MAPPER.readTree(
                "{\n" +
                "  \"type\": \"paragraph\",\n" +
                "  \"content\": []\n" +
                "}"
        );
        assertTrue("Block with empty content array should return true", StoryBlockUtil.isTextContentEmpty(blockWithEmptyContent));
    }

    /**
     * Method to test: {@link StoryBlockUtil#isTextContentEmpty(JsonNode)}
     * Given Scenario: JsonNode contains text elements with empty text values
     * Expected Result: Method should return true as empty text values contain no meaningful content
     */
    @Test
    public void test_isTextContentEmpty_empty_text_returns_true() throws Exception {
        final JsonNode blockWithEmptyText = JsonUtil.JSON_MAPPER.readTree(
                "{\n" +
                "  \"type\": \"paragraph\",\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"type\": \"text\",\n" +
                "      \"text\": \"\"\n" +
                "    }\n" +
                "  ]\n" +
                "}"
        );
        assertTrue("Block with empty text should return true", StoryBlockUtil.isTextContentEmpty(blockWithEmptyText));
    }

    /**
     * Method to test: {@link StoryBlockUtil#isTextContentEmpty(JsonNode)}
     * Given Scenario: JsonNode contains text elements with only whitespace characters
     * Expected Result: Method should return true as whitespace-only text is considered empty
     */
    @Test
    public void test_isTextContentEmpty_whitespace_text_returns_true() throws Exception {
        final JsonNode blockWithWhitespace = JsonUtil.JSON_MAPPER.readTree(
                "{\n" +
                "  \"type\": \"paragraph\",\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"type\": \"text\",\n" +
                "      \"text\": \"   \"\n" +
                "    }\n" +
                "  ]\n" +
                "}"
        );
        assertTrue("Block with whitespace text should return true", StoryBlockUtil.isTextContentEmpty(blockWithWhitespace));
    }

    /**
     * Method to test: {@link StoryBlockUtil#isTextContentEmpty(JsonNode)}
     * Given Scenario: JsonNode contains text elements with actual meaningful text content
     * Expected Result: Method should return false as the block contains real text content
     */
    @Test
    public void test_isTextContentEmpty_actual_text_returns_false() throws Exception {
        final JsonNode blockWithText = JsonUtil.JSON_MAPPER.readTree(
                "{\n" +
                "  \"type\": \"paragraph\",\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"type\": \"text\",\n" +
                "      \"text\": \"Hello World\"\n" +
                "    }\n" +
                "  ]\n" +
                "}"
        );
        assertFalse("Block with actual text should return false", StoryBlockUtil.isTextContentEmpty(blockWithText));
    }

    /**
     * Method to test: {@link StoryBlockUtil#isTextContentEmpty(JsonNode)}
     * Given Scenario: JsonNode contains mix of empty text elements and elements with actual content
     * Expected Result: Method should return false as at least one element contains meaningful text
     */
    @Test
    public void test_isTextContentEmpty_mixed_empty_and_text_returns_false() throws Exception {
        final JsonNode blockWithMixedContent = JsonUtil.JSON_MAPPER.readTree(
                "{\n" +
                "  \"type\": \"paragraph\",\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"type\": \"text\",\n" +
                "      \"text\": \"\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"type\": \"text\",\n" +
                "      \"text\": \"Hello\"\n" +
                "    }\n" +
                "  ]\n" +
                "}"
        );
        assertFalse("Block with mixed empty and actual text should return false", StoryBlockUtil.isTextContentEmpty(blockWithMixedContent));
    }

    // =========================================================================
    // getCharCount tests
    // =========================================================================

    /**
     * Method to test: {@link StoryBlockUtil#getCharCount(String)}
     * Given Scenario: Story Block JSON contains attrs.charCount with a valid integer value
     * Expected Result: Method should return OptionalInt with the charCount value
     */
    @Test
    public void test_getCharCount_returns_value_when_present() {
        final String storyBlock = "{\n" +
                "  \"attrs\": {\n" +
                "    \"charCount\": 42,\n" +
                "    \"readingTime\": 1,\n" +
                "    \"wordCount\": 8\n" +
                "  },\n" +
                "  \"content\": [],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        final OptionalInt result = StoryBlockUtil.getCharCount(storyBlock);
        assertTrue("Should have charCount value", result.isPresent());
        assertEquals("Should return correct charCount", 42, result.getAsInt());
    }

    /**
     * Method to test: {@link StoryBlockUtil#getCharCount(String)}
     * Given Scenario: Story Block JSON has attrs but no charCount property
     * Expected Result: Method should return empty OptionalInt
     */
    @Test
    public void test_getCharCount_returns_empty_when_no_charCount() {
        final String storyBlock = "{\n" +
                "  \"attrs\": {\n" +
                "    \"readingTime\": 1,\n" +
                "    \"wordCount\": 8\n" +
                "  },\n" +
                "  \"content\": [],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        final OptionalInt result = StoryBlockUtil.getCharCount(storyBlock);
        assertFalse("Should return empty when no charCount", result.isPresent());
    }

    /**
     * Method to test: {@link StoryBlockUtil#getCharCount(String)}
     * Given Scenario: Story Block JSON has no attrs property at all
     * Expected Result: Method should return empty OptionalInt
     */
    @Test
    public void test_getCharCount_returns_empty_when_no_attrs() {
        final String storyBlock = "{\n" +
                "  \"content\": [],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        final OptionalInt result = StoryBlockUtil.getCharCount(storyBlock);
        assertFalse("Should return empty when no attrs", result.isPresent());
    }

    /**
     * Method to test: {@link StoryBlockUtil#getCharCount(String)}
     * Given Scenario: Input is null
     * Expected Result: Method should return empty OptionalInt
     */
    @Test
    public void test_getCharCount_returns_empty_for_null() {
        final OptionalInt result = StoryBlockUtil.getCharCount(null);
        assertFalse("Should return empty for null input", result.isPresent());
    }

    /**
     * Method to test: {@link StoryBlockUtil#getCharCount(String)}
     * Given Scenario: Input is empty string
     * Expected Result: Method should return empty OptionalInt
     */
    @Test
    public void test_getCharCount_returns_empty_for_empty_string() {
        final OptionalInt result = StoryBlockUtil.getCharCount("");
        assertFalse("Should return empty for empty string", result.isPresent());
    }

    /**
     * Method to test: {@link StoryBlockUtil#getCharCount(String)}
     * Given Scenario: Input is malformed/invalid JSON
     * Expected Result: Method should return empty OptionalInt without throwing
     */
    @Test
    public void test_getCharCount_returns_empty_for_invalid_json() {
        final OptionalInt result = StoryBlockUtil.getCharCount("{invalid json}");
        assertFalse("Should return empty for invalid JSON", result.isPresent());
    }

    /**
     * Method to test: {@link StoryBlockUtil#getCharCount(String)}
     * Given Scenario: Story Block JSON has charCount of 0
     * Expected Result: Method should return OptionalInt with value 0
     */
    @Test
    public void test_getCharCount_returns_zero_when_charCount_is_zero() {
        final String storyBlock = "{\n" +
                "  \"attrs\": {\n" +
                "    \"charCount\": 0\n" +
                "  },\n" +
                "  \"content\": [],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        final OptionalInt result = StoryBlockUtil.getCharCount(storyBlock);
        assertTrue("Should have charCount value", result.isPresent());
        assertEquals("Should return zero charCount", 0, result.getAsInt());
    }

    /**
     * Method to test: {@link StoryBlockUtil#getCharCount(String)}
     * Given Scenario: Story Block JSON has charCount as a string instead of integer
     * Expected Result: Method should return empty OptionalInt as the value is not an integer
     */
    @Test
    public void test_getCharCount_returns_empty_for_non_integer_charCount() {
        final String storyBlock = "{\n" +
                "  \"attrs\": {\n" +
                "    \"charCount\": \"not a number\"\n" +
                "  },\n" +
                "  \"content\": [],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        final OptionalInt result = StoryBlockUtil.getCharCount(storyBlock);
        assertFalse("Should return empty for non-integer charCount", result.isPresent());
    }

    // =========================================================================
    // computeCharCount tests
    // =========================================================================

    /**
     * Method to test: {@link StoryBlockUtil#computeCharCount(String)}
     * Given Scenario: Story Block JSON contains a single text node
     * Expected Result: Method should return the character count of the text
     */
    @Test
    public void test_computeCharCount_single_text_node() {
        final String storyBlock = "{\n" +
                "  \"type\": \"doc\",\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"type\": \"paragraph\",\n" +
                "      \"content\": [\n" +
                "        {\"type\": \"text\", \"text\": \"A title!!\"}\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        assertEquals("Should count 9 characters in 'A title!!'", 9, StoryBlockUtil.computeCharCount(storyBlock));
    }

    /**
     * Method to test: {@link StoryBlockUtil#computeCharCount(String)}
     * Given Scenario: Story Block JSON contains multiple text nodes across blocks
     * Expected Result: Method should return the total character count including spaces
     */
    @Test
    public void test_computeCharCount_multiple_text_nodes() {
        final String storyBlock = "{\n" +
                "  \"type\": \"doc\",\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"type\": \"paragraph\",\n" +
                "      \"content\": [\n" +
                "        {\"type\": \"text\", \"text\": \"Hello\"}\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"type\": \"paragraph\",\n" +
                "      \"content\": [\n" +
                "        {\"type\": \"text\", \"text\": \"World\"}\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        // "Hello World" = 11 (5 + separator + 5)
        assertEquals("Should count 11 characters including separator", 11, StoryBlockUtil.computeCharCount(storyBlock));
    }

    /**
     * Method to test: {@link StoryBlockUtil#computeCharCount(String)}
     * Given Scenario: Story Block JSON is null
     * Expected Result: Method should return 0
     */
    @Test
    public void test_computeCharCount_null_returns_zero() {
        assertEquals("Null input should return 0", 0, StoryBlockUtil.computeCharCount(null));
    }

    /**
     * Method to test: {@link StoryBlockUtil#computeCharCount(String)}
     * Given Scenario: Story Block JSON contains no text content
     * Expected Result: Method should return 0
     */
    @Test
    public void test_computeCharCount_empty_block_returns_zero() {
        final String storyBlock = "{\n" +
                "  \"type\": \"doc\",\n" +
                "  \"content\": [\n" +
                "    {\"type\": \"paragraph\"}\n" +
                "  ]\n" +
                "}";

        assertEquals("Empty block should return 0", 0, StoryBlockUtil.computeCharCount(storyBlock));
    }

    // =========================================================================
    // computeWordCount tests
    // =========================================================================

    /**
     * Method to test: {@link StoryBlockUtil#computeWordCount(String)}
     * Given Scenario: Story Block JSON contains text with multiple words
     * Expected Result: Method should return the correct word count
     */
    @Test
    public void test_computeWordCount_returns_correct_count() {
        final String storyBlock = "{\n" +
                "  \"type\": \"doc\",\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"type\": \"heading\",\n" +
                "      \"content\": [\n" +
                "        {\"type\": \"text\", \"text\": \"A title!!\"}\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        assertEquals("Should count 2 words in 'A title!!'", 2, StoryBlockUtil.computeWordCount(storyBlock));
    }

    /**
     * Method to test: {@link StoryBlockUtil#computeWordCount(String)}
     * Given Scenario: Story Block JSON contains text spread across multiple blocks
     * Expected Result: Words across all blocks are counted together
     */
    @Test
    public void test_computeWordCount_multiple_blocks() {
        final String storyBlock = "{\n" +
                "  \"type\": \"doc\",\n" +
                "  \"content\": [\n" +
                "    {\n" +
                "      \"type\": \"paragraph\",\n" +
                "      \"content\": [\n" +
                "        {\"type\": \"text\", \"text\": \"Hello World\"}\n" +
                "      ]\n" +
                "    },\n" +
                "    {\n" +
                "      \"type\": \"paragraph\",\n" +
                "      \"content\": [\n" +
                "        {\"type\": \"text\", \"text\": \"Foo Bar\"}\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        assertEquals("Should count 4 words across two paragraphs", 4, StoryBlockUtil.computeWordCount(storyBlock));
    }

    /**
     * Method to test: {@link StoryBlockUtil#computeWordCount(String)}
     * Given Scenario: Story Block JSON is null
     * Expected Result: Method should return 0
     */
    @Test
    public void test_computeWordCount_null_returns_zero() {
        assertEquals("Null input should return 0", 0, StoryBlockUtil.computeWordCount(null));
    }

    /**
     * Method to test: {@link StoryBlockUtil#computeWordCount(String)}
     * Given Scenario: Story Block JSON contains no text content
     * Expected Result: Method should return 0
     */
    @Test
    public void test_computeWordCount_empty_content_returns_zero() {
        final String storyBlock = "{\n" +
                "  \"type\": \"doc\",\n" +
                "  \"content\": []\n" +
                "}";

        assertEquals("Empty content should return 0", 0, StoryBlockUtil.computeWordCount(storyBlock));
    }

    // =========================================================================
    // computeReadingTime tests
    // =========================================================================

    /**
     * Method to test: {@link StoryBlockUtil#computeReadingTime(int)}
     * Given Scenario: Word count is 0
     * Expected Result: Method should return 0
     */
    @Test
    public void test_computeReadingTime_zero_words_returns_zero() {
        assertEquals("0 words should give 0 minutes reading time", 0, StoryBlockUtil.computeReadingTime(0));
    }

    /**
     * Method to test: {@link StoryBlockUtil#computeReadingTime(int)}
     * Given Scenario: Word count is less than 265 (less than 1 minute)
     * Expected Result: Method should return 1 (ceiling of fraction)
     */
    @Test
    public void test_computeReadingTime_under_one_minute_rounds_up() {
        assertEquals("100 words (< 265) should round up to 1 minute", 1, StoryBlockUtil.computeReadingTime(100));
        assertEquals("264 words (< 265) should round up to 1 minute", 1, StoryBlockUtil.computeReadingTime(264));
    }

    /**
     * Method to test: {@link StoryBlockUtil#computeReadingTime(int)}
     * Given Scenario: Word count is exactly 265
     * Expected Result: Method should return 1 minute
     */
    @Test
    public void test_computeReadingTime_exactly_one_minute() {
        assertEquals("265 words should give exactly 1 minute", 1, StoryBlockUtil.computeReadingTime(265));
    }

    /**
     * Method to test: {@link StoryBlockUtil#computeReadingTime(int)}
     * Given Scenario: Word count requires multiple minutes
     * Expected Result: Method should return the correct number of minutes (ceiling)
     */
    @Test
    public void test_computeReadingTime_multiple_minutes() {
        assertEquals("530 words should give 2 minutes", 2, StoryBlockUtil.computeReadingTime(530));
        assertEquals("375 words should give 2 minutes (2m read as shown in UI)", 2, StoryBlockUtil.computeReadingTime(375));
        assertEquals("531 words should round up to 3 minutes", 3, StoryBlockUtil.computeReadingTime(531));
    }
}
