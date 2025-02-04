import { Page } from "@playwright/test";

export class ContentTypeFormPage {
  constructor(private page: Page) {}

  async fillNewContentType() {
    await this.addTextField();
    await this.addSiteOrFolderField();
  }

  async addTextField() {
    const dropZoneLocator = this.page.getByTestId('fields-bag-0');
    const textFieldItemLocator = this.page.getByTestId("com.dotcms.contenttype.model.field.ImmutableTextField");
    await textFieldItemLocator.dragTo(dropZoneLocator);

    const dialogInputLocator = this.page.locator("input#name");
    await dialogInputLocator.fill("Text Field");

    const dialogAcceptBtnLocator = this.page.getByTestId(
      "dotDialogAcceptAction",
    );
    await dialogAcceptBtnLocator.click();
  }

  async addSiteOrFolderField() {
    const dropZoneLocator = this.page.getByTestId('fields-bag-0');
    const siteOrFolderFieldItemLocator = this.page.getByTestId("com.dotcms.contenttype.model.field.ImmutableHostFolderField");
    await siteOrFolderFieldItemLocator.dragTo(dropZoneLocator);

    const dialogInputLocator = this.page.locator("input#name");
    await dialogInputLocator.fill("Site or Folder Field");

    const dialogAcceptBtnLocator = this.page.getByTestId(
      "dotDialogAcceptAction",
    );
    await dialogAcceptBtnLocator.click();
  }
}
