package com.dotcms.rest.api.v1.osgi;

import com.dotcms.rest.ResponseEntityView;

import java.util.List;

/**
 * Encapsulates a list of bundle maps
 * @author jsanca
 */
public class ResponseEntityBundleListView extends ResponseEntityView<List<BundleMap>> {

    public ResponseEntityBundleListView(final List<BundleMap> entity) {
        super(entity);
    }

}
