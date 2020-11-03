package com.dotcms.rest.api.v1.template;

import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SidebarView {

    private final List<ContainerUUID> containers;
    private final String location;
    private final String width;

    @JsonCreator
    public SidebarView(@JsonProperty("containers") final List<ContainerUUID> containers,
                       @JsonProperty("location")   final String location,
                       @JsonProperty("width")      final String width) {

        this.containers = containers;
        this.location   = location;
        this.width      = width;
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
