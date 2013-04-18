package com.dotmarketing.portlets.templates.design.bean;

import com.dotmarketing.portlets.templates.design.util.PreviewTemplateUtil;

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

    public boolean preview;
    public String type;
    public String location;//ONLY FOR SIDEBARS!!!
    public List<String> containers;
    public Integer widthPercent;
    public Integer width;

    public List<TemplateLayoutRow> rows;

    public List<String> getContainers () {
        return containers;
    }

    public void setContainers ( List<String> containers ) {
        this.containers = containers;
    }

    public boolean isPreview () {
        return preview;
    }

    public void setPreview ( boolean preview ) {
        this.preview = preview;
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

    public List<TemplateLayoutRow> getRows () {
        return rows;
    }

    public void setRows ( List<TemplateLayoutRow> rows ) {
        this.rows = rows;
    }

    public Boolean isSidebar () {
        return this.type != null && this.type.equals( TYPE_SIDEBAR );
    }

    public String draw () throws Exception {

        StringBuffer sb = new StringBuffer();
        if ( this.containers != null ) {
            for ( String container: this.containers ) {

                if ( this.preview ) {
                    sb.append( PreviewTemplateUtil.getMockBodyContent() );
                } else {
                    sb.append( "#parseContainer('" ).append( container ).append( "')" );
                }
            }
        }

        return sb.toString();
    }

}