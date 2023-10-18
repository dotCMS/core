package com.dotcms.rendering.js.proxy;

import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import org.graalvm.polyglot.HostAccess;

import java.io.Serializable;

/**
 * Encapsulates a {@link com.dotmarketing.portlets.templates.design.bean.ContainerUUID} in a Js context.
 * @author jsanca
 */
public class JsContainerUUID implements Serializable, JsProxyObject<ContainerUUID> {

   private final ContainerUUID  containerUUID;

    public JsContainerUUID(final ContainerUUID containerUUID) {
        this.containerUUID = containerUUID;
    }


    @HostAccess.Export
    @Override
    public ContainerUUID getWrappedObject() {
        return this.containerUUID;
    }

    @HostAccess.Export
    public String getIdentifier() {
        return containerUUID.getIdentifier();
    }

    @HostAccess.Export
    public String getUUID() {
        return containerUUID.getUUID();
    }

    @HostAccess.Export
    @Override
    public String toString() {
       return containerUUID.toString();
    }
}
