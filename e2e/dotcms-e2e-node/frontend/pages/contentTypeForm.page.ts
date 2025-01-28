import { Page } from "@playwright/test";

export class ContentTypeFormPage {
  private dropZone = this.page.locator('div[dragula="fields-bag"]');
  private dotDialogInput = this.page.locator("input#name");
  private textFieldItem = this.page.locator(
    "[data-clazz='com.dotcms.contenttype.model.field.ImmutableTextField']",
  );
  private siteOrFolderFieldItem = this.page.locator(
    "[data-clazz='com.dotcms.contenttype.model.field.ImmutableHostFolderField']",
  );
  private dotDialogAcceptAction = this.page.getByTestId(
    "dotDialogAcceptAction",
  );

  constructor(private page: Page) {}

  async fillNewContentType() {
    await this.addTextField();
    await this.addSiteOrFolderField();
  }

  async addTextField() {
    await this.textFieldItem.dragTo(this.dropZone);
    await this.dotDialogInput.fill("Text Field");
    await this.dotDialogAcceptAction.click();
  }

  async addSiteOrFolderField() {
    await this.siteOrFolderFieldItem.dragTo(this.dropZone);
    await this.dotDialogInput.fill("Site or Folder Field");
    await this.dotDialogAcceptAction.click();
  }
}
