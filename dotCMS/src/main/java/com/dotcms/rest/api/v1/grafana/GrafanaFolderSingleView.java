package com.dotcms.rest.api.v1.grafana;

import com.dotcms.rest.ResponseEntityView;
import com.dotcms.rest.api.v1.grafana.client.dto.GrafanaFolder;

/**
 * Response view for a single Grafana folder.
 *
 * This class extends ResponseEntityView to provide consistent response formatting
 * for single folder operations in the Grafana REST API.
 */
public class GrafanaFolderSingleView extends ResponseEntityView<GrafanaFolder> {

    public GrafanaFolderSingleView(GrafanaFolder entity) {
        super(entity);
    }
}