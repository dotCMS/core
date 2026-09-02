package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.rest.ResponseEntityView;

import java.util.List;
import java.util.Map;

/**
 * Response wrapper for the list Quartz system jobs endpoint.
 *
 * @author hassandotcms
 */
public class ResponseEntitySystemJobListView extends ResponseEntityView<List<Map<String, Object>>> {

    public ResponseEntitySystemJobListView(final List<Map<String, Object>> entity) {
        super(entity);
    }
}
