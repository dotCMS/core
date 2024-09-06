package com.dotcms.ai.viewtool;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.servlet.ServletToolInfo;

/**
 * AIToolInfo is a class that extends ServletToolInfo.
 * It provides methods to get the key, scope, classname and to get an instance of AIViewTool.
 */
public class AIToolInfo extends ServletToolInfo {

    @Override
    public String getKey () {
        return "ai";
    }

    @Override
    public String getScope () {
        return ViewContext.REQUEST;
    }

    @Override
    public String getClassname () {
        return AIViewTool.class.getName();
    }

    @Override
    public Object getInstance (final Object initData) {
        final AIViewTool viewTool = new AIViewTool();
        viewTool.init(initData);

        setScope( ViewContext.REQUEST );

        return viewTool;
    }

}
