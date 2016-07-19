package com.dotcms.rest.api.v1.menu;

/**
 * Created by freddyrodriguez on 18/5/16.
 */
public class MenuItem {

    private String url;
    private String id;
    private String name;
    private boolean angular = false;
    private boolean isAjax = false;

    /**
     * Generate a Submenu portlet
     * @param id  Portlet id
     * @param url Portlet url
     * @param name Portlet name
     * @param isAngular if the portlet is an PortletController portlet 
     * @param isAjax if the portlet is an BaseRestPortlet portlet
     */
    public MenuItem(String id, String url, String name, boolean isAngular, boolean isAjax) {
        this.url = url;
        this.id = id;
        this.name = name;
        this.angular = isAngular;
        this.isAjax = isAjax;
    }

    /**
     * Get the sub menu portlet url
     * @return the portlet url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Get the sub menu portlet id
     * @return the portlet  id
     */
    public String getId() {
        return id;
    }

    /**
     * Get the sub menu portlet name
     * @return the portlet  name
     */
    public String getName() {
        return name;
    }

    /**
     * Return true if the portlet is an PortletController portlet
     * @return true is is a PortletController portlet, false if not
     */
    public boolean isAngular() {
        return angular;
    }

    /**
     * Return true if the portlet is an BaseRestPortlet portlet
     * @return true is is a BaseRestPortlet portlet, false if not
     */
    public boolean isAjax() {
        return isAjax;
    }
}
