package com.dotcms.e2e.page;

/**
 * Enum representing group entries for different menus and layouts.
 * This enum provides constants for various group entries and a method to retrieve the group string.
 *
 * @author vico
 */
public enum GroupEntries {

    CONTENT_MENU(":text('format_align_leftContentarrow_drop_up')"),
    SITE_MENU(":text('folder_openSitearrow_drop_up')"),
    SITE_LAYOUT(":text('folder_openSite')");

    private final String group;

    GroupEntries(final String group) {
        this.group = group;
    }

    /**
     * Retrieves the group string.
     *
     * @return the group string
     */
    public String getGroup() {
        return group;
    }

}
