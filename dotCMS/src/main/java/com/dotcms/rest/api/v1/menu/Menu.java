package com.dotcms.rest.api.v1.menu;

import java.util.List;

/**
 * Created by freddyrodriguez on 18/5/16.
 */
public class Menu {

    private String tabName;
    private String tabIcon;
    private List<MenuItem> menuItems;
    private String url;

    /**
     * Generate a new menu Tab element
     * @param tabName Name of the tab
     * @param tabDescription Description of the tab
     * @param url 
     */
    public Menu(String tabName, String tabIcon, String url) {
        this.tabName = tabName;
        this.tabIcon = tabIcon;
        this.url = url;
    }

    /**
     * Get the Tab Name
     * @return the tab name
     */
    public String getTabName() {
        return tabName;
    }

    /**
     * Get the Tab Icon
     * @return the tab icon
     */
    public String getTabIcon() {
        return tabIcon;
    }

    /**
     * Set the Tab list of menu portlets items
     */
    public void setMenuItems(List<MenuItem> menuItems) {
        this.menuItems = menuItems;
    }

    /**
     * Get the Tab list of menu portlet items
     * @return the list of menu portlet items
     */
    public List<MenuItem> getMenuItems() {
        return menuItems;
    }

    /**
     * Get the Url of the main menu portlet item for this tab
     * @return
     */
    public String getUrl() {
        return url;
    }
}
