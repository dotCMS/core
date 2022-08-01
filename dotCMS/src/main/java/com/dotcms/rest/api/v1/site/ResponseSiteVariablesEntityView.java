package com.dotcms.rest.api.v1.site;

import com.dotcms.rest.ResponseEntityView;
import com.dotmarketing.portlets.hostvariable.model.HostVariable;

import java.util.List;

public class ResponseSiteVariablesEntityView extends ResponseEntityView<List<SiteVariableView>> {


    public ResponseSiteVariablesEntityView(final List<SiteVariableView> entity) {
        super(entity);
    }
}
