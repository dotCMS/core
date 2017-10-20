package com.dotcms.rest.api.v1.menu;

import com.dotmarketing.business.Layout;

import java.util.List;

/**
 * Created by freddyrodriguez on 18/5/16.
 */
public class Menu {

    private String tabName;
    private String tabIcon;
    private List<MenuItem> menuItems;
    private String url;
    private Layout layout;

    /**
     * Generate a new menu Tab element
     * @param tabName Name of the tab
     * @param layout layout link to the Tab Menu
     * @param url 
     */
    public Menu(String tabName, String tabIcon, String url, Layout layout) {
        this.tabName = tabName;
        this.tabIcon = tabIcon;
        this.url = url;
        this.layout = layout;
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

    /**
     * Return layout id
     * @return
     */
    public String getId() {
        return layout.getId();
    }

    /**
     * Return layout name
     * @return
     */
    public String getName() {
        return layout.getName();
    }
}
