package com.dotcms.rest.api.v1.health;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * Entity View for health check names list.
 * Contains available health check identifiers for system monitoring.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityHealthCheckNamesView extends ResponseEntityView<List<String>> {
    public ResponseEntityHealthCheckNamesView(final List<String> entity) {
        super(entity);
    }
}