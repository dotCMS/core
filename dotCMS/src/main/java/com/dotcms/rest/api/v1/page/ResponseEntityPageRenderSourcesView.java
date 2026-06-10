package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response entity for the {@code GET /api/v1/page/_render-sources} endpoint.
 */
public class ResponseEntityPageRenderSourcesView extends ResponseEntityView<PageRenderSourcesView> {

    public ResponseEntityPageRenderSourcesView(final PageRenderSourcesView entity) {
        super(entity);
    }
}
