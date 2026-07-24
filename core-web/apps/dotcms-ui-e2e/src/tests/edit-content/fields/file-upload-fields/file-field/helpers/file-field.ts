import { expect, type Locator, type Page } from '@playwright/test';

import { REQUIRED_FIELD_ERROR } from '../../helpers/file-test-data';

export {
    E2E_IMPORT_URL,
    createTestPngFile,
    createTestTextFile
} from '../../helpers/file-test-data';

/**
 * Locator wrapper for the File field (`dot-edit-content-file-field` / `dot-file-field`).
 * Scopes interactions to `data-testid="field-{variable}"`.
 */
export class FileField {
    readonly root: Locator;
    readonly dropzone: Locator;
    readonly fileInput: Locator;
    readonly chooseFileBtn: Locator;
    readonly importFromUrlBtn: Locator;
    readonly selectExistingFileBtn: Locator;
    readonly createNewFileBtn: Locator;
    readonly generateWithAiBtn: Locator;
    readonly preview: Locator;
    readonly editButton: Locator;
    readonly editButtonResponsive: Locator;
    readonly removeButton: Locator;
    readonly removeButtonResponsive: Locator;
    readonly requiredError: Locator;

    constructor(
        protected page: Page,
        readonly fieldVariable = 'fileField'
    ) {
        this.root = page.getByTestId(`field-${fieldVariable}`);
        this.dropzone = this.root.getByTestId('dropzone');
        this.fileInput = this.root.getByTestId('file-field__file-input');
        this.chooseFileBtn = this.root.getByTestId('choose-file-btn');
        this.importFromUrlBtn = this.root.getByTestId('action-import-from-url');
        this.selectExistingFileBtn = this.root.getByTestId('action-existing-file');
        this.createNewFileBtn = this.root.getByTestId('action-new-file');
        this.generateWithAiBtn = this.root.getByTestId('action-generate-with-ai');
        this.preview = this.root.getByTestId('preview');
        this.editButton = this.root.getByTestId('edit-button');
        this.editButtonResponsive = this.root.getByTestId('edit-button-responsive');
        this.removeButton = this.root.getByTestId('remove-button');
        this.removeButtonResponsive = this.root.getByTestId('remove-button-responsive');
        this.requiredError = this.root.locator('.error-message small');
    }

    async expectVisible() {
        await expect(this.dropzone).toBeVisible({ timeout: 15000 });
    }

    async uploadFile(file: { name: string; mimeType: string; buffer: Buffer }) {
        const uploadResponse = this.page.waitForResponse(
            (response) =>
                response.url().includes('/api/v1/workflow/actions/') &&
                response.url().includes('/fire/NEW') &&
                response.request().method() === 'PUT' &&
                response.status() === 200,
            { timeout: 30000 }
        );

        await this.fileInput.setInputFiles(file);
        await uploadResponse;
        await this.expectPreviewVisible();
    }

    async expectPreviewVisible() {
        await expect(this.preview).toBeVisible({ timeout: 15000 });
    }

    async expectPreviewHidden() {
        await expect(this.preview).toBeHidden({ timeout: 15000 });
        await expect(this.dropzone).toBeVisible({ timeout: 15000 });
    }

    /**
     * The "Edit image" action is available for Binary, Image and File fields when the
     * previewed file is an image (#36363).
     */
    async expectEditButtonVisible() {
        await expect(this.editButton.or(this.editButtonResponsive).first()).toBeVisible({
            timeout: 15000
        });
    }

    async expectEditButtonHidden() {
        await this.expectPreviewVisible();
        await expect(this.editButton).toBeHidden();
        await expect(this.editButtonResponsive).toBeHidden();
    }

    async clickRemoveButton() {
        await this.removeButton.or(this.removeButtonResponsive).first().click();
    }

    async confirmRemoveInPopup() {
        const popup = this.page.locator('.p-confirmpopup');
        await expect(popup).toBeVisible({ timeout: 10000 });
        await expect(popup).toContainText('Are you sure you want to remove this file?');
        await popup.getByRole('button', { name: 'Remove' }).click();
    }

    async expectNoServerErrorMessage() {
        await expect(this.root.getByText('Something went wrong')).toBeHidden();
    }

    async expectPreviewShowsFileName(fileName: string) {
        await expect(this.root.getByTestId('metadata-title')).toContainText(fileName, {
            timeout: 15000
        });
    }

    async expectPreviewShowsContent(text: string) {
        await expect(this.root.getByTestId('code-preview')).toContainText(text, {
            timeout: 15000
        });
    }

    async expectThumbnailVisible() {
        const thumbnail = this.root
            .getByTestId('contentlet-thumbnail')
            .or(this.root.getByTestId('temp-file-thumbnail'));
        await expect(thumbnail).toBeVisible({ timeout: 15000 });
    }

    async expectRequiredErrorVisible() {
        await expect(this.requiredError).toBeVisible({ timeout: 10000 });
        await expect(this.requiredError).toHaveText(REQUIRED_FIELD_ERROR);
    }

    async openImportFromUrlDialog() {
        await this.importFromUrlBtn.getByRole('button').click();
        const dialog = this.page.getByRole('dialog');
        await expect(dialog).toBeVisible({ timeout: 10000 });
        await expect(dialog).toContainText('URL');
        return dialog;
    }

    getImportDialogLocators() {
        const dialog = this.page.getByRole('dialog');
        return {
            dialog,
            urlInput: dialog.getByTestId('url-input'),
            cancelButton: dialog.getByTestId('cancel-button'),
            importButton: dialog.getByTestId('import-button')
        };
    }

    async closeImportDialogViaX() {
        const dialog = this.page.getByRole('dialog');
        await dialog.getByRole('button', { name: 'Close' }).click();
        await expect(dialog).toBeHidden({ timeout: 10000 });
    }

    async importFromUrl(url: string) {
        await this.openImportFromUrlDialog();
        const { urlInput, importButton } = this.getImportDialogLocators();

        const byUrlResponse = this.page.waitForResponse(
            (response) =>
                response.url().includes('/api/v1/temp/byUrl') &&
                response.request().method() === 'POST'
        );
        const workflowResponse = this.page.waitForResponse(
            (response) =>
                response.url().includes('/api/v1/workflow/actions/') &&
                response.url().includes('/fire/NEW') &&
                response.request().method() === 'PUT' &&
                response.status() === 200,
            { timeout: 30000 }
        );

        await urlInput.fill(url);
        await importButton.getByRole('button').click();

        const response = await byUrlResponse;
        expect(response.ok()).toBeTruthy();

        await workflowResponse;
        await this.expectPreviewVisible();
    }

    async expectCreateNewFileVisible() {
        await expect(this.createNewFileBtn).toBeVisible();
    }

    async expectCreateNewFileHidden() {
        await expect(this.createNewFileBtn).toBeHidden();
    }

    async expectGenerateWithAiVisible() {
        await expect(this.generateWithAiBtn).toBeVisible();
    }

    async expectGenerateWithAiHidden() {
        await expect(this.generateWithAiBtn).toBeHidden();
    }
}
