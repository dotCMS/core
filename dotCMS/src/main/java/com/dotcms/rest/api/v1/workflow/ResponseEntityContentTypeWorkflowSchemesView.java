package com.dotcms.rest.api.v1.workflow;

import com.dotcms.rest.ErrorEntity;
import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * Response entity wrapping a list of {@link ContentTypeWorkflowSchemesView}, one entry per
 * content type, each pairing a content type identifier with its associated workflow schemes.
 * Used by the {@code GET /api/v1/workflow/contenttypes/schemes} endpoint.
 */
public class ResponseEntityContentTypeWorkflowSchemesView extends
        ResponseEntityView<List<ContentTypeWorkflowSchemesView>> {

    /**
     * @param contentTypeWorkflowSchemesView list of content-type/scheme associations to return
     */
    public ResponseEntityContentTypeWorkflowSchemesView(
            final List<ContentTypeWorkflowSchemesView> contentTypeWorkflowSchemesView) {
        super(contentTypeWorkflowSchemesView);
    }

    /**
     * @param errors                         list of identifiers that could not be resolved
     * @param contentTypeWorkflowSchemesView list of content-type/scheme associations to return
     */
    public ResponseEntityContentTypeWorkflowSchemesView(
            final List<ErrorEntity> errors,
            final List<ContentTypeWorkflowSchemesView> contentTypeWorkflowSchemesView) {
        super(errors, contentTypeWorkflowSchemesView);
    }
}