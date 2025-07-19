package com.dotcms.rest.api.v1.system.logger;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * Entity View for multiple logger responses.
 * Contains a list of logger configurations and level information.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityLoggerListView extends ResponseEntityView<List<LoggerView>> {
    public ResponseEntityLoggerListView(final List<LoggerView> entity) {
        super(entity);
    }
}