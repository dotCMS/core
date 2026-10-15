package com.dotcms.rest.api.v1.index;

import javax.annotation.Nullable;

/**
 * The few values a failed reindex record shows about its content, read without loading the
 * contentlet (review of #37924).
 *
 * @param inode               the inode of the version
 * @param languageId          the language of the version
 * @param title               the stored title of the version; {@code null} when none is stored
 * @param contentTypeVariable the content type variable; {@code null} when it cannot be resolved
 * @param archived            {@code true} when the version's language is archived
 */
record DisplayVersion(String inode, long languageId, @Nullable String title,
                      @Nullable String contentTypeVariable, boolean archived) {
}
