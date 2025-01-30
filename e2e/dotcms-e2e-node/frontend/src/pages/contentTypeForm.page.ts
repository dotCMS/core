import { Page } from "@playwright/test";

export class ContentTypeFormPage {
  constructor(private page: Page) {}

  async fillNewContentType() {
    await this.addTextField();
    await this.addSiteOrFolderField();
  }

  async addTextField() {
    const dropZoneLocator = this.page.locator('div[dragula="fields-bag"]');
    const textFieldItemLocator = this.page.locator(
      "[data-clazz='com.dotcms.contenttype.model.field.ImmutableTextField']",
    );
    await textFieldItemLocator.dragTo(dropZoneLocator);

    const dialogInputLocator = this.page.locator("input#name");
    await dialogInputLocator.fill("Text Field");

    const btnDialogLocator = this.page.getByTestId("dotDialogAcceptAction");
    await btnDialogLocator.click();
  }

  async addSiteOrFolderField() {
    const dropZoneLocator = this.page.locator('div[dragula="fields-bag"]');
    const siteOrFolderFieldItemLocator = this.page.locator(
      "[data-clazz='com.dotcms.contenttype.model.field.ImmutableHostFolderField']",
    );
    await siteOrFolderFieldItemLocator.dragTo(dropZoneLocator);

    const dialogInputLocator = this.page.locator("input#name");
    await dialogInputLocator.fill("Site or Folder Field");

    const btnDialogLocator = this.page.getByTestId("dotDialogAcceptAction");
    await btnDialogLocator.click();
  }
}
