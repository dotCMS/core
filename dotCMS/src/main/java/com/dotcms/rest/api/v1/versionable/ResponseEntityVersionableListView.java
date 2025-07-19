package com.dotcms.rest.api.v1.versionable;

import com.dotcms.rest.ResponseEntityView;
import java.util.List;

/**
 * Entity View for versionable collection responses.
 * Contains list of VersionableView entities.
 * 
 * @author Steve Bolton
 */
public class ResponseEntityVersionableListView extends ResponseEntityView<List<VersionableView>> {
    public ResponseEntityVersionableListView(final List<VersionableView> entity) {
        super(entity);
    }
}
