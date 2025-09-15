import { Page } from "@playwright/test";

import {
    FieldsTypes,
    RelationshipField,
    SiteorHostField,
    TextField,
} from "@models/newContentType.model";

export class ContentTypeFormPage {
    constructor(private page: Page) { }

    async createNewContentType(fields: FieldsTypes[]) {
        const promise = fields.reduce((prevPromise, field) => {
            if (field.fieldType === "text") {
                return prevPromise.then(() => this.addTextField(field));
            }
            if (field.fieldType === "siteOrFolder") {
                return prevPromise.then(() => this.addSiteOrFolderField(field));
            }
            if (field.fieldType === "relationship") {
                return prevPromise.then(() => this.addRelationshipField(field));
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

        const dialogInputLocator = this.page.getByTestId("field-name-input");
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

        const dialogInputLocator = this.page.getByTestId("field-name-input");
        await dialogInputLocator.fill(field.title);

        const dialogAcceptBtnLocator = this.page.getByTestId(
            "dotDialogAcceptAction",
        );
        await dialogAcceptBtnLocator.click();
    }

    async addRelationshipField(field: RelationshipField) {
        const dropZoneLocator = this.page.getByTestId("fields-bag-0");
        const relationshipFieldItemLocator = this.page.getByTestId(
            "com.dotcms.contenttype.model.field.ImmutableRelationshipField",
        );
        await relationshipFieldItemLocator.waitFor();
        await relationshipFieldItemLocator.dragTo(dropZoneLocator);

        const dialogInputLocator = this.page.getByTestId("field-name-input");
        await dialogInputLocator.fill(field.title);

        const selectContentTypeLocator = this.page.getByTestId("content-type-dropdown");
        await selectContentTypeLocator.click();

        const entityToRelateLocator = this.page.getByLabel(field.entityToRelate);
        await entityToRelateLocator.click();

        const cardinalitySelectorLocator = this.page.getByTestId("cardinality-selector");
        await cardinalitySelectorLocator.click();

        const cardinalityOptionLocator = this.page.getByLabel(field.cardinality);
        await cardinalityOptionLocator.click();

        const dialogAcceptBtnLocator = this.page.getByTestId(
            "dotDialogAcceptAction",
        );
        await dialogAcceptBtnLocator.click();
    }
}
