package com.dotcms.rest.api.v1.system;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for upgrade task operation responses.
 * Contains upgrade task operation confirmation messages.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityUpgradeTaskView extends ResponseEntityView<String> {
    public ResponseEntityUpgradeTaskView(final String entity) {
        super(entity);
    }
}
