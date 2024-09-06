package com.dotcms.rendering.js.proxy;

import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.graalvm.polyglot.HostAccess;

import java.io.Serializable;


/**
 * Encapsulates a {@link TemplateLayout} in a Js context.
 * @author jsanca
 */
public class JsTemplateLayout implements Serializable, JsProxyObject<TemplateLayout> {

    private final TemplateLayout layout;

    public JsTemplateLayout(final TemplateLayout layout) {
        this.layout = layout;
    }

    @Override
    public TemplateLayout getWrappedObject() {
        return this.layout;
    }

    @HostAccess.Export
    public String getPageWidth () {
        return this.layout.getPageWidth();
    }

    @HostAccess.Export
    public String getLayout () {
        return layout.getLayout();
    }

    @HostAccess.Export
    public String getWidth () {
        return this.layout.getWidth();
    }

    @HostAccess.Export
    public String getTitle () {
        return this.layout.getTitle();
    }


    @HostAccess.Export
    public boolean isHeader () {
        return this.layout.isHeader();
    }

    @HostAccess.Export
    public boolean isFooter () {
        return this.layout.isFooter();
    }

    @HostAccess.Export
    public Object getBody () {
        return JsProxyFactory.createProxy(this.layout.getBody());
    }

    @HostAccess.Export
    public Object getSidebar () {
        return  JsProxyFactory.createProxy(this.layout.getSidebar());
    }

    @HostAccess.Export
    @Override
    public String toString() {
        return this.layout.toString();
    }

    @HostAccess.Export
    /**
     * Return true if the container exists into the TemplateLayout
     *
     * @param container container
     * @param uuid Container uuid into the TemplateLayout
     * @return
     */
    public boolean existsContainer(final JsContainer container, final String uuid) {
        return this.layout.existsContainer(container.getWrappedObject(), uuid);
    }

    @HostAccess.Export
    @JsonIgnore
    public Object getContainersIdentifierOrPath() {

        return JsProxyFactory.createProxy(this.layout.getContainersIdentifierOrPath());
    }

    @HostAccess.Export
    @JsonIgnore
    public Object getContainersUUID() {

        return JsProxyFactory.createProxy(this.layout.getContainersUUID());
    }
}
