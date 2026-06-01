package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.rest.ResponseEntityView;

import java.util.List;

/**
 * Response wrapper for the list-active-sessions endpoint.
 *
 * @author hassandotcms
 */
public class ResponseEntitySessionListView extends ResponseEntityView<List<SessionView>> {

    public ResponseEntitySessionListView(final List<SessionView> entity) {
        super(entity);
    }
}
