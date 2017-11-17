package com.dotmarketing.portlets.templates.design.bean;

import java.util.List;

/**
 * Created by freddyrodriguez on 11/13/17.
 */
public class Sidebar extends ContainerHolder {

    //ONLY FOR SIDEBARS!!!
    public static String LOCATION_RIGHT = "right";
    public static String LOCATION_LEFT = "left";
    //ONLY FOR SIDEBARS!!!

    public String location;
    public Integer widthPercent;
    public Integer width;

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Integer getWidthPercent() {
        return widthPercent;
    }

    public void setWidthPercent(Integer widthPercent) {
        this.widthPercent = widthPercent;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }
}
