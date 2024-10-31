package com.dotcms.telemetry.rest;

import com.dotcms.telemetry.MetricsSnapshot;
import com.dotcms.rest.ResponseEntityView;

/**
 * {@link ResponseEntityView} of {@link MetricsSnapshot}
 */
public class ResponseEntityMetricsSnapshotView extends ResponseEntityView<MetricsSnapshot> {

    public ResponseEntityMetricsSnapshotView(final MetricsSnapshot entity) {
        super(entity);
    }
}