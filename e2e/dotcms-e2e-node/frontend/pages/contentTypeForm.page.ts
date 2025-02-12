import { Page } from "@playwright/test";
import {
  FieldsTypes,
  TextField,
  SiteorHostField,
} from "@models/newContentType.model";

export class ContentTypeFormPage {
  constructor(private page: Page) {}

  async createNewContentType(fields: FieldsTypes[]) {
    const promise = fields.reduce((prevPromise, field) => {
      if (field.fieldType === "text") {
        return prevPromise.then(() => this.addTextField(field));
      }
      if (field.fieldType === "siteOrFolder") {
        return prevPromise.then(() => this.addSiteOrFolderField(field));
      }
    }, Promise.resolve());

    await promise;
  }

  async addTextField(field: TextField) {
    const dropZoneLocator = this.page.getByTestId("fields-bag-0");
    const textFieldItemLocator = this.page.getByTestId(
      "com.dotcms.contenttype.model.field.ImmutableTextField",
    );
    await textFieldItemLocator.waitFor();
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
    await siteOrFolderFieldItemLocator.waitFor();
    await siteOrFolderFieldItemLocator.dragTo(dropZoneLocator);

    const dialogInputLocator = this.page.locator("input#name");
    await dialogInputLocator.fill(field.title);

    const dialogAcceptBtnLocator = this.page.getByTestId(
      "dotDialogAcceptAction",
    );
    await dialogAcceptBtnLocator.click();
  }
}
