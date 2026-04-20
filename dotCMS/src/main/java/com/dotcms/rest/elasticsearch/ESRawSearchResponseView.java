package com.dotcms.rest.elasticsearch;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * OpenAPI schema view for the POST /api/es/raw 200 response.
 * Represents the unprocessed Elasticsearch SearchResponse returned directly
 * without contentlet hydration. This class exists only as an annotation target.
 */
@Schema(description = "Raw Elasticsearch SearchResponse returned without contentlet processing")
public class ESRawSearchResponseView {

    @Schema(description = "Hit results including total count and individual hit documents")
    public Map<String, Object> hits;

    @Schema(description = "Query execution time in milliseconds")
    public long took;

    @Schema(description = "Whether the search timed out")
    public boolean timed_out;

    @Schema(description = "Aggregation results keyed by aggregation name")
    public Map<String, Object> aggregations;

    @Schema(description = "Term suggestion results keyed by suggestion name")
    public Map<String, Object> suggest;

    @Schema(description = "Shard execution statistics (_total, _successful, _failed)")
    public Map<String, Object> _shards;
}
