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
        this.rows = this.table.locator(
            'tbody tr:not(:has([data-testid="relationship-dialog-empty"]))'
        );
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
        await this.table.locator('[data-testid="header-checkbox"]').click();
    }

    async expectCheckboxes(): Promise<void> {
        await expect(this.rows.first().locator('p-tablecheckbox')).toBeVisible();
    }

    async expectHeaderCheckbox(): Promise<void> {
        await expect(this.table.locator('[data-testid="header-checkbox"]')).toBeVisible();
    }

    // ─── Apply / Cancel ─────────────────────────────────────────────

    async clickApply(): Promise<void> {
        await this.dialog.getByTestId('apply-button').click();
    }

    async clickCancel(): Promise<void> {
        await this.dialog.getByTestId('cancel-button').click();
    }

    async expectApplyDisabled(): Promise<void> {
        await expect(this.dialog.getByTestId('apply-button').getByRole('button')).toBeDisabled();
    }

    async expectApplyEnabled(): Promise<void> {
        await expect(this.dialog.getByTestId('apply-button').getByRole('button')).toBeEnabled();
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
        const searchInput = this.dialog.getByTestId('relationship-dialog-search-query');
        await searchInput.fill(query);
        // Submit the form via Enter — the search button is inside a popover, not always visible
        await searchInput.press('Enter');
    }

    async openFilters(): Promise<void> {
        await this.dialog.getByTestId('open-filters-button').click();
        // PrimeNG popover appends to body with an enter animation — wait for it to settle
        await this.page
            .getByTestId('clear-button')
            .getByRole('button')
            .waitFor({ state: 'visible' });
    }

    /**
     * Searches using the popover filter panel (opens filters, fills query, clicks search button).
     */
    async searchViaFilters(query: string): Promise<void> {
        const searchInput = this.dialog.getByTestId('relationship-dialog-search-query');
        await searchInput.fill(query);
        await this.openFilters();
        await this.page.getByTestId('search-button').getByRole('button').click();
    }

    async clearSearch(): Promise<void> {
        await this.page.getByTestId('clear-button').getByRole('button').click();
    }

    // ─── Show Selected Toggle ────────────────────────────────────────

    async toggleShowSelected(): Promise<void> {
        await this.dialog.getByTestId('show-selected-switch').click();
    }

    // ─── Pagination / row counts ─────────────────────────────────────

    /**
     * Asserts row count is at least `min` (retries until timeout — use instead of snapshot count + expect).
     */
    async expectRowCountAtLeast(min: number, options?: { timeout?: number }): Promise<void> {
        await expect
            .poll(() => this.rows.count(), { timeout: options?.timeout ?? 10000 })
            .toBeGreaterThanOrEqual(min);
    }

    /**
     * Waits until row count is at least `min`, then returns that count (retried read — safe for baselines before actions).
     */
    async waitForRowCountAtLeast(min: number, options?: { timeout?: number }): Promise<number> {
        let last = 0;
        await expect
            .poll(
                async () => {
                    last = await this.rows.count();
                    return last;
                },
                { timeout: options?.timeout ?? 10000 }
            )
            .toBeGreaterThanOrEqual(min);
        return last;
    }

    async expectRowCount(count: number): Promise<void> {
        await expect(this.rows).toHaveCount(count);
    }

    /**
     * **Baseline capture only.** `locator.count()` is a single point-in-time snapshot — it does **not** auto-retry.
     * For count assertions, use {@link expectRowCount} (`expect(locator).toHaveCount(n)`), {@link expectRowCountAtLeast},
     * or {@link waitForRowCountAtLeast}; do not wrap this return value in a plain `expect(...)` without polling.
     */
    async getRowCount(): Promise<number> {
        return this.rows.count();
    }

    async clickNextPage(): Promise<void> {
        await this.dialog.locator('.p-paginator .p-paginator-next').click();
    }

    async expectPaginatorVisible(): Promise<void> {
        await expect(this.dialog.locator('.p-paginator')).toBeVisible();
    }

    // ─── Constrained Items (Cardinality) ──────────────────────────────

    /**
     * Returns the row at the given index.
     */
    getRow(index: number): Locator {
        return this.rows.nth(index);
    }

    /**
     * Asserts a row is constrained (disabled, grayed out — already related to another parent).
     */
    async expectRowConstrained(rowIndex: number): Promise<void> {
        const row = this.rows.nth(rowIndex);
        await expect(row).toHaveClass(/opacity-50/);
        await expect(row).toHaveClass(/pointer-events-none/);

        const checkbox = row.getByTestId('row-checkbox');
        const radio = row.getByTestId('row-radio');
        const control = (await checkbox.count()) > 0 ? checkbox : radio;
        await expect(control.locator('.p-disabled')).toBeVisible();
    }

    /**
     * Asserts the row containing the given text is constrained.
     */
    async expectRowConstrainedByText(text: string): Promise<void> {
        const row = this.rows.filter({ hasText: text });
        await expect(row).toHaveCount(1);
        await expect(row).toHaveClass(/opacity-50/);
        await expect(row).toHaveClass(/pointer-events-none/);

        const checkbox = row.getByTestId('row-checkbox');
        const radio = row.getByTestId('row-radio');
        const control = (await checkbox.count()) > 0 ? checkbox : radio;
        await expect(control.locator('.p-disabled')).toBeVisible();
    }

    /**
     * Asserts a row is NOT constrained (enabled, full opacity).
     */
    async expectRowSelectable(rowIndex: number): Promise<void> {
        const row = this.rows.nth(rowIndex);
        await expect(row).not.toHaveClass(/opacity-50/);
    }

    /**
     * Asserts the row containing the given text is selectable.
     */
    async expectRowSelectableByText(text: string): Promise<void> {
        const row = this.rows.filter({ hasText: text });
        await expect(row).toHaveCount(1);
        await expect(row).not.toHaveClass(/opacity-50/);
    }

    // ─── Error State ─────────────────────────────────────────────────

    async expectErrorMessage(): Promise<void> {
        await expect(this.dialog.getByTestId('error-message')).toBeVisible();
    }
}
