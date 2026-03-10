package com.dotcms.contenttype.util;

import com.dotcms.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

/**
 * Utility class for Story Block field validation and processing.
 * Contains domain-specific business logic for determining whether a Story Block
 * field contains meaningful content or is effectively empty.
 *
 * @author dotCMS Team
 */
public class StoryBlockUtil {

    /**
     * Validates if a Story Block field contains meaningful content or is effectively empty.
     * A Story Block is considered empty if it only contains empty text blocks (paragraphs, headings, code blocks)
     * without actual text content. Non-text blocks (images, videos, lists, tables, etc.) are always
     * considered as having content.
     *
     * @param storyBlockValue The JSON string representing the Story Block content
     * @return true if the Story Block is effectively empty, false if it has meaningful content
     */
    public static boolean isEmptyStoryBlock(final String storyBlockValue) {
        if (!UtilMethods.isSet(storyBlockValue) || storyBlockValue.trim().isEmpty()) {
            return true;
        }

        try {
            final JsonNode storyBlockJson = JsonUtil.JSON_MAPPER.readTree(storyBlockValue);

            // Check if it has content array
            if (!storyBlockJson.has("content")) {
                return true;
            }

            final JsonNode contentNode = storyBlockJson.get("content");
            if (!contentNode.isArray() || contentNode.size() == 0) {
                return true;
            }

            // If any block has content, the story block has content
            for (JsonNode block : contentNode) {
                if (!isEmptyBlock(block)) {
                    return false; // Found content, story block is not empty
                }
            }

            // All blocks are empty, so story block is empty
            return true;

        } catch (Exception e) {
            Logger.warn(StoryBlockUtil.class, "Error parsing Story Block JSON, treating as empty due to malformation: " + e.getMessage());
            return true; // Malformed JSON = empty = validation fails
        }
    }

    /**
     * Helper method to determine if a specific block within a Story Block is empty.
     * Simple logic: If it's a text block, check if it has actual text content.
     * For everything else (images, videos, custom blocks, etc.), assume it has content.
     *
     * @param block The JSON node representing a single block
     * @return true if the block is effectively empty, false otherwise
     */
    public static boolean isEmptyBlock(final JsonNode block) {
        if (!block.has("type")) {
            return true; // No type means invalid/empty block
        }

        final String blockType = block.get("type").asText();

        // For text-based blocks, check if they contain actual text content
        if ("paragraph".equals(blockType) || "heading".equals(blockType) || "codeBlock".equals(blockType)) {
            return isTextContentEmpty(block);
        }

        // For everything else (images, videos, lists, tables, blockquotes, custom blocks, etc.):
        // If the block exists, it represents content,
        // This is to avoid recursive checking of blocks that are not text-based like lists on tables or similar.
        return false;
    }

    /**
     * Extracts the character count from a Story Block JSON value.
     * The TipTap editor stores character count metadata in the root {@code attrs.charCount} field.
     *
     * @param storyBlockValue The JSON string representing the Story Block content
     * @return An {@link OptionalInt} containing the character count if present and valid,
     *         or {@link OptionalInt#empty()} if the value is not set, not valid JSON,
     *         or does not contain a charCount attribute.
     */
    public static OptionalInt getCharCount(final String storyBlockValue) {
        if (!UtilMethods.isSet(storyBlockValue)) {
            return OptionalInt.empty();
        }

        try {
            final JsonNode storyBlockJson = JsonUtil.JSON_MAPPER.readTree(storyBlockValue);

            if (storyBlockJson.has("attrs")) {
                final JsonNode attrsNode = storyBlockJson.get("attrs");
                if (attrsNode.has("charCount") && attrsNode.get("charCount").isInt()) {
                    return OptionalInt.of(attrsNode.get("charCount").asInt());
                }
            }

            return OptionalInt.empty();
        } catch (Exception e) {
            Logger.debug(StoryBlockUtil.class,
                    "Unable to extract charCount from Story Block JSON: " + e.getMessage());
            return OptionalInt.empty();
        }
    }

