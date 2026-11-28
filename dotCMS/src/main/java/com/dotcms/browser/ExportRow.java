package com.dotcms.browser;

/**
 * One row of a Content Drive export, resolved before any contentlet is loaded: the working version
 * of one contentlet in one language.
 *
 * @param inode         the working inode of the contentlet in this language
 * @param identifier    the contentlet identifier
 * @param languageId    the language of this version
 * @param contentTypeId the identifier of the contentlet's content type
 */
public record ExportRow(String inode, String identifier, long languageId, String contentTypeId) {

    /**
     * The key rows are deduplicated by: one row per identifier and language, whatever the inode.
     *
     * @return {@code identifier + ":" + languageId}
     */
    public String key() {
        return identifier + ":" + languageId;
    }
}
