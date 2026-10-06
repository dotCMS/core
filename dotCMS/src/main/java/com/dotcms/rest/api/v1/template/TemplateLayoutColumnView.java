package com.dotcms.rest.api.v1.template;

import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class TemplateLayoutColumnView {

    private final List<ContainerUUID> containers;
    private final int width;
    private final int leftOffset;
    private final String styleClass;
    private final Map<String, Object> metadata;

    @JsonCreator
    public TemplateLayoutColumnView(@JsonProperty("containers") final List<ContainerUUID> containers,
                                    @JsonProperty("width")      final int width,
                                    @JsonProperty("leftOffset") final int leftOffset,
                                    @JsonProperty("styleClass") final String styleClass,
                                    @JsonProperty("metadata")   final Map<String, Object> metadata) {

        this.containers = containers;
        this.width      = width;
        this.leftOffset = leftOffset;
        this.styleClass = styleClass;
        this.metadata   = metadata;
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

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
