package com.dotcms.rest.api.v1.template;

import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class TemplateLayoutColumnView {

    private final List<ContainerUUID> containers;
    private final int width;
    private final int leftOffset;
    private final String styleClass;

    @JsonCreator
    public TemplateLayoutColumnView(@JsonProperty("containers") final List<ContainerUUID> containers,
                                    @JsonProperty("width")      final int width,
                                    @JsonProperty("leftOffset") final int leftOffset,
                                    @JsonProperty("styleClass") final String styleClass) {

        this.containers = containers;
        this.width      = width;
        this.leftOffset = leftOffset;
        this.styleClass = styleClass;
    }

    public List<ContainerUUID> getContainers() {
        return containers;
    }

    public int getWidth() {
        return width;
    }

    public int getLeftOffset() {
        return leftOffset;
    }

    public String getStyleClass() {
        return styleClass;
    }
}
