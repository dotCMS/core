import { expect, type Locator, type Page } from '@playwright/test';

/**
 * Locator wrapper for the popover-based Host/Folder field in Edit Contentlet.
 * Uses data-testid selectors from dot-host-folder-field component.
 */
export class HostFolderField {
    readonly root: Locator;
    readonly trigger: Locator;
    readonly label: Locator;
    readonly overlay: Locator;
    readonly sitesPanel: Locator;
    readonly foldersPanel: Locator;
    readonly searchInput: Locator;
    readonly folderTree: Locator;
    readonly selectButton: Locator;
    readonly copyButton: Locator;
    readonly copyIcon: Locator;

    constructor(
        private page: Page,
        fieldVariable = 'siteOrFolder'
    ) {
        this.root = page.getByTestId(`field-${fieldVariable}`);
        this.trigger = this.root.getByTestId('host-folder-trigger');
        this.label = this.root.getByTestId('host-folder-trigger-label');
        // PrimeNG keeps p-popover host hidden while content is visible — scope from page.
        this.overlay = page.getByTestId('host-folder-overlay');
        this.sitesPanel = page.getByTestId('host-folder-sites');
        this.foldersPanel = page.getByTestId('host-folder-folders');
        this.searchInput = page.getByTestId('host-folder-search-input');
        this.folderTree = page.getByTestId('host-folder-tree');
        this.selectButton = page.getByTestId('host-folder-select');
        this.copyButton = this.root.getByTestId('host-folder-copy');
        this.copyIcon = this.root.getByTestId('host-folder-copy-icon');
    }

    private overlayFooterAnchor(): Locator {
        return this.page.getByTestId('host-folder-select');
    }

    async openOverlay() {
        await this.trigger.scrollIntoViewIfNeeded();
        await expect(this.trigger).toBeEnabled();
        await this.trigger.click();
        await expect(this.overlayFooterAnchor()).toBeVisible({ timeout: 15000 });
    }

    /** @deprecated Use openOverlay */
    async openDropdown() {
        return this.openOverlay();
    }

    async closeOverlay() {
        await this.page.keyboard.press('Escape');
        await this.expectOverlayClosed();
    }

    async expectOverlayOpen() {
        await expect(this.overlayFooterAnchor()).toBeVisible({ timeout: 15000 });
    }

    async expectOverlayClosed() {
        await expect(this.overlayFooterAnchor()).toBeHidden({ timeout: 10000 });
    }

    /** @deprecated Use expectOverlayOpen */
    async expectPanelOpen() {
        return this.expectOverlayOpen();
    }

    /** @deprecated Use expectOverlayClosed */
    async expectPanelClosed() {
        return this.expectOverlayClosed();
    }

    async expectLabelText(expected: string) {
        await expect(this.label).toHaveText(expected, { timeout: 15000 });
    }

    async expectLabelContains(partial: string) {
        await expect(this.label).toContainText(partial, { timeout: 15000, ignoreCase: true });
    }

    async expectLabelMatchesPattern(pattern: RegExp) {
        await expect(this.label).toHaveText(pattern, { timeout: 15000 });
    }

    async expectVisible() {
        await this.trigger.waitFor({ state: 'visible', timeout: 15000 });
    }

    async expectFormFunctional() {
        await this.trigger.waitFor({ state: 'visible', timeout: 10000 });
        await expect(this.trigger).toBeEnabled();
    }

    getLabelText(): Promise<string | null> {
        return this.label.textContent();
    }

    siteItem(name: string): Locator {
        return this.sitesPanel
            .getByTestId('host-folder-site-item')
            .filter({ hasText: new RegExp(name.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'i') });
    }

    async selectSite(name: string) {
        const item = this.siteItem(name);
        await item.waitFor({ state: 'visible', timeout: 10000 });
        // Re-clicking the already-selected site stages site root only (no folder/search call).
        const responsePromise = this.page
            .waitForResponse((r) => this.isFolderSearchResponse(r.url(), r.status()), {
                timeout: 5000
            })
            .catch(() => null);
        await item.click();
        await responsePromise;
    }

    /**
     * Selects the first site in the sites panel and returns its label text.
     * Tolerates no folder/search request when that site is already selected.
     */
    async selectFirstSite(): Promise<string> {
        const item = this.sitesPanel.getByTestId('host-folder-site-item').first();
        await item.waitFor({ state: 'visible', timeout: 10000 });
        const label = item.locator('.truncate').first();
        const text = ((await label.textContent()) ?? '').trim();
        const responsePromise = this.page
            .waitForResponse((r) => this.isFolderSearchResponse(r.url(), r.status()), {
                timeout: 5000
            })
            .catch(() => null);
        await item.click();
        await responsePromise;
        return text;
    }

    folderNode(name: string): Locator {
        return this.folderTree.locator('.p-tree-node-content', { hasText: name });
    }

    async clickFolder(name: string) {
        const node = this.folderNode(name);
        await node.waitFor({ state: 'visible', timeout: 10000 });
        await node.click();
    }

