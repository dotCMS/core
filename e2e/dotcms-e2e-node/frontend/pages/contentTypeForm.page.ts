import { Page } from "@playwright/test";

export class ContentTypeFormPage {
  constructor(private page: Page) {}

  async fillNewContentType() {
    await this.addTitleField();
  }

  async addTitleField() {
    const dropZone = this.page.locator('div[dragula="fields-bag"]');
    await this.page
      .locator("li")
      .getByText("Text", { exact: true })
      .dragTo(dropZone);
    await this.page.locator("input#name").fill("Text Field");
    await this.page.getByTestId("dotDialogAcceptAction").click();
  }
}
