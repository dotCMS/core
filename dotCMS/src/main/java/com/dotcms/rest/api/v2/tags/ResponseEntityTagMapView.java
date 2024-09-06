package com.dotcms.rest.api.v2.tags;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.tag.RestTag;

import java.util.Map;

/**
 * Response for Tags results
 * @author jsanca
 */
public class ResponseEntityTagMapView extends ResponseEntityView<Map<String, RestTag>> {
    public ResponseEntityTagMapView(final Map<String, RestTag> entity) {
        super(entity);
    }
}
