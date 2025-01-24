import { Page } from "@playwright/test";

export class ContentTypeFormPage {
  private dropZone = this.page.locator('div[dragula="fields-bag"]');
  private dotDialogInput = this.page.locator("input#name");
  private dotDialogAcceptAction = this.page.getByTestId(
    "dotDialogAcceptAction",
  );

  constructor(private page: Page) {}

  async fillNewContentType() {
    await this.addTextField();
    await this.addSiteOrFolderField();
  }

  async addTextField() {
    await this.page
      .locator("li")
      .getByText("Text", { exact: true })
      .dragTo(this.dropZone);
    await this.dotDialogInput.fill("Text Field");
    await this.dotDialogAcceptAction.click();
  }

  async addSiteOrFolderField() {
    await this.page
      .locator("li")
      .getByText("Site or Folder", { exact: true })
      .dragTo(this.dropZone);
    await this.dotDialogInput.fill("Site or Folder Field");
    await this.dotDialogAcceptAction.click();
  }
}
