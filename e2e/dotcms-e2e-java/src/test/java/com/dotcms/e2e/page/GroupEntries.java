package com.dotcms.e2e.page;

public enum GroupEntries {

    CONTENT_MENU(":text('format_align_leftContentarrow_drop_up')"),
    SITE_MENU(":text('folder_openSitearrow_drop_up')"),
    SITE_LAYOUT(":text('folder_openSite')");

    private final String group;

    GroupEntries(final String group) {
        this.group = group;
    }

    /**
     * Get the group entries
     *
     * @return the group entries
     */
    public String getGroup() {
        return group;
    }

}
