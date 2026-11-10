package com.dotcms.rest.api.v1.linkchecker;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * One row of the broken-links list: a link found in a field of a contentlet the requesting user
 * can read, together with the contentlet details needed to locate and fix it.
 *
 * @param inode        the inode of the contentlet that holds the link
 * @param contentTitle the title of that contentlet
 * @param status       "archived", "live", "working" or empty when none applies
 * @param field        the name of the field that holds the link
 * @param contentType  the name of the contentlet's content type
 * @param modDate      the contentlet's last modification date, formatted as {@code MM/dd/yyyy hh:mm a}
 * @param modUserName  the full name of the last editor, empty when the editor cannot be resolved
 * @param linkTitle    the title of the link
 * @param url          the URL of the link
 */
@Schema(description = "A broken link found in a contentlet the requesting user can read")
public record BrokenLinkView(
        @Schema(description = "Inode of the contentlet that holds the link", example = "6a8a1f2c-3b4d-4e5f-8a9b-0c1d2e3f4a5b") String inode,
        @Schema(description = "Title of the contentlet", example = "Sample title") String contentTitle,
        @Schema(description = "Contentlet status: archived, live, working or empty", example = "live") String status,
        @Schema(description = "Name of the field that holds the link", example = "Body") String field,
        @Schema(description = "Name of the contentlet's content type", example = "Blog") String contentType,
        @Schema(description = "Last modification date, formatted as MM/dd/yyyy hh:mm a", example = "03/14/2026 09:30 AM") String modDate,
        @Schema(description = "Full name of the last editor; empty when the editor cannot be resolved", example = "Jane Doe") String modUserName,
        @Schema(description = "Title of the link", example = "Read more") String linkTitle,
        @Schema(description = "URL of the link", example = "https://example.com/page") String url) {
}
