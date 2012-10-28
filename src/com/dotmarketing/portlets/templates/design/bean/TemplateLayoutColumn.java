package com.dotmarketing.portlets.templates.design.bean;

import java.util.List;

/**
 * Created by Jonathan Gamba
 * Date: 10/25/12
 */
public class TemplateLayoutColumn {

    public static String TYPE_BODY = "body";//Main body column
    public static String TYPE_COLUMN = "column";//Normal column inside a row
    public static String TYPE_SIDEBAR = "sidebar";//Sidebar column

    //ONLY FOR SIDEBARS!!!
    public static String LOCATION_RIGHT = "right";
    public static String LOCATION_LEFT = "left";
    //ONLY FOR SIDEBARS!!!

    public String type;
    public String location;//ONLY FOR SIDEBARS!!!
    public String container;
    public Integer widthPercent;
    public Integer width;

    public List<TemplateLayoutRow> columnRows;

    public String getContainer () {
        return container;
    }

    public void setContainer ( String container ) {
        this.container = container;
    }

    public Integer getWidthPercent () {
        return widthPercent;
    }

    public void setWidthPercent ( Integer widthPercent ) {
        this.widthPercent = widthPercent;
    }

    public Integer getWidth () {
        return width;
    }

    public void setWidth ( Integer width ) {
        this.width = width;
    }

    public String getType () {
        return type;
    }

    public void setType ( String type ) {
        this.type = type;
    }

    public String getLocation () {
        return location;
    }

    public void setLocation ( String location ) {
        this.location = location;
    }

    public List<TemplateLayoutRow> getColumnRows () {
        return columnRows;
    }

    public void setColumnRows ( List<TemplateLayoutRow> columnRows ) {
        this.columnRows = columnRows;
    }

    public Boolean isSidebar () {
        return this.type != null && this.type.equals( TYPE_SIDEBAR );
    }

}