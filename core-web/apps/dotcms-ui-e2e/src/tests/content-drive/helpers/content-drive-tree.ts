import { expect, type Locator, type Page } from '@playwright/test';

/**
 * Locator wrapper for the Content Drive sidebar folder tree
 * (`dot-tree-folder` → shared `dot-folder-tree`).
 *
 * Scoped to `sidebar` so locators stay within the Content Drive panel
 * (the shared tree also exposes `data-testid` on the `p-tree` host).
 */
export class ContentDriveTree {
    readonly root: Locator;

    constructor(private page: Page) {
        this.root = page.getByTestId('sidebar');
    }

    async expectVisible() {
        await expect(this.root.getByTestId('tree-node-label').first()).toBeVisible({
            timeout: 20000
        });
    }

    folderLabel(name: string): Locator {
        return this.root.getByTestId('tree-node-label').filter({ hasText: name });
    }

    folderNode(name: string): Locator {
        return this.root.locator('.p-tree-node-content', { hasText: name });
    }

    async expectFolderVisible(name: string) {
        await expect(this.folderLabel(name)).toBeVisible({ timeout: 20000 });
    }

    async expectFolderNotVisible(name: string) {
        await expect(this.folderLabel(name)).toBeHidden({ timeout: 5000 });
    }

    private isFolderSearchResponse(responseUrl: string, status: number): boolean {
        return responseUrl.includes('/api/v1/folder/search') && status === 200;
    }

    private isDriveSearchResponse(responseUrl: string, status: number): boolean {
        return responseUrl.includes('/api/v1/drive/search') && status === 200;
    }

    /**
     * Expands a folder node via its toggler and waits for child folder load when requested.
     */
    async expandFolder(name: string) {
        const node = this.folderNode(name);
        await node.waitFor({ state: 'visible', timeout: 15000 });
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

    /**
     * Selects a folder node and waits for the drive search that refreshes the list.
     */
    async selectFolder(name: string) {
        const label = this.folderLabel(name);
        await label.waitFor({ state: 'visible', timeout: 15000 });

        const responsePromise = this.page
            .waitForResponse((r) => this.isDriveSearchResponse(r.url(), r.status()), {
                timeout: 20000
            })
            .catch(() => null);

        await label.click();
        await responsePromise;
    }

    async expectFolderSelected(name: string) {
        const node = this.root.locator('.p-tree-node-content.p-tree-node-selected', {
            hasText: name
        });
        await expect(node).toBeVisible({ timeout: 10000 });
    }
}
