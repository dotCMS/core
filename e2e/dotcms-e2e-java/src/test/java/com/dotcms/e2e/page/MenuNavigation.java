package com.dotcms.e2e.page;

import com.microsoft.playwright.Page;

/**
 * Class for navigating through the menu.
 *
 * This class provides methods to navigate to different group and tool entries in the menu.
 *
 * @author vico
 */
public class MenuNavigation {

    private final Page menuNavigation;

    public MenuNavigation(final Page page) {
        this.menuNavigation = page;
    }

    /**
     * Navigate to the given group and tool entries
     *
     * @param groupEntries the group entries to navigate to
     * @param toolEntries  the tool entries to navigate to
     */
    public void navigateTo(final GroupEntries groupEntries, final ToolEntries toolEntries) {
        menuNavigation.locator(groupEntries.getGroup()).hover();
        menuNavigation.locator(toolEntries.getTool()).click();
    }

}
