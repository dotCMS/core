package com.dotcms.auth.providers.oauth.viewtool;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.servlet.ServletToolInfo;

public class OAuthToolInfo extends ServletToolInfo {

    @Override
    public String getKey() {
        return "oauth";
    }

    @Override
    public String getScope() {
        return ViewContext.REQUEST;
    }

    @Override
    public String getClassname() {
        return OAuthTool.class.getName();
    }

    @Override
    public Object getInstance(final Object initData) {
        final OAuthTool tool = new OAuthTool();
        tool.init(initData);
        setScope(ViewContext.REQUEST);
        return tool;
    }
}
