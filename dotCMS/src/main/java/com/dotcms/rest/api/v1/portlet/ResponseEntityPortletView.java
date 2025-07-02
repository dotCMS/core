package com.dotcms.rest.api.v1.portlet;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for portlet operation responses.
 * Contains portlet information and operation results.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityPortletView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityPortletView(final Map<String, Object> entity) {
        super(entity);
    }
}