package com.dotcms.rest.api.v1.grafana;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.grafana.client.dto.DashboardSearchResult;

import java.util.List;

/**
 * Response view for Grafana dashboard search results.
 *
 * This class extends ResponseEntityView to provide consistent response formatting
 * for dashboard search operations in the Grafana REST API.
 */
public class GrafanaDashboardSearchResultView extends ResponseEntityView<List<DashboardSearchResult>> {

    public GrafanaDashboardSearchResultView(List<DashboardSearchResult> entity) {
        super(entity);
    }
}