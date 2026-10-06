package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response wrapper for the bulk delete contentlets endpoint.
 *
 * @author hassandotcms
 */
public class ResponseEntityDeleteContentletsResultView extends ResponseEntityView<DeleteContentletsResultView> {

    public ResponseEntityDeleteContentletsResultView(final DeleteContentletsResultView entity) {
        super(entity);
    }
}
