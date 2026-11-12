package com.dotcms.rest.api.v1.linkchecker;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * One page of the broken-links list of a content type, limited to the rows the requesting user
 * can read.
 *
 * @param list  the rows of the requested page, sorted by contentlet inode, then field id, then URL
 * @param total the number of rows the requesting user can read across all pages
 */
@Schema(description = "A page of broken links and the total number of rows the requesting user can read")
public record BrokenLinksView(
        @Schema(description = "Rows of the requested page") List<BrokenLinkView> list,
        @Schema(description = "Number of rows the requesting user can read across all pages", example = "42") int total) {
}
