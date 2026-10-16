import { expect, type Download, type Locator, type Page } from '@playwright/test';

/**
 * How long a preview may take. It is a job on the queue (`/_validate`) that runs the whole import
 * as a dry run, so it grows with the file; the files here are a few rows, and this only bounds a
 * busy or sick instance.
 */
const PREVIEW_TIMEOUT = 60000;

/**
 * The Content Drive CSV import dialog (#37896): opened from the Upload menu, a form, then a
 * preview of what the import would do, then the import itself, which runs in the background.
 */
export class ImportDialog {
    readonly dialog: Locator;
    readonly contentType: Locator;
    readonly workflowAction: Locator;
    readonly dropzone: Locator;
    readonly fileName: Locator;
    readonly sampleButton: Locator;
    readonly previewButton: Locator;
    readonly preview: Locator;
    readonly willImport: Locator;
    readonly errorsBanner: Locator;
    readonly warningsCount: Locator;
    readonly errorsCount: Locator;
    readonly warningItems: Locator;
    readonly errorItems: Locator;
    readonly backButton: Locator;
    readonly confirmButton: Locator;

    constructor(private page: Page) {
        this.dialog = page.getByTestId('dialog-import');
        this.contentType = this.dialog.getByTestId('import-content-type');
        this.workflowAction = this.dialog.getByTestId('import-workflow-action');
        this.dropzone = this.dialog.getByTestId('import-file-dropzone');
        this.fileName = this.dialog.getByTestId('import-file-name');
        this.sampleButton = this.dialog.getByTestId('import-sample-link');
        this.previewButton = this.dialog.getByTestId('import-preview-btn');
        this.preview = this.dialog.getByTestId('import-preview');
        this.willImport = this.dialog.getByTestId('import-summary-will-import');
        this.errorsBanner = this.dialog.getByTestId('import-preview-errors-banner');
        this.warningsCount = this.dialog.getByTestId('import-warnings-count');
        this.errorsCount = this.dialog.getByTestId('import-errors-count');
        this.warningItems = this.dialog.getByTestId('import-warning-item');
        this.errorItems = this.dialog.getByTestId('import-error-item');
        this.backButton = this.dialog.getByTestId('import-preview-back');
        this.confirmButton = this.dialog.getByTestId('import-confirm-btn');
    }

    /** Opens the dialog the only way an author can: Upload, then Import CSV. */
    async open() {
        await this.page.getByTestId('toolbar').getByTestId('upload-button').click();
        await this.page.getByTestId('upload-selector-import-csv').click();
        await expect(this.contentType).toBeVisible({ timeout: 20000 });
    }

    /**
     * Picks a content type in the shared selector, searching for it first: the list is paged, so a
     * type created for the test is not necessarily on the first page.
     */
    async pickContentType(name: string) {
        await this.contentType.click();
        const search = this.page.getByRole('searchbox');
        await search.fill(name);
        await this.page.getByRole('option', { name, exact: true }).click();
        await expect(this.contentType).toContainText(name);
    }

    async pickWorkflowAction(label: string) {
        await this.workflowAction.click();
        await this.page.getByRole('option', { name: label }).click();
    }

    /** Chooses a CSV through the real file chooser the drop area opens. */
    async chooseCsv(name: string, content: string) {
        const chooser = this.page.waitForEvent('filechooser');
        await this.dropzone.click();
        await (
            await chooser
        ).setFiles({ name, mimeType: 'text/csv', buffer: Buffer.from(content, 'utf-8') });
        await expect(this.fileName).toHaveText(name);
    }

    /** Queues the preview and waits for its result to be on screen. */
    async runPreview() {
        const queued = this.page.waitForResponse(
            (response) => response.url().includes('/api/v1/content/_import/_validate'),
            { timeout: PREVIEW_TIMEOUT }
        );
        await this.previewButton.click();
        await queued;
        await expect(this.preview).toBeVisible({ timeout: PREVIEW_TIMEOUT });
    }

    /** Confirms the import and answers the confirmation Content Search also asks for. */
    async importAndConfirm() {
        const started = this.page.waitForResponse(
            (response) =>
                response.request().method() === 'POST' &&
                /\/api\/v1\/content\/_import(\?|$)/.test(response.url())
        );
        await this.confirmButton.click();
        await this.page.getByRole('button', { name: 'Accept', exact: true }).click();
        await started;
    }

    /** Downloads the sample CSV and hands back the download. */
    async downloadSample(): Promise<Download> {
        const download = this.page.waitForEvent('download');
        await this.sampleButton.click();

        return download;
    }
}