    /**
     * Computes the total number of characters contained in the text content of a Story Block JSON.
     * This recursively collects all {@code text} node values and returns their total length,
     * matching the character counting behavior of the TipTap editor.
     *
     * @param storyBlockValue The JSON string representing the Story Block content
     * @return The number of characters in all text nodes, or {@code 0} if the value is not set or
     *         invalid JSON.
     */
    public static int computeCharCount(final String storyBlockValue) {
        if (!UtilMethods.isSet(storyBlockValue)) {
            return 0;
        }
        try {
            final JsonNode storyBlockJson = JsonUtil.JSON_MAPPER.readTree(storyBlockValue);
            return joinTextNodes(storyBlockJson).length();
        } catch (final Exception e) {
            Logger.debug(StoryBlockUtil.class,
                    "Unable to compute charCount from Story Block JSON: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Computes the total number of words contained in the text content of a Story Block JSON.
     * This recursively collects all {@code text} node values, joins them, and splits on whitespace
     * to count words, matching the word counting behavior of the TipTap editor.
     *
     * @param storyBlockValue The JSON string representing the Story Block content
     * @return The number of words across all text nodes, or {@code 0} if the value is not set,
     *         contains no text, or is invalid JSON.
     */
    public static int computeWordCount(final String storyBlockValue) {
        if (!UtilMethods.isSet(storyBlockValue)) {
            return 0;
        }
        try {
            final JsonNode storyBlockJson = JsonUtil.JSON_MAPPER.readTree(storyBlockValue);
            final String text = joinTextNodes(storyBlockJson).trim();
            if (text.isEmpty()) {
                return 0;
            }
            return text.split("\\s+").length;
        } catch (final Exception e) {
            Logger.debug(StoryBlockUtil.class,
                    "Unable to compute wordCount from Story Block JSON: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Computes the estimated reading time in minutes for a given word count, using the average
     * adult reading speed of 265 words per minute as defined by Medium.
     *
     * @param wordCount The number of words
     * @return The estimated reading time in minutes (minimum 0 for empty content)
     * @see <a href="https://help.medium.com/hc/en-us/articles/214991667-Read-time">Medium Read Time</a>
     */
    public static int computeReadingTime(final int wordCount) {
        if (wordCount <= 0) {
            return 0;
        }
        return (int) Math.ceil(wordCount / 265.0);
    }

    /**
     * Builds a single string from all text nodes in a Block Editor JSON by joining them with a
     * space separator. This models the TipTap editor behavior of treating separate content blocks
     * as distinct words.
     *
     * @param root The root JSON node of the Story Block document
     * @return A string containing all text content, with text from different nodes separated by
     *         spaces
     */
    private static String joinTextNodes(final JsonNode root) {
        final List<String> segments = new ArrayList<>();
        collectTextSegments(root, segments);
        return String.join(" ", segments);
    }

    /**
     * Recursively collects text values from all {@code text} fields in the Block Editor JSON tree
     * into the provided list.
     *
     * @param node     The JSON node to traverse
     * @param segments The list to append extracted text segments to
     */
    private static void collectTextSegments(final JsonNode node, final List<String> segments) {
        if (node.has("text")) {
            segments.add(node.get("text").asText());
        }
        if (node.has("content") && node.get("content").isArray()) {
            for (final JsonNode child : node.get("content")) {
                collectTextSegments(child, segments);
            }
        }
    }

    /**
     * Helper method to check if a text block contains only empty text
     * @param block The block to check for text content
     * @return true if all text content is empty, false otherwise
     */
    public static boolean isTextContentEmpty(final JsonNode block) {
        final JsonNode contentNode = block.get("content");
        if (contentNode == null || !contentNode.isArray() || contentNode.size() == 0) {
            return true; // No content property means empty
        }

        // Check if all content items are empty text nodes
        for (JsonNode contentItem : contentNode) {
            if (contentItem != null) {
                final String itemType = contentItem.has("type") ? contentItem.get("type").asText() : "";

                if ("text".equals(itemType)) {
                    final String text = contentItem.has("text") ? contentItem.get("text").asText() : "";
                    if (UtilMethods.isSet(text.trim())) {
                        return false; // Found non-empty text
                    }
                }
                // Note: We only consider "text" nodes for text content
                // Other types like "hardBreak" might exist but don't contain text
            }
        }

        return true; // All text content is empty
    }
}
