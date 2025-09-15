import { Locator, Page, expect } from "@playwright/test";

export class PagesListPage {
    constructor(private page: Page) { }

    /**
     * Navigate to the pages list page
     */
    async navigateTo() {
        await this.page.goto("/dotAdmin/#/pages");
    }

    getPageListItems() {
        return this.page
            .getByTestId("pages-listing-panel")
            .locator("tbody")
            .getByRole("row");
    }

    getRowByTitle(title: string) {
        const rows = this.getPageListItems();
        return rows.filter({ hasText: title });
    }

    async activeArchivedFilter() {
        await this.page
            .getByTestId("pages-listing-panel")
            .getByText("Show Archived")
            .click();
    }

    async doActionOnPage(
        rowLocator: Locator,
        action: "Unpublish" | "Archive" | "Destroy",
    ) {
        await rowLocator.getByRole("button").click();

        const actionLocator = this.page
            .locator(".p-menu-overlay")
            .getByLabel(action)
            .locator("a");

        await expect(actionLocator).toBeVisible();

        const responsePromise = this.page.waitForResponse((response) => {
            return (
                response.status() === 200 &&
                response.url().includes("/api/content/_search")
            );
        });
        await actionLocator.click();
        await responsePromise;
    }

    getStatusIcon(rowLocator: Locator) {
        return rowLocator.locator("dot-state-icon");
    }
}
