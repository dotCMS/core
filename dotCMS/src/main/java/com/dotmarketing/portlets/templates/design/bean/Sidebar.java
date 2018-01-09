package com.dotmarketing.portlets.templates.design.bean;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * It's a {@link com.dotmarketing.portlets.templates.model.Template}'s Sidebar
 */
public class Sidebar extends ContainerHolder {

    public static String LOCATION_RIGHT = "right";
    public static String LOCATION_LEFT = "left";

    private String location;
    private Integer widthPercent;
    private Integer width;

    @JsonCreator
    public Sidebar(@JsonProperty("containers") List<ContainerUUID> containers,
                   @JsonProperty("location") final String location,
                   @JsonProperty("width") final int width,
                   @JsonProperty("widthPercent") final int widthPercent) {
        super(containers);

        this.location = location;
        this.widthPercent = widthPercent;
        this.width = width;
    }

    public String getLocation() {
        return location;
    }

    public Integer getWidthPercent() {
        return widthPercent;
    }

    public Integer getWidth() {
        return width;
    }
}
