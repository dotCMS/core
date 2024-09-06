package com.dotcms.rendering.js.proxy;

import com.dotmarketing.portlets.templates.design.bean.Sidebar;
import org.graalvm.polyglot.HostAccess;

import java.io.Serializable;

/**
 * Encapsulates a {@link Sidebar} in a Js context.
 * @author jsanca
 */
public class JsSidebar implements Serializable, JsProxyObject<Sidebar> {

    private final Sidebar sidebar;

    public JsSidebar(final Sidebar sidebar) {
        this.sidebar = sidebar;
    }

    @Override
    public Sidebar getWrappedObject() {
        return this.sidebar;
    }

    @HostAccess.Export
    public String getLocation() {
        return this.sidebar.getLocation();
    }

    @HostAccess.Export
    public Integer getWidthPercent() {
        return this.sidebar.getWidthPercent();
    }

    @HostAccess.Export
    public String getWidth() {
        return this.sidebar.getWidth();
    }

    @HostAccess.Export
    @Override
    public String toString() {
       return this.sidebar.toString();
    }
}
