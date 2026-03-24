import { type Locator, type Page, expect } from '@playwright/test';

/**
 * Locator helper for the "Select Existing Content" dialog.
 * All locators are scoped to `this.dialog` to avoid conflicts with the main page.
 */
export class SelectExistingContentDialog {
    readonly dialog: Locator;
    readonly table: Locator;
    readonly rows: Locator;

    constructor(private page: Page) {
        // Use the mask wrapper as root — PrimeNG renders header/footer templates
        // as siblings of .p-dialog, so scoping to .p-dialog misses them.
        this.dialog = page.locator('.p-dialog-relationship-field');
        this.table = this.dialog.locator('p-table, .p-datatable');
        this.rows = this.table.locator('tbody tr');
    }

    // ─── Dialog State ────────────────────────────────────────────────

    async waitForVisible(): Promise<void> {
        await expect(this.dialog).toBeVisible({ timeout: 10000 });
    }

    async waitForContentLoaded(): Promise<void> {
        await this.table.waitFor({ state: 'visible', timeout: 10000 });
        await expect(this.dialog.locator('.p-datatable-loading-overlay')).toBeHidden({
            timeout: 10000
        });
    }

    async expectClosed(): Promise<void> {
        await expect(this.dialog).toBeHidden({ timeout: 5000 });
    }

    // ─── Selection (Radio Buttons - Single Mode) ────────────────────

    async selectSingleItem(rowIndex: number): Promise<void> {
        const row = this.rows.nth(rowIndex);
        await row.locator('p-tableradiobutton').click();
    }

    async expectRadioButtons(): Promise<void> {
        await expect(this.rows.first().locator('p-tableradiobutton')).toBeVisible();
    }

    // ─── Selection (Checkboxes - Multiple Mode) ─────────────────────

    async selectItem(rowIndex: number): Promise<void> {
        const row = this.rows.nth(rowIndex);
        await row.locator('p-tablecheckbox').click();
    }

    async selectItems(rowIndices: number[]): Promise<void> {
        for (const index of rowIndices) {
            await this.selectItem(index);
        }
    }

    async toggleSelectAll(): Promise<void> {
        await this.table.locator('p-tableheadercheckbox').click();
    }

    async expectCheckboxes(): Promise<void> {
        await expect(this.rows.first().locator('p-tablecheckbox')).toBeVisible();
    }

    async expectHeaderCheckbox(): Promise<void> {
        await expect(this.table.locator('p-tableheadercheckbox')).toBeVisible();
    }

    // ─── Apply / Cancel ─────────────────────────────────────────────

    async clickApply(): Promise<void> {
        await this.dialog.getByTestId('apply-button').click();
    }

    async clickCancel(): Promise<void> {
        await this.dialog.getByTestId('cancel-button').click();
    }

    async expectApplyDisabled(): Promise<void> {
        // p-button is a custom element — the native <button> inside holds the disabled state
        await expect(this.dialog.getByTestId('apply-button').locator('button')).toBeDisabled();
    }

    async expectApplyEnabled(): Promise<void> {
        await expect(this.dialog.getByTestId('apply-button').locator('button')).toBeEnabled();
    }

    // ─── Dialog Dismissal ────────────────────────────────────────────

    async closeViaXButton(): Promise<void> {
        const closeButton = this.dialog.locator('.p-dialog-header-close, .p-dialog-close-button');
        await expect(closeButton).toBeVisible({ timeout: 5000 });
        await closeButton.click();
    }

    async closeViaEsc(): Promise<void> {
        await this.page.keyboard.press('Escape');
    }

    // ─── Search ──────────────────────────────────────────────────────

    async search(query: string): Promise<void> {
        const searchInput = this.dialog.locator('input[formcontrolname="query"]');
        await searchInput.fill(query);
        // Submit the form via Enter — the search button is inside a popover, not always visible
        await searchInput.press('Enter');
    }

    async openFilters(): Promise<void> {
        await this.dialog.getByTestId('open-filters-button').click();
    }

    /**
     * Searches using the popover filter panel (opens filters, fills query, clicks search button).
     */
    async searchViaFilters(query: string): Promise<void> {
        const searchInput = this.dialog.locator('input[formcontrolname="query"]');
        await searchInput.fill(query);
        await this.openFilters();
        // search-button is inside a popover appended to body
        await this.page.getByTestId('search-button').click();
    }

    async clearSearch(): Promise<void> {
        // Clear button is inside a popover that PrimeNG appends to body (outside dialog scope)
        await this.page.getByTestId('clear-button').click();
    }

    // ─── Show Selected Toggle ────────────────────────────────────────

    async toggleShowSelected(): Promise<void> {
        await this.dialog.getByTestId('show-selected-switch').click();
    }

    // ─── Pagination ──────────────────────────────────────────────────

    async getRowCount(): Promise<number> {
        return this.rows.count();
    }

    async expectRowCount(count: number): Promise<void> {
        await expect(this.rows).toHaveCount(count);
    }

    async clickNextPage(): Promise<void> {
        await this.dialog.locator('.p-paginator .p-paginator-next').click();
    }

    async expectPaginatorVisible(): Promise<void> {
        await expect(this.dialog.locator('.p-paginator')).toBeVisible();
    }

    // ─── Error State ─────────────────────────────────────────────────

    async expectErrorMessage(): Promise<void> {
        await expect(this.dialog.locator('.border-gray-400')).toBeVisible();
    }
}
