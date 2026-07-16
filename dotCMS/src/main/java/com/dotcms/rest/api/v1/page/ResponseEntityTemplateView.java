package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.templates.model.Template;

/**
 * Entity View for template responses.
 * Contains individual template information including layout and metadata.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityTemplateView extends ResponseEntityView<Template> {
    public ResponseEntityTemplateView(final Template entity) {
        super(entity);
    }
}