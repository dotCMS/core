package com.dotcms.rest.api.v1.site;

import com.dotcms.rest.ResponseEntityView;

import java.util.List;

/**
 * Response View for the {@link SiteVariableView}
 * @author jsanca
 */
public class ResponseSiteVariablesEntityView extends ResponseEntityView<List<SiteVariableView>> {


    public ResponseSiteVariablesEntityView(final List<SiteVariableView> entity) {
        super(entity);
    }
}
