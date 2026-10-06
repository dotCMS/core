package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.rest.ResponseEntityView;

import java.util.Map;

/**
 * Response wrapper for the delete Quartz system job endpoint.
 *
 * @author hassandotcms
 */
public class ResponseEntitySystemJobDeleteView extends ResponseEntityView<Map<String, Object>> {

    public ResponseEntitySystemJobDeleteView(final Map<String, Object> entity) {
        super(entity);
    }
}
