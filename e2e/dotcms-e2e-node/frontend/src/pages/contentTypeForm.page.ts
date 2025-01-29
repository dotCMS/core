import { Page } from "@playwright/test";

export class ContentTypeFormPage {
  constructor(private page: Page) {}

  async fillNewContentType() {
    await this.addTextField();
    await this.addSiteOrFolderField();
  }

  async addTextField() {
    const dropZone = this.page.locator('div[dragula="fields-bag"]');
    const dotDialogInput = this.page.locator("input#name");
    const dotDialogAcceptAction = this.page.getByTestId("dotDialogAcceptAction");

    const textFieldItem = this.page.locator(
      "[data-clazz='com.dotcms.contenttype.model.field.ImmutableTextField']",
    );

    await textFieldItem.dragTo(dropZone);
    await dotDialogInput.fill("Text Field");
    await dotDialogAcceptAction.click();
  }

  async addSiteOrFolderField() {
    const dropZone = this.page.locator('div[dragula="fields-bag"]');
    const dotDialogInput = this.page.locator("input#name");
    const dotDialogAcceptAction = this.page.getByTestId("dotDialogAcceptAction");

    const siteOrFolderFieldItem = this.page.locator(
      "[data-clazz='com.dotcms.contenttype.model.field.ImmutableHostFolderField']",
    );

    await siteOrFolderFieldItem.dragTo(dropZone);
    await dotDialogInput.fill("Site or Folder Field");
    await dotDialogAcceptAction.click();
  }
}
