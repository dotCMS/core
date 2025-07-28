package com.dotcms.rest.api.v1.system.logger;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for single logger responses.
 * Contains logger configuration and level information.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityLoggerView extends ResponseEntityView<LoggerView> {
    public ResponseEntityLoggerView(final LoggerView entity) {
        super(entity);
    }
}