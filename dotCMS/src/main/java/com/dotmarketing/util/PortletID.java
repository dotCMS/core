package com.dotmarketing.util;

/**
 * Set of Portlet constants
 */
public enum PortletID {

    SITE_BROWSER;

    private String url;

    private PortletID(){
        url = this.name().toLowerCase().replace("_", "-");
    }

    private PortletID(String url){
        this.url = url;
    }

    @Override
    public String toString() {
        return url;
    }
}
