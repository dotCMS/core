package com.dotcms.rendering.js.proxy;

import com.dotmarketing.portlets.templates.design.bean.Body;
import org.graalvm.polyglot.HostAccess;

import java.io.Serializable;
import java.util.stream.Collectors;

/**
 * Encapsulates a {@link com.dotmarketing.portlets.templates.design.bean.Body} in a Js context.
 * @author jsanca
 */
public class JsBody implements Serializable, JsProxyObject<Body> {

    private final Body body;

    public JsBody(final Body body) {
        this.body = body;
    }

    @HostAccess.Export
    public Object getRows() {

        return JsProxyFactory.createProxy(this.body.getRows().stream()
                .map(JsTemplateLayoutRow::new).collect(Collectors.toList()));
    }

    @HostAccess.Export
    @Override
    public String toString() {
       return this.body.toString();
    }

    @Override
    public Body getWrappedObject() {
        return this.body;
    }
}
