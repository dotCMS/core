package com.dotcms.rest.api.v1.grafana;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.grafana.client.dto.DashboardDetail;

/**
 * Response view for Grafana dashboard details.
 *
 * This class extends ResponseEntityView to provide consistent response formatting
 * for dashboard detail operations in the Grafana REST API.
 */
public class GrafanaDashboardDetailView extends ResponseEntityView<DashboardDetail> {

    public GrafanaDashboardDetailView(DashboardDetail entity) {
        super(entity);
    }
}