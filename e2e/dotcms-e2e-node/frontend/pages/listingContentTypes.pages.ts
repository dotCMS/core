import { Page } from "@playwright/test";

export class ListingContentTypesPage {

  constructor(private page: Page) {}

  async goToUrl() {
    await this.page.goto("/dotAdmin/#/content-types-angular");
  }

  async addNewContentType(name: string) {
    await this.page.getByRole("button", { name: "" }).click();
    await this.page.getByLabel("Content").locator("a").click();
    await this.page
      .locator('[data-test-id="content-type__new-content-banner"] div')
      .nth(2)
      .click();

    await this.page.getByLabel("Content Name").fill(name);
    await this.page.getByTestId("dotDialogAcceptAction").click();
  }

  async goToAddNewContentType(contentType: string) {
    const capitalized =
      contentType.charAt(0).toUpperCase() + contentType.slice(1);

    await this.page
      .getByTestId(`row-${capitalized}`)
      .getByRole("link", { name: "View (0)" })
      .click();
    await this.page
      .locator('iframe[name="detailFrame"]')
      .contentFrame()
      .locator("#dijit_form_DropDownButton_0")
      .click();
    await this.page
      .locator('iframe[name="detailFrame"]')
      .contentFrame()
      .getByLabel("▼")
      .getByText("Add New Content")
      .click();
  }

  async deleteContentType(contentType: string) {
    const capitalized =
      contentType.charAt(0).toUpperCase() + contentType.slice(1);

    await this.page
      .getByTestId(`row-${capitalized}`)
      .getByTestId("dot-menu-button")
      .click();
    await this.page.getByLabel("Delete").locator("a").click();
    await this.page.getByRole("button", { name: "Delete" }).click();
  }
}
