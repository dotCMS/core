package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response wrapper for the kill-all-sessions endpoint.
 *
 * @author hassandotcms
 */
public class ResponseEntityKillSessionsResultView extends ResponseEntityView<KillSessionsResultView> {

    public ResponseEntityKillSessionsResultView(final KillSessionsResultView entity) {
        super(entity);
    }
}
