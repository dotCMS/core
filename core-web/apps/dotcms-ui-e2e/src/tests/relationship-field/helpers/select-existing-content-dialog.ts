import { type Locator, type Page, expect } from '@playwright/test';

/**
 * Page Object Model for the "Select Existing Content" dialog.
 * Encapsulates interactions with the selection dialog for relationship fields,
 * including search, filtering, pagination, and item selection.
 */
export class SelectExistingContentDialogComponent {
    readonly dialog: Locator;
    readonly table: Locator;
    readonly rows: Locator;
    readonly applyButton: Locator;
    readonly cancelButton: Locator;

    constructor(private page: Page) {
        // The dialog is a PrimeNG dynamic dialog
        this.dialog = page.locator('.p-dialog-relationship-field').locator('.p-dialog');
        this.table = this.dialog.locator('p-table, .p-datatable');
        this.rows = this.table.locator('tbody tr');
        this.applyButton = page.getByTestId('apply-button');
        this.cancelButton = page.getByTestId('cancel-button');
    }

    // ─── Dialog State ────────────────────────────────────────────────

    /**
     * Waits for the selection dialog to be visible.
     */
    async waitForVisible(): Promise<void> {
        await expect(this.dialog).toBeVisible({ timeout: 10000 });
    }

    /**
     * Waits for the dialog content to load (table rows appear).
     */
    async waitForContentLoaded(): Promise<void> {
        await this.dialog.locator('.p-datatable').waitFor({ state: 'visible', timeout: 10000 });
        // Wait for loading to finish
        await expect(this.dialog.locator('.p-datatable-loading-overlay')).toBeHidden({
            timeout: 10000
        });
    }

    /**
     * Asserts the dialog is hidden/closed.
     */
    async expectClosed(): Promise<void> {
        await expect(this.dialog).toBeHidden({ timeout: 5000 });
    }

    // ─── Selection (Radio Buttons - Single Mode) ────────────────────

    /**
     * Selects a single item by row index using the radio button (single mode).
     */
    async selectSingleItem(rowIndex: number): Promise<void> {
        const row = this.rows.nth(rowIndex);
        const radioButton = row.locator('p-tableradiobutton');
        await radioButton.click();
    }

    /**
     * Asserts radio buttons are shown (single selection mode).
     */
    async expectRadioButtons(): Promise<void> {
        const radioButton = this.rows.first().locator('p-tableradiobutton');
        await expect(radioButton).toBeVisible();
    }

    // ─── Selection (Checkboxes - Multiple Mode) ─────────────────────

    /**
     * Selects an item by row index using the checkbox (multiple mode).
     */
    async selectItem(rowIndex: number): Promise<void> {
        const row = this.rows.nth(rowIndex);
        const checkbox = row.locator('p-tablecheckbox');
        await checkbox.click();
    }

    /**
     * Selects multiple items by their row indices.
     */
    async selectItems(rowIndices: number[]): Promise<void> {
        for (const index of rowIndices) {
            await this.selectItem(index);
        }
    }

    /**
     * Clicks the header checkbox to select/deselect all visible items.
     */
    async toggleSelectAll(): Promise<void> {
        const headerCheckbox = this.table.locator('p-tableheadercheckbox');
        await headerCheckbox.click();
    }

    /**
     * Asserts checkboxes are shown (multiple selection mode).
     */
    async expectCheckboxes(): Promise<void> {
        const checkbox = this.rows.first().locator('p-tablecheckbox');
        await expect(checkbox).toBeVisible();
    }

    /**
     * Asserts the header checkbox is visible (multiple mode).
     */
    async expectHeaderCheckbox(): Promise<void> {
        const headerCheckbox = this.table.locator('p-tableheadercheckbox');
        await expect(headerCheckbox).toBeVisible();
    }

    // ─── Apply / Cancel ─────────────────────────────────────────────

    /**
     * Clicks the "Apply" button to confirm selection.
     */
    async clickApply(): Promise<void> {
        const btn = this.applyButton.locator('button');
        await btn.click();
    }

    /**
     * Clicks the "Cancel" button to discard selection.
     */
    async clickCancel(): Promise<void> {
        const btn = this.cancelButton.locator('button');
        await btn.click();
    }

    /**
     * Asserts the "Apply" button is disabled.
     */
    async expectApplyDisabled(): Promise<void> {
        const btn = this.applyButton.locator('button');
        await expect(btn).toBeDisabled();
    }

    /**
     * Asserts the "Apply" button is enabled.
     */
    async expectApplyEnabled(): Promise<void> {
        const btn = this.applyButton.locator('button');
        await expect(btn).toBeEnabled();
    }

    // ─── Dialog Dismissal ────────────────────────────────────────────

    /**
     * Closes the dialog by clicking the X (close) button.
     */
    async closeViaXButton(): Promise<void> {
        const closeButton = this.dialog.locator('.p-dialog-header-close, .p-dialog-close-button');
        // If the close button exists, click it; otherwise we may need the ESC approach
        if ((await closeButton.count()) > 0) {
            await closeButton.click();
        }
    }

    /**
     * Closes the dialog by pressing the ESC key.
     */
    async closeViaEsc(): Promise<void> {
        await this.page.keyboard.press('Escape');
    }

    // ─── Search ──────────────────────────────────────────────────────

    /**
     * Types a search query in the search input field.
     */
    async search(query: string): Promise<void> {
        const searchInput = this.dialog.locator('input[formcontrolname="query"]');
        await searchInput.fill(query);
        // Click the search button
        await this.page.getByTestId('search-button').locator('button').click();
    }

    /**
     * Opens the filter popover.
     */
    async openFilters(): Promise<void> {
        await this.page.getByTestId('open-filters-button').locator('button').click();
    }

    /**
     * Clears the search and filters.
     */
    async clearSearch(): Promise<void> {
        await this.page.getByTestId('clear-button').locator('button').click();
    }

    // ─── Show Selected Toggle ────────────────────────────────────────

    /**
     * Toggles the "Show Selected Items" switch.
     */
    async toggleShowSelected(): Promise<void> {
        await this.page.getByTestId('show-selected-switch').click();
    }

    // ─── Pagination ──────────────────────────────────────────────────

    /**
     * Gets the row count in the dialog table.
     */
    async getRowCount(): Promise<number> {
        return this.rows.count();
    }

    /**
     * Asserts the dialog table has the expected number of rows.
     */
    async expectRowCount(count: number): Promise<void> {
        await expect(this.rows).toHaveCount(count);
    }

    /**
     * Clicks the "Next" page button in the dialog paginator.
     */
    async clickNextPage(): Promise<void> {
        const paginator = this.dialog.locator('.p-paginator');
        const nextButton = paginator.locator('.p-paginator-next');
        await nextButton.click();
    }

    /**
     * Asserts the paginator is visible in the dialog.
     */
    async expectPaginatorVisible(): Promise<void> {
        const paginator = this.dialog.locator('.p-paginator');
        await expect(paginator).toBeVisible();
    }

    // ─── Error State ─────────────────────────────────────────────────

    /**
     * Asserts an error message is displayed in the dialog.
     */
    async expectErrorMessage(): Promise<void> {
        const errorContainer = this.dialog.locator('.border-gray-400');
        await expect(errorContainer).toBeVisible();
    }
}
