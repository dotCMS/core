package com.dotcms.ai.viewtool;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.servlet.ServletToolInfo;

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

        AIViewTool viewTool = new AIViewTool(initData);

        setScope( ViewContext.REQUEST );

        return viewTool;
    }

}
