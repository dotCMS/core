import { expect, Locator, Page } from '@playwright/test';
import { DotCMSClazzes } from '@utils/dot-clazzes';

import { FieldsTypes, SiteorHostField, TextField } from '@models/newContentType.model';

/**
 * Page object for the Angular content-type builder
 * (`/dotAdmin/#/content-types-angular/edit/{id}`).
 *
 * Covers drag-and-drop "add field" in the builder plus edit and persistence
 * verification for the `DotFieldService` (`saveFields`/`updateField`) flow.
 */
export class ContentTypeBuilderPage {
    constructor(private page: Page) {}

    /** A placed field card inside the drop zone, located by its display name. */
    private placedField(name: string): Locator {
        return this.page.locator('dot-content-type-field-dragabble-item').filter({ hasText: name });
    }

    /**
     * Open the builder for an existing content type and wait until the layout is ready
     * (drop zone rendered and the field palette available for dragging).
     */
    async goToBuilder(contentTypeId: string) {
        await this.page.goto(`/dotAdmin/#/content-types-angular/edit/${contentTypeId}`);
        await this.page.getByTestId('fields-bag-0').first().waitFor({ state: 'visible' });
        await this.page.getByTestId(DotCMSClazzes.TEXT).first().waitFor({ state: 'visible' });
    }

    /** Add a field through the builder UI (drag from the palette, then fill the dialog). */
    async addField(field: FieldsTypes) {
        switch (field.fieldType) {
            case 'text':
                await this.addTextField(field);
                break;
            case 'siteOrFolder':
                await this.addSiteOrFolderField(field);
                break;
            default:
                throw new Error(`addField does not support field type "${field.fieldType}" yet`);
        }
    }

    async addTextField(field: TextField) {
        await this.dragFieldToZone(DotCMSClazzes.TEXT);
        await this.fillFieldDialogAndSave(field.title);
    }

    async addSiteOrFolderField(field: SiteorHostField) {
        await this.dragFieldToZone(DotCMSClazzes.HOST_FOLDER);
        await this.fillFieldDialogAndSave(field.title);
    }

    /**
     * Open an existing field's dialog (the whole card is clickable), rename it and save.
     * Exercises `DotFieldService.updateField` (`PUT .../fields/{id}`).
     */
    async editField(currentName: string, newName: string) {
        await this.placedField(currentName).click();
        await this.fillFieldDialogAndSave(newName);
    }

    async expectFieldPresent(name: string) {
        await expect(this.placedField(name)).toBeVisible();
    }

    async expectFieldAbsent(name: string) {
        await expect(this.placedField(name)).toHaveCount(0);
    }

    /** Drag a palette item (by field clazz) onto the first drop zone. */
    private async dragFieldToZone(clazz: string) {
        const source = this.page.getByTestId(clazz);
        const dropZone = this.page.getByTestId('fields-bag-0').first();
        await source.waitFor({ state: 'visible' });
        await source.dragTo(dropZone);
    }

    /**
     * Fill the field-properties dialog name input and accept, waiting for the
     * fields persistence request to complete before returning.
     */
    private async fillFieldDialogAndSave(name: string) {
        // Scope to the field-edit DynamicDialog so we never match an unrelated `.p-dialog`.
        const dialog = this.page.locator('p-dynamicdialog:has(dot-edit-field-dialog) .p-dialog');
        await dialog.first().waitFor({ state: 'visible' });

        const nameInput = this.page.locator('input#name');
        await nameInput.waitFor({ state: 'visible' });
        await nameInput.fill(name);

        const acceptButton = this.page.getByTestId('dotDialogAcceptAction');
        await expect(acceptButton).toBeEnabled();

        // Set up the wait BEFORE the click that triggers persistence (saveFields / updateField).
        const responsePromise = this.page.waitForResponse(
            (response) =>
                /\/api\/v\d\/contenttype\/.+\/fields/.test(response.url()) &&
                ['POST', 'PUT'].includes(response.request().method())
        );
        await acceptButton.click();
        // Fail fast if persistence errored — otherwise a 4xx/5xx would still resolve
        // the wait and the test could pass without the field being saved.
        const response = await responsePromise;
        expect(response.ok()).toBeTruthy();

        await expect(dialog).toBeHidden();
    }

    /** The field-properties dialog, scoped so we never match an unrelated `.p-dialog`. */
    private fieldDialog(): Locator {
        return this.page.locator('p-dynamicdialog:has(dot-edit-field-dialog) .p-dialog').first();
    }

    /**
     * Opens a placed field's dialog and switches to its Field Variables tab —
     * the second consumer of the shared Key/Value editor (#37191).
     */
    async openFieldVariables(fieldName: string) {
        await this.placedField(fieldName).click();

        const dialog = this.fieldDialog();
        await dialog.waitFor({ state: 'visible' });
        await dialog.getByRole('tab', { name: /field variables/i }).click();
        await this.fieldVariablesPanel().waitFor({ state: 'visible' });
    }

    /** Root of the Field Variables editor, for scoping a `KeyValueField` helper. */
    fieldVariablesPanel(): Locator {
        return this.fieldDialog().locator('dot-content-type-fields-variables');
    }

    /**
     * Closes the field dialog with its own close button.
     *
     * Not the Cancel button: `hideButtons` is true on every tab but Overview (and
     * Settings, where the field has them), so the Field Variables tab has no footer.
     * Not Escape either — that depends on where focus happens to be, and it left the
     * dialog open in CI while passing locally. The close button is always there and
     * always does the same thing.
     */
    async closeFieldDialog() {
        const dialog = this.fieldDialog();
        await dialog.locator('.p-dialog-close-button').click();
        await expect(dialog).toBeHidden({ timeout: 10000 });
    }
}
