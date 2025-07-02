package com.dotcms.rest.api.v1.site;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;
import java.util.Map;

/**
 * Entity View for site thumbnails collection responses.
 * Contains list of site thumbnail information.
 * 
 * @author Steve Bolton
 */
public class ResponseEntitySiteThumbnailsView extends ResponseEntityView<List<Map<String, Object>>> {
    public ResponseEntitySiteThumbnailsView(final List<Map<String, Object>> entity) {
        super(entity);
    }
}
