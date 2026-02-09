package com.dotcms.contenttype.util;

import com.dotcms.util.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;

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
