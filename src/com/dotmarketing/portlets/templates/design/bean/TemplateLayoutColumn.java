package com.dotmarketing.portlets.templates.design.bean;

/**
 * Created by Jonathan Gamba
 * Date: 10/25/12
 */
public class TemplateLayoutColumn {

    public String container;
    public Integer widthPercent;

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

}