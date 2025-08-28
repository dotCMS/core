package com.dotcms.rest.api.v1.versionable;

import com.dotcms.rest.ResponseEntityView;

/**
 * Entity View for single versionable responses.
 * Contains VersionableView entity data.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityVersionableView extends ResponseEntityView<VersionableView> {
    public ResponseEntityVersionableView(final VersionableView entity) {
        super(entity);
    }
}
