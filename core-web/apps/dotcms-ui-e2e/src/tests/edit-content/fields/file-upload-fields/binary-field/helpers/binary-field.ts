import { expect, type Page } from '@playwright/test';

import { FileField } from '../../file-field/helpers/file-field';
import {
    E2E_IMPORT_URL,
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
export class BinaryField extends FileField {
    constructor(page: Page, fieldVariable = 'binaryField') {
        super(page, fieldVariable);
    }

    override async uploadFile(
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

    override async importFromUrl(url: string) {
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

    /**
     * Asserts the disabled dotAI tooltip when the plugin is not installed.
     * No-op when dotAI is configured (button enabled) — that environment is valid but out of scope for this case.
     */
    async expectAiButtonDisabledWithTooltipWhenApplicable() {
        const aiButton = this.generateWithAiBtn.getByRole('button');

        if (await aiButton.isEnabled()) {
            return;
        }

        await expect(aiButton).toBeDisabled();

        // Hover the p-button host (pointer-events-auto) — disabled inner buttons ignore hover.
        await this.generateWithAiBtn.hover();
        await expect(this.page.getByText(AI_DISABLED_TOOLTIP)).toBeVisible({ timeout: 10000 });
    }

    async clickEditImage() {
        await this.editButton.or(this.editButtonResponsive).first().click();
    }

    /**
     * New editor: clicking "Edit image" opens the Angular image editor
     * ({@link DotImageEditorComponent}, `data-testid="image-editor-root"`) inside a
     * PrimeNG dialog. The flag that used to gate it (FEATURE_FLAG_NEW_IMAGE_EDITOR) was
     * removed, so the new editor is always used in the new Edit Content — there is no
     * legacy Dojo iframe here.
     */
    async expectImageEditorOpen() {
        const dialog = this.page.getByRole('dialog');
        await expect(dialog).toBeVisible({ timeout: 15000 });

        await expect(dialog.getByTestId('image-editor-root')).toBeVisible({ timeout: 30000 });
    }

    async openImageEditorInNewEditor() {
        await this.expectEditButtonVisible();
        await this.clickEditImage();
        await this.expectImageEditorOpen();
    }
}
