package com.dotcms.rest.api.v1.index;

import com.dotcms.rest.ResponseEntityView;
import java.util.Map;

/**
 * Entity View for Elasticsearch cluster and index statistics responses.
 * Contains statistical information about cluster health, node status, and index metrics.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityIndexStatsView extends ResponseEntityView<Map<String, Object>> {
    public ResponseEntityIndexStatsView(final Map<String, Object> entity) {
        super(entity);
    }
}
