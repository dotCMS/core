import { type Locator, type Page, expect } from '@playwright/test';

/**
 * Locator helper for the AssetPicker dialog — the one "browse for an existing asset" modal in the
 * product. Four entry points open it, which is why this lives in `components/` rather than under any
 * one field's `helpers/`:
 *
 * - the File field's "Select Existing File"
 * - the Image field's "Select Existing Image"
 * - the Story Block's insert image / video / audio toolbar buttons and slash commands
 * - the WYSIWYG (TinyMCE) field's insert-image button
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
    readonly siteSelector: Locator;
    readonly folderSearch: Locator;
    readonly searchResults: Locator;
    readonly folderTree: Locator;
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
        // The sidebar is three stacked, single-purpose controls (#37208). It used to be one
        // "Search sites & folders" box over a tree whose roots were every browsable site; the site
        // half of that moved into its own selector, so a single `treeSearch` locator no longer
        // describes anything on screen.
        this.siteSelector = this.sidebar.getByTestId('asset-picker-site-selector');
        this.folderSearch = this.root.getByTestId('asset-picker-folder-search-input');
        this.searchResults = this.sidebar.getByTestId('asset-picker-folder-search-results');
        this.folderTree = this.sidebar.getByTestId('asset-picker-folder-tree');
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

    // ─── Sidebar: site selector ──────────────────────────────────────────────

    async expectBrowsingSite(hostname: string): Promise<void> {
        await expect(this.siteSelector).toContainText(hostname, { timeout: 15000 });
    }

    /**
     * Opens the site dropdown. It renders into `body` (PrimeNG `appendTo`), so the overlay is
     * addressed from the page rather than from inside the dialog root.
     */
    async openSiteSelector(): Promise<void> {
        await this.siteSelector.click();
        await expect(this.siteOverlay).toBeVisible({ timeout: 10000 });
    }

    private get siteOverlay(): Locator {
        return this.page.locator('.p-select-overlay');
    }

    /** Every option currently listed in the open site dropdown. */
    get siteOptions(): Locator {
        return this.siteOverlay.locator('.p-select-option');
    }

    async filterSites(term: string): Promise<void> {
        await this.siteOverlay.getByRole('searchbox').fill(term);
    }

    /**
     * Switches the browsed site and waits for the folder tree to reload, so callers do not race the
     * request that the selection kicks off.
     */
    async chooseSite(hostname: string): Promise<void> {
        const folders = this.page.waitForResponse(
            (res) => res.url().includes('/api/v1/folder/search') && res.status() === 200,
            { timeout: 30000 }
        );
        await this.openSiteSelector();
        await this.siteOverlay.getByText(hostname, { exact: true }).click();
        await folders;
        await this.expectBrowsingSite(hostname);
    }

    // ─── Sidebar: folder search ──────────────────────────────────────────────

    /** Rows in the flat result list that replaces the tree while a term is active. */
    get folderResultRows(): Locator {
        return this.searchResults.getByTestId('folder-search-result');
    }

    /**
     * Types a folder term and waits for the search it produces.
     *
     * The input is debounced, so filling it and asserting immediately races the request. Waiting on
     * the recursive call specifically avoids latching onto the tree's own non-recursive paging.
     */
    async searchFolders(term: string): Promise<void> {
        const response = this.page.waitForResponse(
            (res) =>
                res.url().includes('/api/v1/folder/search') &&
                res.url().includes('recursive=true') &&
                res.status() === 200,
            { timeout: 30000 }
        );
        await this.folderSearch.fill(term);
        await response;
    }

    async clearFolderSearch(): Promise<void> {
        await this.folderSearch.fill('');
        await expect(this.folderTree).toBeVisible({ timeout: 15000 });
    }

    // ─── Sidebar: folder tree ────────────────────────────────────────────────

    /** Every node label currently rendered in the tree. */
    get treeNodes(): Locator {
        return this.folderTree.getByTestId('tree-node-label');
    }

    async expectTreeRoots(labels: string[]): Promise<void> {
        await expect(this.folderTree.locator('.p-tree-root > .p-tree-node')).toHaveCount(
            labels.length,
            { timeout: 15000 }
        );
        for (const label of labels) {
            await expect(this.treeNodes.filter({ hasText: label }).first()).toBeVisible();
        }
    }
}
