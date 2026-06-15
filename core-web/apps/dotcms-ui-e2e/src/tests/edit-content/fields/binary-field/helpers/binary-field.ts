import { expect, type Locator, type Page } from '@playwright/test';

import {
    E2E_IMPORT_URL,
    REQUIRED_FIELD_ERROR,
    createTestPngFile,
    createTestTextFile
} from '../../helpers/file-test-data';

export { E2E_IMPORT_URL, createTestPngFile, createTestTextFile };

const AI_DISABLED_TOOLTIP = 'Please configure dotAI to enable this feature';

/**
 * Locator wrapper for the Binary field rendered by the unified
 * `dot-edit-content-file-field` component. Scopes interactions to
 * `data-testid="field-{variable}"`.
 */
export class BinaryField {
    readonly root: Locator;
    readonly dropzone: Locator;
    readonly fileInput: Locator;
    readonly chooseFileBtn: Locator;
    readonly importFromUrlBtn: Locator;
    readonly createNewFileBtn: Locator;
    readonly generateWithAiBtn: Locator;
    readonly preview: Locator;
    readonly requiredError: Locator;
    readonly editButton: Locator;
    readonly editButtonResponsive: Locator;

    constructor(
        private page: Page,
        readonly fieldVariable = 'binaryField'
    ) {
        this.root = page.getByTestId(`field-${fieldVariable}`);
        this.dropzone = this.root.getByTestId('dropzone');
        this.fileInput = this.root.getByTestId('file-field__file-input');
        this.chooseFileBtn = this.root.getByTestId('choose-file-btn');
        this.importFromUrlBtn = this.root.getByTestId('action-import-from-url');
        this.createNewFileBtn = this.root.getByTestId('action-new-file');
        this.generateWithAiBtn = this.root.getByTestId('action-generate-with-ai');
        this.preview = this.root.getByTestId('preview');
        this.requiredError = this.root.locator('.error-message small');
        this.editButton = this.root.getByTestId('edit-button');
        this.editButtonResponsive = this.root.getByTestId('edit-button-responsive');
    }

    async expectVisible() {
        await expect(this.dropzone).toBeVisible({ timeout: 15000 });
    }

    async uploadFile(
        file: { name: string; mimeType: string; buffer: Buffer } = createTestTextFile()
    ) {
        const uploadResponse = this.page.waitForResponse(
            (response) =>
                response.url().includes('/api/v1/temp') &&
                !response.url().includes('/byUrl') &&
                response.request().method() === 'POST' &&
                response.status() === 200
        );

        await this.fileInput.setInputFiles(file);
        await uploadResponse;
        await this.expectPreviewVisible();
    }

    async uploadImage(
        file: { name: string; mimeType: string; buffer: Buffer } = createTestPngFile()
    ) {
        const uploadResponse = this.page.waitForResponse(
            (response) =>
                response.url().includes('/api/v1/temp') &&
                !response.url().includes('/byUrl') &&
                response.request().method() === 'POST' &&
                response.status() === 200
        );

        await this.fileInput.setInputFiles(file);
        await uploadResponse;
        await this.expectPreviewVisible();
    }

    async expectPreviewVisible() {
        await expect(this.preview).toBeVisible({ timeout: 15000 });
    }

    async expectPreviewShowsFileName(fileName: string) {
        await expect(this.preview.locator('.preview-metadata_header')).toContainText(fileName, {
            timeout: 15000
        });
    }

    async expectPreviewShowsContent(text: string) {
        await expect(this.preview.getByTestId('code-preview')).toContainText(text, {
            timeout: 15000
        });
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

    async expectAiButtonDisabledWithTooltip() {
        const aiButton = this.generateWithAiBtn.getByRole('button');
        await expect(aiButton).toBeDisabled();

        await aiButton.hover({ force: true });
        await expect(this.page.getByText(AI_DISABLED_TOOLTIP)).toBeVisible({ timeout: 10000 });
    }

    async isAiButtonEnabled(): Promise<boolean> {
        return this.generateWithAiBtn.getByRole('button').isEnabled();
    }

    async expectEditButtonVisible() {
        await expect(this.editButton.or(this.editButtonResponsive).first()).toBeVisible({
            timeout: 15000
        });
    }

    /**
     * The "Edit image" action only shows when the previewed file is actually an
     * image, so it stays hidden for non-image files (e.g. plain text uploads).
     */
    async expectEditButtonHidden() {
        await this.expectPreviewVisible();
        await expect(this.editButton).toBeHidden();
        await expect(this.editButtonResponsive).toBeHidden();
    }

    async clickEditImage() {
        await this.editButton.or(this.editButtonResponsive).first().click();
    }

    /**
     * New editor: the Dojo ImageEditor dialog opens at page level (the new editor
     * runs inside the same Dojo admin shell as the legacy form).
     */
    async expectImageEditorOpen() {
        await expect(this.page.locator('#dotImageDialog')).toBeVisible({ timeout: 15000 });
        await expect(this.page.locator('#imageToolIframe')).toBeVisible({ timeout: 30000 });
    }

    async openImageEditorInNewEditor() {
        await this.expectEditButtonVisible();
        await this.clickEditImage();
        await this.expectImageEditorOpen();
    }
}
