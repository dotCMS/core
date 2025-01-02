import { Page } from "@playwright/test";

export class ListingContentPage {

    constructor(private page: Page) {}
    #addBtn = this.page.locator("span[widgetid='dijit_form_DropDownButton_0']");
  #addNewContent = this.page.locator(
    ".dijitPopup tr[aria-label='Add New Content']",
  );


  async goTo(filter?: string) {
    const urlPath = new URL("/c/content", this.page.url());

    if (filter) {
      urlPath.searchParams.set("filter", filter);
    }

    await this.page.goto(urlPath.toString());
  }

  async clickAddNewContent() {
    await this.#addBtn.click();
    await this.#addNewContent.click();
  }
}
