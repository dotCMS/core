package com.dotcms.rest.api.v1.template;

import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;

import java.util.List;

public class SidebarView {

    private final List<ContainerUUID> containers;
    private final String location;
    private final String width;

    public SidebarView(final List<ContainerUUID> containers,
                       final String location,
                       final String width) {

        this.containers = containers;
        this.location = location;
        this.width = width;
    }

    public List<ContainerUUID> getContainers() {
        return containers;
    }

    public String getLocation() {
        return location;
    }

    public String getWidth() {
        return width;
    }

}
