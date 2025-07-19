package com.dotcms.rest.api.v1.template;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for template responses.
 * Contains individual template information including layout, design, and metadata.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityTemplateView extends ResponseEntityView<TemplateView> {
    public ResponseEntityTemplateView(final TemplateView entity) {
        super(entity);
    }
}