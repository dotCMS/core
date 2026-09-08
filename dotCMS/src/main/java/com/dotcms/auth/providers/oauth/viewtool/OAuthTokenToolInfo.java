package com.dotcms.auth.providers.oauth.viewtool;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.servlet.ServletToolInfo;

public class OAuthTokenToolInfo extends ServletToolInfo {

    @Override
    public String getKey() {
        return "oauthToken";
    }

    @Override
    public String getScope() {
        return ViewContext.REQUEST;
    }

    @Override
    public String getClassname() {
        return OAuthTokenTool.class.getName();
    }

    @Override
    public Object getInstance(final Object initData) {
        final OAuthTokenTool tool = new OAuthTokenTool();
        tool.init(initData);
        setScope(ViewContext.REQUEST);
        return tool;
    }
}
