package com.dotcms.rest.api.v1.workflow;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;

public class ResponseEntityContentTypeWorkflowSchemesView extends
        ResponseEntityView<List<ContentTypeWorkflowSchemesView>> {

    public ResponseEntityContentTypeWorkflowSchemesView(
            final List<ContentTypeWorkflowSchemesView> contentTypeWorkflowSchemesView) {
        super(contentTypeWorkflowSchemesView);
    }
}