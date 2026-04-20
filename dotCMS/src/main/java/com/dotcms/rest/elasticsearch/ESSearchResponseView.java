package com.dotcms.rest.elasticsearch;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;

/**
 * OpenAPI schema view for the POST /api/es/search 200 response.
 * The actual response is built dynamically from ESSearchResults and is not a
 * serializable entity, so this class exists only as an annotation target.
 */
@Schema(description = "Elasticsearch search results containing processed contentlets and raw ES response metadata")
public class ESSearchResponseView {

    @Schema(description = "Processed contentlet objects matching the query. Each object contains the contentlet's fields, identifier, inode, and type information.")
    public List<Map<String, Object>> contentlets;

    @Schema(description = "Raw Elasticsearch SearchResponse metadata array. Contains hits total, query timing (took), aggregations, suggestions, and scroll ID.")
    public List<Map<String, Object>> esresponse;
}
