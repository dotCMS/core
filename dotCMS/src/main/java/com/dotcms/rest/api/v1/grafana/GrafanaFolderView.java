package com.dotcms.rest.api.v1.grafana;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.grafana.client.dto.GrafanaFolder;

import java.util.List;

/**
 * Response view for Grafana folders.
 *
 * This class extends ResponseEntityView to provide consistent response formatting
 * for folder operations in the Grafana REST API.
 */
public class GrafanaFolderView extends ResponseEntityView<List<GrafanaFolder>> {

    public GrafanaFolderView(List<GrafanaFolder> entity) {
        super(entity);
    }
}