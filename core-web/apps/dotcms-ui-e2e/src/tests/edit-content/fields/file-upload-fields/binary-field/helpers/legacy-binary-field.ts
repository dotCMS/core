import { expect, type Frame, type Locator, type Page } from '@playwright/test';

import { E2E_IMPORT_URL } from '../../helpers/file-test-data';

export { E2E_IMPORT_URL };

/**
 * Locator wrapper for the binary field web component inside the legacy editor (edit_contentlet iframe).
 * Scopes to `#binary-field-{variable}` / `dotcms-binary-field` — no `field-{variable}` wrapper.
 */
export class LegacyBinaryField {
    readonly root: Locator;
    readonly dropzone: Locator;
    readonly fileInput: Locator;
    readonly importFromUrlBtn: Locator;
    readonly preview: Locator;
    readonly editButton: Locator;
    readonly editButtonResponsive: Locator;

    constructor(
        private frame: Frame,
        private page: Page,
        readonly fieldVariable = 'binaryField'
    ) {
        this.root = frame.locator(`#binary-field-${fieldVariable}, dotcms-binary-field`);
        this.dropzone = this.root.getByTestId('dropzone');
        this.fileInput = this.root.getByTestId('file-field__file-input');
        this.importFromUrlBtn = this.root.getByTestId('action-import-from-url');
        this.preview = this.root.getByTestId('preview');
        this.editButton = this.root.getByTestId('edit-button');
        this.editButtonResponsive = this.root.getByTestId('edit-button-responsive');
    }

    async expectVisible() {
        await expect(this.dropzone).toBeVisible({ timeout: 15000 });
    }

    async expectPreviewVisible() {
        await expect(this.preview).toBeVisible({ timeout: 15000 });
    }

    async openImportFromUrlDialog() {
        await this.importFromUrlBtn.getByRole('button').click();
        const dialog = this.frame.getByRole('dialog');
        await expect(dialog).toBeVisible({ timeout: 10000 });
        await expect(dialog).toContainText('URL');
        return dialog;
    }

    getImportDialogLocators() {
        const dialog = this.frame.getByRole('dialog');
        return {
            dialog,
            urlInput: dialog.getByTestId('url-input'),
            importButton: dialog.getByTestId('import-button')
        };
    }

    async importFromUrl(url: string) {
        await this.openImportFromUrlDialog();
        const { urlInput, importButton } = this.getImportDialogLocators();

        const byUrlResponse = this.page.waitForResponse(
            (response) =>
                response.url().includes('/api/v1/temp/byUrl') &&
                response.request().method() === 'POST',
            { timeout: 30000 }
        );

        await urlInput.fill(url);
        await importButton.getByRole('button').click();

        const response = await byUrlResponse;
        expect(response.ok()).toBeTruthy();

        await this.expectPreviewVisible();
    }

    async expectEditButtonVisible() {
        await expect(this.editButton.or(this.editButtonResponsive).first()).toBeVisible({
            timeout: 15000
        });
    }

    async clickEditImage() {
        await this.editButton.or(this.editButtonResponsive).first().click();
    }

    /** Legacy editor: Dojo ImageEditor dialog inside edit_contentlet frame */
    async expectLegacyImageEditorOpen() {
        await expect(this.frame.locator('#dotImageDialog')).toBeVisible({ timeout: 15000 });
        await expect(this.frame.locator('#imageToolIframe')).toBeVisible({ timeout: 30000 });
    }

    async openImageEditorInLegacyEditor() {
        await this.expectEditButtonVisible();
        await this.clickEditImage();
        await this.expectLegacyImageEditorOpen();
    }
}
