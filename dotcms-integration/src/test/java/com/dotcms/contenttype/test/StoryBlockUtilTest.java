package com.dotcms.contenttype.test;

import com.dotcms.contenttype.util.StoryBlockUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.dotcms.util.JsonUtil;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for StoryBlockUtil utility methods.
 * These tests focus on testing the utility methods in isolation.
 */
public class StoryBlockUtilTest {

    @Test
    public void test_isEmptyStoryBlock_null_input_returns_true() {
        assertTrue("Null input should return true", StoryBlockUtil.isEmptyStoryBlock(null));
    }

    @Test
    public void test_isEmptyStoryBlock_handles_string_edge_cases() {
        // Test various string edge cases that could cause issues
        assertTrue("Empty string should return true", StoryBlockUtil.isEmptyStoryBlock(""));
        assertTrue("Only spaces should return true", StoryBlockUtil.isEmptyStoryBlock("   "));
        assertTrue("Only tabs should return true", StoryBlockUtil.isEmptyStoryBlock("\t\t"));
        assertTrue("Only newlines should return true", StoryBlockUtil.isEmptyStoryBlock("\n\n"));
        assertTrue("Mixed whitespace should return true", StoryBlockUtil.isEmptyStoryBlock(" \t\n "));
    }

    @Test
    public void test_isEmptyStoryBlock_empty_string_returns_true() {
        assertTrue("Empty string should return true", StoryBlockUtil.isEmptyStoryBlock(""));
        assertTrue("Whitespace string should return true", StoryBlockUtil.isEmptyStoryBlock("   "));
    }

    @Test
    public void test_isEmptyStoryBlock_invalid_json_returns_true() {
        // Invalid JSON should be treated as empty to force validation failure
        assertTrue("Invalid JSON should return true (fail validation)", StoryBlockUtil.isEmptyStoryBlock("invalid json"));
        assertTrue("Malformed JSON should return true (fail validation)", StoryBlockUtil.isEmptyStoryBlock("{invalid}"));
    }

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

    @Test
    public void test_isEmptyStoryBlock_no_content_property_returns_true() {
        final String noContent = "{\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        assertTrue("Story block without content property should return true", StoryBlockUtil.isEmptyStoryBlock(noContent));
    }

    @Test
    public void test_isEmptyStoryBlock_empty_content_array_returns_true() {
        final String emptyContentArray = "{\n" +
                "  \"content\": [],\n" +
                "  \"type\": \"doc\"\n" +
                "}";

        assertTrue("Story block with empty content array should return true", StoryBlockUtil.isEmptyStoryBlock(emptyContentArray));
    }

    @Test
    public void test_isEmptyBlock_no_type_returns_true() throws Exception {
        final JsonNode blockWithoutType = JsonUtil.JSON_MAPPER.readTree("{}");
        assertTrue("Block without type should return true", StoryBlockUtil.isEmptyBlock(blockWithoutType));
    }

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

    @Test
    public void test_isTextContentEmpty_no_content_property_returns_true() throws Exception {
        final JsonNode blockWithoutContent = JsonUtil.JSON_MAPPER.readTree(
                "{\n" +
                "  \"type\": \"paragraph\"\n" +
                "}"
        );
        assertTrue("Block without content property should return true", StoryBlockUtil.isTextContentEmpty(blockWithoutContent));
    }

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
}
