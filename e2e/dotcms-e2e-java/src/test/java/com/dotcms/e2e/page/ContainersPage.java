package com.dotcms.e2e.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

/**
 * Page class for interacting with the Containers page.
 *
 * This class provides methods to fill the containers form, find a container by title,
 * and execute workflow actions on a container.
 *
 * @author vico
 */
public class ContainersPage {

    private final Page containersPage;

    public ContainersPage(final Page containersPage) {
        this.containersPage = containersPage;
    }

    /**
     * Fills the containers form with the provided details.
     *
     * @param title the title of the container
     * @param description the description of the container
     * @param maxContents the maximum number of contents
     * @param contentType the content type
     * @param contentTypeCode the content type code
     */
    public void fillContainersForm(final String title,
                                   final String description,
                                   final String maxContents,
                                   final String contentType,
                                   final String contentTypeCode) {
        containersPage.getByTestId("title").fill(title);
        containersPage.getByTestId("description").fill(description);
        containersPage.getByTestId("max-contents").fill(maxContents);
        containersPage.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName("")).click();
        containersPage.getByRole(AriaRole.MENUITEM,
                new Page.GetByRoleOptions().setName(contentType)).click();
        containersPage.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName(contentType))
                .click();
        containersPage.getByTestId("2a3e91e4-fbbf-4876-8c5b-2233c1739b05")
                .getByLabel("Editor content;Press Alt+F1").fill(contentTypeCode + "\n");
        containersPage.getByTestId("saveBtn").click();
    }

    /**
     * Finds a container by its title.
     *
     * @param containerTitle the title of the container to find
     * @return the row ID of the container if found, otherwise an empty string
     */
    public String findContainer(final String containerTitle) {
        String rowId = "";

        // Locate the table
        Locator table = containersPage.locator("tbody"); // Adjust the selector if necessary

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
                if (cellText.equals(containerTitle)) {
                    rowId = rows.nth(i).getAttribute("data-testrowid");
                    return rowId;
                }
            }
        }
        return rowId;
    }

    /**
     * Executes a workflow action on a container.
     *
     * @param containerTitle the title of the container
     * @param action the workflow action to execute
     */
    public void executeContainerWorkflow(final String containerTitle, final String action) {
        String rowId = findContainer(containerTitle);
        containersPage.getByTestId(rowId)
                .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(" p")).click();
        containersPage.getByRole(AriaRole.MENUITEM, new Page.GetByRoleOptions().setName(action))
                .click();
        if (action.equals("Delete")) {
            containersPage.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Accept"))
                    .click();
        }
    }

}
