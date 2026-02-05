package com.dotcms.rest.api.v1.menu;

/**
 * Created by freddyrodriguez on 18/5/16.
 */
public class MenuItem {

    private String url;
    private String id;
    private String label;
    private boolean angular = false;
    private boolean isAjax = false;
    private String angularModule;

    /**
     * Generate a Submenu portlet
     * @param id  Portlet id
     * @param url Portlet url
     * @param label Portlet label
     * @param isAngular if the portlet is an PortletController portlet
     * @param isAjax if the portlet is an BaseRestPortlet portlet
     */
    public MenuItem(String id, String url, String label, boolean isAngular, boolean isAjax) {
        this(id, url, label, isAngular, isAjax, null);
    }

    /**
     * Generate a Submenu portlet with dynamic Angular module loading support
     *
     * @param id            Portlet id
     * @param url           Portlet url
     * @param label         Portlet label
     * @param isAngular     if the portlet is an PortletController portlet
     * @param isAjax        if the portlet is an BaseRestPortlet portlet
     * @param angularModule the Angular module path for dynamic lazy loading (e.g., "@dotcms/portlets/my-custom")
     */
    public MenuItem(String id, String url, String label, boolean isAngular, boolean isAjax, String angularModule) {
        this.url = url;
        this.id = id;
        this.label = label;
        this.angular = isAngular;
        this.isAjax = isAjax;
        this.angularModule = angularModule;
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
    public String getLabel() {
        return label;
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

    /**
     * Get the Angular module path for dynamic lazy loading. This is used by the frontend to dynamically import and
     * register the portlet's Angular module.
     *
     * @return the Angular module path (e.g., "@dotcms/portlets/my-custom"), or null if not set
     */
    public String getAngularModule() {
        return angularModule;
    }
}
