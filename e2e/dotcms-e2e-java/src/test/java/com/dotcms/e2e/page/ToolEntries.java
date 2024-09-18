package com.dotcms.e2e.page;

public enum ToolEntries {

    GETTING_WELCOME("[href='#/starter']"),
    // Content group locators
    CONTENT_SEARCH("[href='#/c/content']"),
    CONTENT_PAGES("[href='#/pages']"),
    CONTENT_ACTIVITIES("[href='#/c/c_Activities']"),
    CONTENT_BANNER("[href='#/c/c_Banners']"),
    //Site group locators
    SITE_BROWSER("[href='#/c/site-browser']"),
    LAYOUT_CONTAINERS("[href='#/containers']");

    private final String tool;

    ToolEntries(final String tool) {
        this.tool = tool;
    }

    /**
     * Get the tool entries
     *
     * @return the tool entries
     */
    public String getTool() {
        return tool;
    }

}
