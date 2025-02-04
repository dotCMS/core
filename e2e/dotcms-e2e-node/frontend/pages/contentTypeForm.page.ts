import { Page } from "@playwright/test";
import {
  FieldsTypes,
  TextField,
  SiteorHostField,
} from "@models/newContentType.model";

export class ContentTypeFormPage {
  constructor(private page: Page) {}

  async createNewContentType(fields: FieldsTypes[]) {
    const promises = fields.map((field) => {
      if (field.fieldType === 'text'){
        return this.addTextField(field);
      } 
      if (field.fieldType === 'siteOrFolder') {
        return this.addSiteOrFolderField(field);
      }
    });
    await Promise.all(promises)
  }

  async addTextField(field: TextField) {
    const dropZoneLocator = this.page.getByTestId("fields-bag-0");
    const textFieldItemLocator = this.page.getByTestId(
      "com.dotcms.contenttype.model.field.ImmutableTextField",
    );
    await textFieldItemLocator.dragTo(dropZoneLocator);

    const dialogInputLocator = this.page.locator("input#name");
    await dialogInputLocator.fill(field.title);

    const dialogAcceptBtnLocator = this.page.getByTestId(
      "dotDialogAcceptAction",
    );
    await dialogAcceptBtnLocator.click();
  }

  async addSiteOrFolderField(field: SiteorHostField) {
    const dropZoneLocator = this.page.getByTestId("fields-bag-0");
    const siteOrFolderFieldItemLocator = this.page.getByTestId(
      "com.dotcms.contenttype.model.field.ImmutableHostFolderField",
    );
    await siteOrFolderFieldItemLocator.dragTo(dropZoneLocator);

    const dialogInputLocator = this.page.locator("input#name");
    await dialogInputLocator.fill(field.title);

    const dialogAcceptBtnLocator = this.page.getByTestId(
      "dotDialogAcceptAction",
    );
    await dialogAcceptBtnLocator.click();
  }
}
