package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response wrapper for the thread-dump endpoint.
 *
 * @author hassandotcms
 */
public class ResponseEntityThreadDumpView extends ResponseEntityView<ThreadDumpView> {

    public ResponseEntityThreadDumpView(final ThreadDumpView entity) {
        super(entity);
    }
}
