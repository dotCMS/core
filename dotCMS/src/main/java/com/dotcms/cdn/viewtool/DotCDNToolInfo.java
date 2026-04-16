package com.dotcms.cdn.viewtool;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.servlet.ServletToolInfo;

public class DotCDNToolInfo extends ServletToolInfo {

    @Override
    public String getKey() {
        return "dotcdn";
    }

    @Override
    public String getScope() {
        return ViewContext.REQUEST;
    }

    @Override
    public String getClassname() {
        return DotCDNTool.class.getName();
    }

    @Override
    public Object getInstance(Object initData) {
        DotCDNTool viewTool = new DotCDNTool();
        viewTool.init(initData);
        setScope(ViewContext.REQUEST);
        return viewTool;
    }
}
