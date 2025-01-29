import { expect, Page } from "@playwright/test";

export class ListingContentPage {

  constructor(private page: Page) {}

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
    const addBtn = this.page.locator(
      "span[widgetid='dijit_form_DropDownButton_0']",
    );
    const addNewContent = this.page.locator(
      ".dijitPopup tr[aria-label='Add New Content']",
    );

    await addBtn.click();
    await addNewContent.click();
  }

  async clickFirstContentRow() {
    const resultsTable = this.page
    .locator('iframe[name="detailFrame"]')
    .contentFrame()
    .locator("#results_table");

    await expect(resultsTable).toBeVisible();

    await resultsTable.locator("tr").nth(1).getByRole("link").click();
  }
}
