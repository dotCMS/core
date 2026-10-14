package com.dotcms.rest.api.v1.index;

import javax.annotation.Nullable;

/**
 * The few values a failed reindex record shows about its content, read without loading the
 * contentlet (review of #37924).
 *
 * @param inode               the inode of the version resolved for display
 * @param languageId          the language of that version
 * @param title               the stored title of that version; {@code null} when none is stored
 * @param contentTypeVariable the content type variable; {@code null} when it cannot be resolved
 */
record DisplayVersion(String inode, long languageId, @Nullable String title,
                      @Nullable String contentTypeVariable) {
}
