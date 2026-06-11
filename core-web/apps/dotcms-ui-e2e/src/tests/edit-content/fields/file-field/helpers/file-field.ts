import { expect, type Locator, type Page } from '@playwright/test';

/** Stable public image URL for import-from-URL tests; override via E2E_IMPORT_URL env. */
export const E2E_IMPORT_URL = process.env['E2E_IMPORT_URL'] ?? 'https://placehold.co/1x1.png';

export const REQUIRED_FIELD_ERROR = 'This field is mandatory';

/** Minimal text file buffer for file/binary upload tests. */
export function createTestTextFile(name = 'e2e-test-file.txt') {
    return {
        name,
        mimeType: 'text/plain',
        buffer: Buffer.from('dotCMS E2E test file content')
    };
}

/** Minimal 1x1 PNG for image upload tests. */
export function createTestPngFile(name = 'e2e-test-image.png') {
    const base64Png =
        'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==';
    return {
        name,
        mimeType: 'image/png',
        buffer: Buffer.from(base64Png, 'base64')
    };
}

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
        const preview = this.root
            .getByTestId('code-preview')
            .or(this.root.getByTestId('metadata-title'))
            .or(this.root.getByTestId('contentlet-thumbnail'))
            .or(this.root.getByTestId('temp-file-thumbnail'));
        await expect(preview.first()).toBeVisible({ timeout: 15000 });
    }

    async expectPreviewShowsFileName(fileName: string) {
        const codePreview = this.root.getByTestId('code-preview');
        if (await codePreview.isVisible()) {
            await expect(codePreview).toBeVisible({ timeout: 15000 });
            return;
        }

        await expect(this.root.getByTestId('metadata-title')).toContainText(fileName, {
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
        const closeButton = dialog
            .locator('.p-dialog-header-close, .p-dialog-close-button, button[aria-label="Close"]')
            .first();
        await closeButton.click();
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
        expect(response.status()).not.toBe(400);

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
