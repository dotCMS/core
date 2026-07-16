package com.dotcms.rest.api.v1.page;

import com.dotcms.rest.ResponseEntityView;

import com.dotmarketing.util.PaginatedArrayList;
import java.util.List;
import java.util.Map;

/**
 * Entity View for multitree structure responses.
 * Contains the relationships between pages, containers, and contentlets.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityPaginatedArrayListMapView extends ResponseEntityView<PaginatedArrayList<Map<String, Object>>> {

    public ResponseEntityPaginatedArrayListMapView(PaginatedArrayList<Map<String, Object>> entity) {
        super(entity);
    }
}