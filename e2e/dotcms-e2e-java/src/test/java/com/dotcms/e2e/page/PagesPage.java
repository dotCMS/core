package com.dotcms.e2e.page;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * Page class for interacting with the Pages page.
 *
 * This class provides methods to fill the pages form, find a page by title,
 * and execute workflow actions on a page.
 *
 * @author vico
 */
public class PagesPage {

    private final Page pagesPage;

    public PagesPage(final Page pagesPage) {
        this.pagesPage = pagesPage;
    }

    /**
     * Fill the Pages form with the given data and template and save it as a draft or publish it
     * depending on the action selected (Save/Publish)
     *
     * @param pagesDetailFrame page detail frame locator
     * @param pageTitle        page title
     * @param template         page template
     */
    public void fillPagesForm(final String pagesDetailFrame, final String pageTitle, final String template) {
        pagesPage.frameLocator(pagesDetailFrame).locator("#titleBox").fill(pageTitle);
        pagesPage.frameLocator(pagesDetailFrame).locator("#text60Checkbox").check();
        pagesPage.frameLocator(pagesDetailFrame).locator("#widget_templateSel div").first().click();
        assertThat(pagesPage.frameLocator(pagesDetailFrame).getByText(template)).isVisible();
        pagesPage.frameLocator(pagesDetailFrame).getByText(template).click();
        pagesPage.frameLocator(pagesDetailFrame)
                .getByLabel("Content", new FrameLocator.GetByLabelOptions().setExact(true))
                .getByLabel("", new Locator.GetByLabelOptions().setExact(true)).check();
        //Select the WF action (Save/Publish)
        pagesPage.frameLocator(pagesDetailFrame).getByText("Publish").click();
    }

    /**
     * Find the page in the iframe with the given title and return the row id of the page
     *
     * @param pageTitle the title of the page to find in the iframe
     * @return the row id of the page
     */
    public String findPage(final String pageTitle) {
        String rowId = "";

        // Locate the table
        Locator table = pagesPage.locator("tbody"); // Adjust the selector if necessary

        // Locate all rows within the table
        Locator rows = table.locator("tr");
        // Iterate through each row
        int rowCount = rows.count();
        for (int i = 0; i < rowCount; i++) {
            Locator row = rows.nth(i);

            // Locate all cells within the current row
            Locator cells = row.locator("td");

            // Check the content of each cell
            int cellCount = cells.count();
            for (int j = 0; j < cellCount; j++) {
                String cellText = cells.nth(j).textContent().trim();
                if (cellText.equals(pageTitle)) {
                    rowId = rows.nth(i).getAttribute("id");
                    return rowId;
                }
            }
        }
        return rowId;
    }

    /**
     * Execute the action on the page with the given title
     *
     * @param pageTitle the title of the page to execute the action on
     * @param action    the action to execute on the page
     */
    public void executePagesWorkflow(final String pageTitle, final String action) {
        String rowLocator = "#" + "pageActionButton-" + findPage(pageTitle).replaceAll("\\D+", "");
        pagesPage.locator(rowLocator).click();
        assertThat(pagesPage.locator(rowLocator)).isVisible();
        pagesPage.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName(action)).click();
    }

}