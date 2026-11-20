package com.dotcms.rest.api.v1.linkchecker;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response envelope for a page of broken links, returned by {@code GET /v1/link-checker/broken-links}.
 */
public class ResponseEntityBrokenLinksView extends ResponseEntityView<BrokenLinksView> {

    /**
     * Wraps a page of broken links.
     *
     * @param entity the rows the caller can read plus their total
     */
    public ResponseEntityBrokenLinksView(final BrokenLinksView entity) {
        super(entity);
    }
}
