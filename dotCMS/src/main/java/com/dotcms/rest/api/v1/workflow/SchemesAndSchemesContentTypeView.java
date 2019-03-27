package com.dotcms.rest.api.v1.workflow;

import com.dotmarketing.portlets.workflows.model.WorkflowScheme;
import java.util.List;

/**
 * View just to encapsulate all schemes and all schemes for the content type
 * @author jsanca
 */
public class SchemesAndSchemesContentTypeView {

    private final List<WorkflowScheme> schemes;
    private final List<WorkflowScheme> contentTypeSchemes;

    public SchemesAndSchemesContentTypeView(final List<WorkflowScheme> schemes, final List<WorkflowScheme> contentTypeSchemes) {

        this.schemes = schemes;
        this.contentTypeSchemes = contentTypeSchemes;
    }

    public List<WorkflowScheme> getSchemes() {
        return schemes;
    }

    public List<WorkflowScheme> getContentTypeSchemes() {
        return contentTypeSchemes;
    }
} // E:O:F:SchemesAndSchemesContentTypeView.
