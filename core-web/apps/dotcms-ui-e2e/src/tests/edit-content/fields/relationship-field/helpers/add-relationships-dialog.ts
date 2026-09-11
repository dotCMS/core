import { type Locator, type Page, expect } from '@playwright/test';

/**
 * Locator helper for the "Add Relationships" dialog.
 *
 * Replaces the page object for "Select Existing Content", the dialog this one supersedes. The
 * method names are kept where the concept survived so the specs read the same; what changed is
 * named on the method.
 *
 * Two behaviours deliberately do **not** carry over:
 *
 * - **Confirm is never disabled** (FR-013). The old dialog disabled it on an empty selection,
 *   which made "uncheck the last row" the one case where unchecking could not unrelate. There is
 *   therefore no `expectApplyDisabled` here, and its absence is the point — see
 *   {@link expectConfirmAlwaysEnabled}.
 * - **The dialog has no paginator of its own.** Results page through the shared list view's
 *   paginator, which is part of the table rather than the dialog.
 *
 * All locators are scoped to `this.dialog` so they cannot match the form behind the mask.
 */
export class AddRelationshipsDialog {
    readonly dialog: Locator;
    readonly table: Locator;
    readonly rows: Locator;

    constructor(private page: Page) {
        // The mask wrapper, not `.p-dialog`: PrimeNG renders the footer as a sibling of the dialog
        // body, so scoping to `.p-dialog` loses Cancel and Confirm.
        this.dialog = page.locator('.p-dialog-relationship-field');
        this.table = this.dialog.getByTestId('add-relationships-list');
        this.rows = this.table.locator('tbody tr:not(:has([data-testId="empty-state"]))');
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

    /** The dialog draws its own header, so there must be exactly one title on screen. */
    async expectSingleHeader(): Promise<void> {
        await expect(this.dialog.getByTestId('add-relationships-title')).toHaveCount(1);
        await expect(this.dialog.locator('.p-dialog-header')).toHaveCount(0);
    }

    // ─── Selection (Radio Buttons - Single Mode) ────────────────────

    async selectSingleItem(rowIndex: number): Promise<void> {
        await this.rows.nth(rowIndex).getByTestId('item-radio').click();
    }

    async expectRadioButtons(): Promise<void> {
        await expect(this.rows.first().getByTestId('item-radio')).toBeVisible();
    }

    // ─── Selection (Checkboxes - Multiple Mode) ─────────────────────

    async selectItem(rowIndex: number): Promise<void> {
        await this.rows.nth(rowIndex).locator('[data-testId="item-checkbox"] input').click();
    }

    async selectItems(rowIndices: number[]): Promise<void> {
        for (const index of rowIndices) {
            await this.selectItem(index);
        }
    }

    /** Selects the row whose title matches, rather than trusting a position in the sort order. */
    async selectItemByText(text: string): Promise<void> {
        await this.rows
            .filter({ hasText: text })
            .locator('[data-testId="item-checkbox"] input')
            .click();
    }

    async toggleSelectAll(): Promise<void> {
        await this.table.getByTestId('header-checkbox').click();
    }

    async expectCheckboxes(): Promise<void> {
        await expect(
            this.rows.first().locator('[data-testId="item-checkbox"] input')
        ).toBeVisible();
    }

    async expectHeaderCheckbox(): Promise<void> {
        await expect(this.table.getByTestId('header-checkbox')).toBeVisible();
    }

    // ─── Selection Review ────────────────────────────────────────────

    /**
     * Flips the list between the page of results and the accumulated selection.
     *
     * It matters more here than in the dialog this replaced: the selection survives paging,
     * searching and changing site, so what the editor is about to confirm is routinely not what is
     * on screen.
     */
    async toggleShowSelected(): Promise<void> {
        await this.dialog.getByTestId('add-relationships-selected-toggle').click();
    }

    /** Asserts the running count beside the toggle, which is the only always-visible tally. */
    async expectSelectedCount(count: number): Promise<void> {
        await expect(this.dialog.getByText(`Show Selected (${count})`)).toBeVisible();
    }

    // ─── Confirm / Cancel ───────────────────────────────────────────

    async clickApply(): Promise<void> {
        await this.dialog.getByTestId('add-relationships-confirm').click();
    }

    async clickCancel(): Promise<void> {
        await this.dialog.getByTestId('add-relationships-cancel').click();
    }

    /**
     * FR-013 — confirm is enabled with nothing selected, because confirming an empty selection is
     * how the editor unrelates the last item.
     */
    async expectConfirmAlwaysEnabled(): Promise<void> {
        await expect(
            this.dialog.getByTestId('add-relationships-confirm').getByRole('button')
        ).toBeEnabled();
    }

    // ─── Dialog Dismissal ────────────────────────────────────────────

    async closeViaXButton(): Promise<void> {
        await this.dialog.getByTestId('dialog-close-btn').click();
    }

    async closeViaEsc(): Promise<void> {
        await this.page.keyboard.press('Escape');
    }

    // ─── Search / Filters ────────────────────────────────────────────

    /**
     * `dot-search-input` puts its `testId` on the `<input>` itself, not on a wrapper — so this is
     * the control, and reaching for an `input` inside it finds nothing.
     *
     * No Enter: the shared box emits on its own debounce window and has no submit. The assertion
     * that follows has to poll, which every caller here already does.
     */
    async search(query: string): Promise<void> {
        await this.searchInput().fill(query);
    }

    async clearSearch(): Promise<void> {
        await this.searchInput().fill('');
    }

    searchInput(): Locator {
        return this.dialog.getByTestId('add-relationships-search');
    }

    /** The site/folder scope chip — a real filter, not a label. */
    siteChip(): Locator {
        return this.dialog.getByTestId('add-relationships-site-chip');
    }

    localeChip(): Locator {
        return this.dialog.getByTestId('add-relationships-locale-chip');
    }

    // ─── Row counts ──────────────────────────────────────────────────

    /**
     * Asserts row count is at least `min` (retries until timeout — use instead of snapshot count +
     * expect).
     */
    async expectRowCountAtLeast(min: number, options?: { timeout?: number }): Promise<void> {
        await expect
            .poll(() => this.rows.count(), { timeout: options?.timeout ?? 10000 })
            .toBeGreaterThanOrEqual(min);
    }

    /**
     * Waits until row count is at least `min`, then returns that count (retried read — safe for
     * baselines before actions).
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
     * **Baseline capture only.** `locator.count()` is a single point-in-time snapshot — it does
     * **not** auto-retry. For count assertions use {@link expectRowCount},
     * {@link expectRowCountAtLeast} or {@link waitForRowCountAtLeast}.
     */
    async getRowCount(): Promise<number> {
        return this.rows.count();
    }

    getRow(index: number): Locator {
        return this.rows.nth(index);
    }

    // ─── Empty state ─────────────────────────────────────────────────

    /**
     * The table keeps its header and paging footer when nothing matches, so the controls that get
     * the editor out of the empty result are still there.
     */
    async expectEmptyState(): Promise<void> {
        await expect(this.table.getByTestId('empty-state')).toBeVisible();
        await expect(this.dialog.getByTestId('add-relationships-search')).toBeVisible();
    }

    // ─── Constrained Items (Cardinality) ──────────────────────────────

    /**
     * Asserts a row is refused: listed, but greyed out with its control disabled.
     *
     * It stays listed on purpose — the content exists, and hiding it sends the editor hunting for
     * something they can see everywhere else. What it must not do is look pickable.
     */
    async expectRowConstrainedByText(text: string): Promise<void> {
        const row = this.rows.filter({ hasText: text });

        await expect(row).toHaveCount(1);
        await expect(row).toHaveClass(/opacity-50/);
        await expect(row).toHaveAttribute('aria-disabled', 'true');
        await expect(row.locator('[data-testId="item-checkbox"] input')).toBeDisabled();
    }

    /** Asserts the row containing the given text is selectable. */
    async expectRowSelectableByText(text: string): Promise<void> {
        const row = this.rows.filter({ hasText: text });

        await expect(row).toHaveCount(1);
        await expect(row).not.toHaveClass(/opacity-50/);
        await expect(row.locator('[data-testId="item-checkbox"] input')).toBeEnabled();
    }

    async expectRowSelectable(rowIndex: number): Promise<void> {
        await expect(this.rows.nth(rowIndex)).not.toHaveClass(/opacity-50/);
    }

    // ─── Error State ─────────────────────────────────────────────────

    async expectErrorMessage(): Promise<void> {
        await expect(this.dialog.getByTestId('add-relationships-error')).toBeVisible();
    }
}
