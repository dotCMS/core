package com.dotcms.rest.api.v1.theme;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for theme responses.
 * Contains theme configuration and metadata information.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityThemeView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityThemeView(final Map<String, Object> entity) {
        super(entity);
    }
}