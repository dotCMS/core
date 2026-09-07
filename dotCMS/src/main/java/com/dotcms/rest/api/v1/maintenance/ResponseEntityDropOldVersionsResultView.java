package com.dotcms.rest.api.v1.maintenance;

import com.dotcms.rest.ResponseEntityView;

/**
 * Response wrapper for the drop old versions endpoint.
 *
 * @author hassandotcms
 */
public class ResponseEntityDropOldVersionsResultView extends ResponseEntityView<DropOldVersionsResultView> {

    public ResponseEntityDropOldVersionsResultView(final DropOldVersionsResultView entity) {
        super(entity);
    }
}
