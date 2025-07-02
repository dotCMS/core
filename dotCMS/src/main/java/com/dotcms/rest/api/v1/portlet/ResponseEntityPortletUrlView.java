package com.dotcms.rest.api.v1.portlet;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for portlet URL responses.
 * Contains URLs for content creation and portlet actions.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityPortletUrlView extends ResponseEntityView<String> {
    public ResponseEntityPortletUrlView(final String entity) {
        super(entity);
    }
}