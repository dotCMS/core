import { expect, Page } from "@playwright/test";

export class ListingContentPage {
    private addBtn = this.page.getByTestId("add-content-button");
    private addNewContent = this.page.getByTestId("add-new-content-option");
    private resultsTable = this.page.getByTestId("content-results-table");

    constructor(private page: Page) { }

    async goTo(filter?: string) {
        const urlPath = "/dotAdmin/#c/content";
        const urlParams = new URLSearchParams();

        if (filter) {
            const filterCapitalize = filter.charAt(0).toUpperCase() + filter.slice(1);
            urlParams.set("filter", filterCapitalize);
        }

        await this.page.goto(`${urlPath}?${urlParams.toString()}`);
    }

    async clickAddNewContent() {
        await this.addBtn.click();
        await this.addNewContent.click();
    }

    async clickFirstContentRow() {
        await expect(this.resultsTable).toBeVisible();

        await this.resultsTable.getByTestId("content-row").first().getByRole("link").click();
    }
}
