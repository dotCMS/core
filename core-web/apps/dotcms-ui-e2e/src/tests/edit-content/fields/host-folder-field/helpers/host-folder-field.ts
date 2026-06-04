import { expect, type Locator, type Page } from '@playwright/test';

/**
 * Locator wrapper for the PrimeNG TreeSelect-based Host/Folder field
 * inside the Angular edit-content form.
 *
 * Selectors based on PrimeNG v21 class names.
 */
export class HostFolderField {
    readonly root: Locator;
    readonly treeSelect: Locator;
    readonly label: Locator;
    readonly panel: Locator;
    readonly filterInput: Locator;

    constructor(
        private page: Page,
        fieldVariable = 'siteOrFolder'
    ) {
        this.root = page.getByTestId(`field-${fieldVariable}`);
        this.treeSelect = this.root.locator('.p-treeselect');
        this.label = this.root.locator('.p-treeselect-label');
        this.panel = page.locator('.p-treeselect-overlay');
        this.filterInput = page.locator('input.p-tree-filter-input');
    }

    async openDropdown() {
        await this.treeSelect.click();
        await this.panel.waitFor({ state: 'visible', timeout: 10000 });
    }

    async expectLabelText(expected: string) {
        await expect(this.label).toHaveText(expected, { timeout: 15000 });
    }

    async expectLabelContains(partial: string) {
        await expect(this.label).toContainText(partial, { timeout: 15000 });
    }

    async expectLabelMatchesPattern(pattern: RegExp) {
        await expect(this.label).toHaveText(pattern, { timeout: 15000 });
    }

    async expectPlaceholderNotVisible() {
        const placeholder = this.root.locator('.p-placeholder');
        await expect(placeholder).toHaveCount(0);
    }

    async expectVisible() {
        await this.treeSelect.waitFor({ state: 'visible', timeout: 15000 });
    }

    async expectPanelOpen() {
        await expect(this.panel).toBeVisible({ timeout: 10000 });
    }

    async expectPanelClosed() {
        await expect(this.panel).toBeHidden({ timeout: 10000 });
    }

    async clickTreeNode(text: string) {
        const nodeContent = this.panel.locator('.p-tree-node-content', { hasText: text });
        await nodeContent.waitFor({ state: 'visible', timeout: 10000 });
        await nodeContent.click();
    }

    /**
     * Selects the first visible tree node and returns its label text.
     */
    async selectFirstNode(): Promise<string> {
        const treeItem = this.panel.locator('[role="treeitem"]').first();
        await treeItem.waitFor({ state: 'visible', timeout: 10000 });
        const text = (await treeItem.getAttribute('aria-label')) ?? '';
        const nodeContent = treeItem.locator('.p-tree-node-content');
        await nodeContent.click();
        return text;
    }

    async expandTreeNode(text: string) {
        const nodeContent = this.panel.locator('.p-tree-node-content', { hasText: text });
        const toggle = nodeContent.locator('.p-tree-node-toggle-button').first();
        await toggle.waitFor({ state: 'visible', timeout: 10000 });
        await toggle.click();
    }

    async getFirstNodeLabel(): Promise<string> {
        const treeItem = this.panel.locator('[role="treeitem"]').first();
        await treeItem.waitFor({ state: 'visible', timeout: 10000 });
        return (await treeItem.getAttribute('aria-label')) ?? '';
    }

    async expectTreeNodeVisible(text: string) {
        const node = this.panel.locator('.p-tree-node-content', { hasText: text });
        await expect(node).toBeVisible({ timeout: 10000 });
    }

    async expectTreeNodeNotVisible(text: string) {
        const node = this.panel.locator('.p-tree-node-content', { hasText: text });
        await expect(node).toBeHidden({ timeout: 5000 });
    }

    async expectTreeNodeSelected(text: string) {
        const node = this.panel.locator('.p-tree-node-content.p-tree-node-selected', {
            hasText: text
        });
        await expect(node).toBeVisible({ timeout: 10000 });
    }

    async filterTree(text: string) {
        await this.filterInput.waitFor({ state: 'visible', timeout: 5000 });
        await this.filterInput.fill(text);
    }

    async expectAtLeastOneTreeNode() {
        const nodes = this.panel.locator('.p-tree-node');
        await expect(nodes.first()).toBeVisible({ timeout: 10000 });
    }

    async expectFormFunctional() {
        await this.treeSelect.waitFor({ state: 'visible', timeout: 10000 });
        await expect(this.treeSelect).toBeEnabled();
    }

    getLabelText(): Promise<string | null> {
        return this.label.textContent();
    }
}
