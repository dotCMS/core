import { Page } from "@playwright/test";

export class ListingContentPage {
  constructor(private page: Page) {}
  #addBtn = this.page.locator("span[widgetid='dijit_form_DropDownButton_0']");
  #addNewContent = this.page.locator(
    ".dijitPopup tr[aria-label='Add New Content']",
  );

  async goTo(filter?: string) {
    const urlPath = "/dotAdmin/#c/content";
    const urlParams = new URLSearchParams();

    if (filter) {
      urlParams.set("filter", filter);
    }

    await this.page.goto(`${urlPath}?${urlParams.toString()}`);
  }

  async clickAddNewContent() {
    await this.#addBtn.click();
    await this.#addNewContent.click();
  }

  async clickFirstContentRow() {
    await this.page
      .locator('iframe[name="detailFrame"]')
      .contentFrame()
      .locator("#results_table")
      .locator("tr")
      .nth(1)
      .getByRole("link")
      .click();
  }
}
