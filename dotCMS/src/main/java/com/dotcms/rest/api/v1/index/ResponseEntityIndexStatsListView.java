package com.dotcms.rest.api.v1.index;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;
import java.util.Map;

/**
 * Entity View for index statistics list responses.
 * Contains list of Elasticsearch indices with their detailed statistics and metadata.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityIndexStatsListView extends ResponseEntityView<List<Map<String, Object>>> {
    public ResponseEntityIndexStatsListView(final List<Map<String, Object>> entity) {
        super(entity);
    }
}