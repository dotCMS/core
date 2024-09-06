package com.dotcms.rendering.js.viewtools;

import com.dotcms.rendering.js.JsViewTool;
import com.dotcms.rendering.js.proxy.JsRequest;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.web.WebAPILocator;
import org.graalvm.polyglot.HostAccess;

public class SiteJsViewTool implements JsViewTool {
    @Override
    public String getName() {
        return "site";
    }

    @Override
    public SCOPE getScope() {
        return SCOPE.REQUEST;
    }

    @HostAccess.Export
    public String getCurrentSiteId(final JsRequest request) {

        final Host currentHost = WebAPILocator.getHostWebAPI().getHost(request.getRequest());
        return currentHost.getIdentifier();
    }
}
