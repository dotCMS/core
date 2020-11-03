package com.dotcms.rest.api.v1.template;

import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;

import java.util.List;

public class TemplateLayoutColumnView {

    private final List<ContainerUUID> containers;
    private final int width;
    private final int leftOffset;
    private final String styleClass;

    public TemplateLayoutColumnView(final List<ContainerUUID> containers,
                                    final int width,
                                    final int leftOffset,
                                    final String styleClass) {
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
