import { type Locator, type Page, expect } from '@playwright/test';

/**
 * Locator helper for the AssetPicker dialog — the "Select Existing File/Image" modal opened from a
 * File or Image field.
 *
 * The picker renders its own header (the dialog is opened with `showHeader: false`), so everything
 * here is scoped to the picker root rather than to PrimeNG's chrome.
 */
export class AssetPickerDialog {
    readonly root: Locator;
    readonly title: Locator;
    readonly closeButton: Locator;
    readonly fullscreenButton: Locator;
    readonly search: Locator;
    readonly sidebar: Locator;
    readonly treeSearch: Locator;
    readonly list: Locator;
    readonly rows: Locator;
    readonly cancelButton: Locator;
    readonly confirmButton: Locator;

    constructor(private page: Page) {
        this.root = page.getByTestId('asset-picker');
        // Title and close come from the shared dialog shell, so their ids are not picker-specific.
        this.title = this.root.getByTestId('dialog-title');
        this.closeButton = this.root.getByTestId('dialog-close-btn');
        this.fullscreenButton = this.root.getByTestId('asset-picker-fullscreen-btn');
        // Two search boxes are on screen at once, so each carries its own id — a shared one made
        // every selector here ambiguous and was what broke this suite in CI.
        this.search = this.root.getByTestId('asset-picker-search-input');
        this.sidebar = this.root.getByTestId('asset-picker-sidebar');
        this.treeSearch = this.root.getByTestId('asset-picker-tree-search-input');
        this.list = this.root.getByTestId('asset-picker-list');
        this.rows = this.list.getByTestId('item-row');
        this.cancelButton = this.root.getByTestId('asset-picker-cancel');
        this.confirmButton = this.root.getByTestId('asset-picker-confirm');
    }

    async waitForVisible(): Promise<void> {
        await expect(this.root).toBeVisible({ timeout: 15000 });
    }

    async expectClosed(): Promise<void> {
        await expect(this.root).toBeHidden({ timeout: 10000 });
    }

    async expectTitle(text: string): Promise<void> {
        await expect(this.title).toHaveText(text);
    }

    /**
     * Types a term into the asset search and waits for the results it produces.
     *
     * The search is debounced and widens the scope to the whole site, which is what makes it a
     * reliable way to reach a seeded asset without depending on which folder the picker opened on.
     */
    async searchFor(term: string): Promise<void> {
        const response = this.page.waitForResponse(
            (res) => res.url().includes('/api/v1/drive/search') && res.status() === 200,
            { timeout: 30000 }
        );
        await this.search.fill(term);
        await response;
    }

    /** The row whose title cell contains `name`. */
    row(name: string): Locator {
        return this.rows.filter({ hasText: name });
    }

    async expectRowVisible(name: string): Promise<void> {
        await expect(this.row(name)).toBeVisible({ timeout: 15000 });
    }

    /**
     * Selects a row by clicking its title — the content, not the cell padding.
     *
     * Clicking the title specifically is the point: in the picker the whole row selects, whereas in
     * Content Drive the title opens the item instead.
     */
    async selectRowByTitle(name: string): Promise<void> {
        await this.row(name).getByTestId('item-title-text').click();
    }

    async expectRowSelected(name: string): Promise<void> {
        await expect(this.row(name).getByRole('radio')).toBeChecked();
    }

    async expectConfirmEnabled(): Promise<void> {
        await expect(this.confirmButton.getByRole('button')).toBeEnabled();
    }

    async expectConfirmDisabled(): Promise<void> {
        await expect(this.confirmButton.getByRole('button')).toBeDisabled();
    }

    async confirm(): Promise<void> {
        await this.confirmButton.getByRole('button').click();
    }

    async cancel(): Promise<void> {
        await this.cancelButton.getByRole('button').click();
    }

    async close(): Promise<void> {
        await this.closeButton.getByRole('button').click();
    }

    /** Rows offer no per-row actions here — a row exists to be picked, not managed. */
    async expectNoRowActions(): Promise<void> {
        await expect(this.list.getByTestId('kebab-menu-button')).toHaveCount(0);
    }
}
