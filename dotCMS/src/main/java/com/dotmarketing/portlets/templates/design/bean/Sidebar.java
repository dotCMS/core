package com.dotmarketing.portlets.templates.design.bean;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * It's a {@link com.dotmarketing.portlets.templates.model.Template}'s Sidebar
 */
public class Sidebar extends ContainerHolder implements Serializable{

    public static String LOCATION_RIGHT = "right";
    public static String LOCATION_LEFT = "left";

    private String location;
    private Integer widthPercent;
    private SidebarWidthValue width;

    @JsonCreator
    public Sidebar(@JsonProperty("containers") List<ContainerUUID> containers,
                   @JsonProperty("location") final String location,
                   @JsonProperty("width") final String width,
                   @JsonProperty("widthPercent") final Integer widthPercent) {
        super(containers);

        this.location = location;
        this.widthPercent = widthPercent;

        if (width != null) {
            this.width = SidebarWidthValue.fromString(width);
            if (null == widthPercent || -1 == widthPercent) {

                this.widthPercent = this.width.getWidthPercent();
            }
        } else {
            this.width = this.getWidthFromWidthPercent();
        }
    }

    public String getLocation() {
        return location;
    }

    public Integer getWidthPercent() {
        return widthPercent != null ? widthPercent : width.getWidthPercent();
    }

    public String getWidth() {
        return width.name().toLowerCase();
    }

    private SidebarWidthValue getWidthFromWidthPercent() {
        return (widthPercent <= 20 ? SidebarWidthValue.SMALL :
                widthPercent <= 30 ? SidebarWidthValue.MEDIUM : SidebarWidthValue.LARGE);
    }

    @Override
    public String toString() {
       try {
           return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return super.toString();
        }
    }
}
