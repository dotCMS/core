import { expect, type Locator, type Page } from '@playwright/test';
import { Portlet } from '@utils/portlets';

/**
 * Page object for the Content Drive portlet shell.
 */
export class ContentDrivePage {
    readonly toolbar: Locator;
    readonly treeSelector: Locator;
    readonly sidebar: Locator;
    readonly currentSiteHostname: Locator;
    readonly listTitles: Locator;
    readonly treeNodeLabels: Locator;

    constructor(private page: Page) {
        this.toolbar = page.getByTestId('toolbar');
        this.treeSelector = page.getByTestId('tree-selector');
        this.sidebar = page.getByTestId('sidebar');
        // The site is named by the tree's own root row rather than a header above it, so the
        // hostname is that row's label, and that row is the first one.
        this.currentSiteHostname = this.sidebar.getByTestId('tree-node-label').first();
        this.listTitles = page.getByTestId('item-title-text');
        this.treeNodeLabels = this.sidebar.getByTestId('tree-node-label');
    }

    /**
     * Navigates to Content Drive and waits for the sidebar tree to render nodes.
     */
    async goTo() {
        const folderSearch = this.page
            .waitForResponse(
                (r) => r.url().includes('/api/v1/folder/search') && r.status() === 200,
                { timeout: 30000 }
            )
            .catch(() => null);

        await this.page.goto(Portlet.ContentDrive);
        await this.page.waitForLoadState('domcontentloaded');
        await expect(this.toolbar).toBeVisible({ timeout: 20000 });
        await expect(this.treeSelector).toBeVisible({ timeout: 20000 });
        await folderSearch;
        // Wait for tree nodes via projected labels (more specific than the p-tree host test id).
        await expect(this.treeNodeLabels.first()).toBeVisible({ timeout: 20000 });
        // Waited on after the tree, not before it: the hostname is the root node's label, so it only
        // exists once the folder tree has rendered.
        await expect(this.currentSiteHostname).toBeVisible({ timeout: 20000 });
    }

    async expectSiteHostname(hostname: string) {
        await expect(this.currentSiteHostname).toContainText(hostname, {
            timeout: 15000,
            ignoreCase: true
        });
    }

    /**
     * Clicks the tree toggler, which lives in the toolbar in both states. The sidebar used to carry
     * a second copy for the expanded state; it no longer does.
     */
    async toggleTree() {
        await this.toolbar.getByTestId('tree-toggler').click();
    }

    async expectTreeExpanded() {
        await expect
            .poll(async () => (await this.treeSelector.boundingBox())?.width ?? 0, {
                timeout: 10000
            })
            .toBeGreaterThan(100);
        await expect(this.treeNodeLabels.first()).toBeVisible({ timeout: 10000 });
    }

    async expectTreeCollapsed() {
        await expect
            .poll(async () => (await this.treeSelector.boundingBox())?.width ?? 0, {
                timeout: 10000
            })
            .toBeLessThan(10);
    }

    async expectListContainsTitle(title: string) {
        await expect(this.listTitles.filter({ hasText: title }).first()).toBeVisible({
            timeout: 20000
        });
    }
}