    async expandFolder(name: string) {
        const node = this.folderNode(name);
        const toggle = node.locator('.p-tree-node-toggle-button').first();
        await toggle.waitFor({ state: 'visible', timeout: 10000 });
        const responsePromise = this.page
            .waitForResponse((r) => this.isFolderSearchResponse(r.url(), r.status()), {
                timeout: 15000
            })
            .catch(() => null);
        await toggle.click();
        await responsePromise;
    }

    /** @deprecated Use expandFolder */
    async expandTreeNode(text: string) {
        return this.expandFolder(text);
    }

    async confirmSelection() {
        await this.selectButton.click();
        await this.expectOverlayClosed();
    }

    async selectFolderFlow(folderName: string) {
        await this.clickFolder(folderName);
        await this.confirmSelection();
    }

    /**
     * Selects a site root: opens overlay, optionally picks site, confirms Select.
     * When no siteName is given, confirms the already-selected site (typical for new
     * content) without re-clicking — the site is already selected and no folder/search
     * request is needed.
     */
    async selectSiteRoot(siteName?: string): Promise<string> {
        await this.openOverlay();
        let selectedName: string;
        if (siteName) {
            await this.selectSite(siteName);
            selectedName = siteName;
        } else {
            selectedName = ((await this.label.textContent()) ?? '').trim();
            await expect(this.sitesPanel.getByTestId('host-folder-site-item').first()).toBeVisible({
                timeout: 10000
            });
        }
        await this.confirmSelection();
        return selectedName;
    }

    async expectTreeNodeVisible(text: string) {
        await expect(this.folderNode(text)).toBeVisible({ timeout: 10000 });
    }

    async expectTreeNodeNotVisible(text: string) {
        await expect(this.folderNode(text)).toBeHidden({ timeout: 5000 });
    }

    async expectTreeNodeSelected(text: string) {
        const node = this.folderTree.locator('.p-tree-node-content.p-tree-node-selected', {
            hasText: text
        });
        await expect(node).toBeVisible({ timeout: 10000 });
    }

    async expectSiteNotVisible(name: string) {
        await expect(this.siteItem(name)).toBeHidden({ timeout: 5000 });
    }

    async expectAtLeastOneFolderNode() {
        const nodes = this.folderTree.locator('.p-tree-node');
        await expect(nodes.first()).toBeVisible({ timeout: 10000 });
    }

    loadMoreButtons(parentFolder?: string): Locator {
        if (parentFolder) {
            return this.loadMoreButtonUnderParent(parentFolder);
        }

        return this.folderTree.getByTestId('host-folder-load-more');
    }

    loadMoreButtonUnderParent(parentFolder: string): Locator {
        return this.folderNode(parentFolder)
            .locator('xpath=ancestor::li[contains(@class,"p-tree-node")][1]')
            .getByTestId('host-folder-load-more');
    }

    async expectLoadMoreVisible(parentFolder?: string) {
        const button = parentFolder
            ? this.loadMoreButtonUnderParent(parentFolder)
            : this.loadMoreButtons().first();
        await expect(button).toBeVisible({ timeout: 10000 });
    }

    private isFolderSearchResponse(responseUrl: string, status: number, name?: string): boolean {
        if (!responseUrl.includes('/api/v1/folder/search') || status !== 200) {
            return false;
        }

        if (!name) {
            return true;
        }

        const url = new URL(responseUrl);
        return url.searchParams.get('name') === name;
    }

    async clickLoadMore(parentFolder?: string) {
        const button = parentFolder
            ? this.loadMoreButtonUnderParent(parentFolder)
            : this.loadMoreButtons().first();
        const countBefore = await this.folderNodeCount();
        const responsePromise = this.page
            .waitForResponse((r) => this.isFolderSearchResponse(r.url(), r.status()), {
                timeout: 15000
            })
            .catch(() => null);
        await button.scrollIntoViewIfNeeded();
        await button.evaluate((element) => (element as HTMLButtonElement).click());
        await responsePromise;
        await expect
            .poll(async () => this.folderNodeCount(), { timeout: 15000 })
            .toBeGreaterThan(countBefore);
    }

    async searchFolders(term: string) {
        if (term.length < 3) {
            await this.searchInput.fill(term);
            return;
        }

        const responsePromise = this.page.waitForResponse(
            (r) => this.isFolderSearchResponse(r.url(), r.status()),
            { timeout: 15000 }
        );
        await this.searchInput.fill(term);
        await responsePromise;
    }

    /** @deprecated Use searchFolders */
    async filterTree(text: string) {
        return this.searchFolders(text);
    }

    async copyPath() {
        await this.copyButton.click();
    }

    async expectCopyIconCheck() {
        await expect(this.copyIcon).toHaveText('check', { timeout: 5000 });
    }

    folderNodeCount(): Promise<number> {
        return this.folderTree
            .locator('.p-tree-node:not(:has([data-testid="host-folder-load-more"]))')
            .count();
    }
}
