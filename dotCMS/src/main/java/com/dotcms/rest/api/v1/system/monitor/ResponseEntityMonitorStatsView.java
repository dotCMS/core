package com.dotcms.rest.api.v1.system.monitor;

import com.dotcms.rest.CountView;
import com.dotcms.rest.ResponseEntityView;

/**
 * This class encapsulates the {@link javax.ws.rs.core.Response} object to include the expected MonitorStats
 * @author sbolton
 */
public class ResponseEntityMonitorStatsView extends ResponseEntityView<MonitorStats> {
    public ResponseEntityMonitorStatsView(final MonitorStats entity) {
        super(entity);
    }
}
