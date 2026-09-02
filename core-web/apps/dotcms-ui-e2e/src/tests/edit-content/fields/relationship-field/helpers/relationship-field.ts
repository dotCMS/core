import { type Locator, type Page, expect } from '@playwright/test';

/**
 * Locator helper for the Relationship Field.
 * Encapsulates interactions with the main relationship table, add menu,
 * delete buttons, drag handles, and pagination.
 */
export class RelationshipField {
    readonly root: Locator;
    readonly table: Locator;
    readonly addButton: Locator;
    readonly rows: Locator;

    /**
     * @param page - Playwright page
     * @param fieldVariable - The relationship field variable name (e.g. 'relBlogA', 'authors').
     *   Codegen shows the field wrapper has data-testid="field-{variable}".
     *   The table inside has data-testid="relationship-field-table".
     */
    constructor(
        private page: Page,
        fieldVariable?: string
    ) {
        if (fieldVariable) {
            this.root = page.getByTestId(`field-${fieldVariable}`);
            this.table = this.root.getByTestId('relationship-field-table');
        } else {
            this.table = page.getByTestId('relationship-field-table').first();
            this.root = this.table;
        }

        this.addButton = this.root.getByTestId('relationship-add-button');
        this.rows = this.table.locator('tbody tr');
    }

    /** Body rows for related content only (excludes the empty-state row). */
    private dataRows(): Locator {
        return this.table.locator('tbody tr:not(:has([data-testid="relationship-field-empty"]))');
    }

    // ─── Menu Actions ────────────────────────────────────────────────

    /**
     * Opens the "+" menu and clicks "Existing Content" to open the selection dialog.
     */
    async clickRelateExisting(): Promise<void> {
        await this.addButton.click();
        const existingOption = this.page.getByRole('menuitem', { name: 'Existing Content' });
        await existingOption.waitFor({ state: 'visible', timeout: 5000 });
        await existingOption.click();
    }

    /**
     * Opens the "+" menu and clicks "Create New" to open the create new dialog.
     */
    async clickCreateNew(): Promise<void> {
        await this.addButton.click();
        const createNewOption = this.page.getByRole('menuitem', { name: 'New Content' });
        await createNewOption.waitFor({ state: 'visible', timeout: 5000 });
        await createNewOption.click();
    }

    /**
     * Opens the "+" menu and returns the menu locator for inspection.
     */
    async openAddMenu(): Promise<Locator> {
        await this.addButton.click();
        // Popup uses appendTo="body" — locate by ARIA role, not PrimeNG overlay classes
        const menu = this.page.getByRole('menu').filter({
            has: this.page.getByRole('menuitem', { name: 'Existing Content' })
        });
        await expect(menu).toBeVisible();
        return menu;
    }

    /**
     * Gets the "Existing Content" menu item locator from an open menu.
     */
    getRelateExistingMenuItem(menu: Locator): Locator {
        return menu.getByRole('menuitem', { name: 'Existing Content' });
    }

    /**
     * Gets the "Create New" menu item locator from an open menu.
     */
    getCreateNewMenuItem(menu: Locator): Locator {
        return menu.getByRole('menuitem', { name: 'New Content' });
    }

    // ─── Table Interactions ──────────────────────────────────────────

    /**
     * Asserts row count is at least `min` (retries until timeout — use instead of snapshot count + expect).
     */
    async expectRowCountAtLeast(min: number, options?: { timeout?: number }): Promise<void> {
        const rows = this.dataRows();
        await expect
            .poll(() => rows.count(), { timeout: options?.timeout ?? 10000 })
            .toBeGreaterThanOrEqual(min);
    }

    /**
     * Waits until row count is at least `min`, then returns that count (retried read — safe for baselines before actions).
     */
    async waitForRowCountAtLeast(min: number, options?: { timeout?: number }): Promise<number> {
        const rows = this.dataRows();
        let last = 0;
        await expect
            .poll(
                async () => {
                    last = await rows.count();
                    return last;
                },
                { timeout: options?.timeout ?? 10000 }
            )
            .toBeGreaterThanOrEqual(min);
        return last;
    }

    /**
     * Asserts the table displays the expected number of rows.
     */
    async expectRowCount(count: number): Promise<void> {
        await expect(this.dataRows()).toHaveCount(count);
    }

    /**
     * **Baseline capture only.** `locator.count()` is a single point-in-time snapshot — it does **not** auto-retry.
     * For count assertions, use {@link expectRowCount} (`expect(locator).toHaveCount(n)`), {@link expectRowCountAtLeast},
     * or {@link waitForRowCountAtLeast}; do not wrap this return value in a plain `expect(...)` without polling.
     */
    async getRowCount(): Promise<number> {
        await this.table.waitFor({ state: 'visible' });
        return this.dataRows().count();
    }

    /**
     * Asserts the table is empty (shows empty message).
     */
    async expectEmpty(): Promise<void> {
        await expect(this.table.getByTestId('relationship-field-empty')).toBeVisible();
    }

    /**
     * Gets the text content of a specific row's title column.
     */
    async getRowTitle(rowIndex: number): Promise<string> {
        const row = this.dataRows().nth(rowIndex);
        const titleCell = row.locator('td').nth(1);
        const text = await titleCell.textContent();
        return text?.trim() ?? '';
    }

    /**
     * Returns normalized header labels from the relationship table.
     */
    async getHeaderTexts(): Promise<string[]> {
        const headers = this.table.locator('thead th');
        const headerCount = await headers.count();
        const headerTexts: string[] = [];

        for (let i = 0; i < headerCount; i++) {
            const text = await headers.nth(i).textContent();
            const trimmed = text?.trim();
            if (trimmed) {
                headerTexts.push(trimmed.toLowerCase());
            }
        }

        return headerTexts;
    }

    /**
     * Drags a row from sourceIndex to targetIndex using drag handles.
     */
    async dragRowToPosition(sourceIndex: number, targetIndex: number): Promise<void> {
        const handles = this.getDragHandles();
        const sourceHandle = handles.nth(sourceIndex);
        const targetHandle = handles.nth(targetIndex);

        const sourceBounds = await sourceHandle.boundingBox();
        const targetBounds = await targetHandle.boundingBox();

        if (!sourceBounds || !targetBounds) {
            throw new Error('Could not get bounding boxes for drag handles');
        }

        await this.page.mouse.move(
            sourceBounds.x + sourceBounds.width / 2,
            sourceBounds.y + sourceBounds.height / 2
        );
        await this.page.mouse.down();
        await this.page.mouse.move(
            targetBounds.x + targetBounds.width / 2,
            targetBounds.y + targetBounds.height / 2,
            { steps: 10 }
        );
        await this.page.mouse.up();
    }

    /**
     * Clicks the delete button on a specific row.
     */
    async deleteRow(rowIndex: number): Promise<void> {
        const deleteButtons = this.table.getByTestId('relationship-delete-button');
        await deleteButtons.nth(rowIndex).click();
    }

    // ─── Status / Locale Chips ───────────────────────────────────────

    /**
     * Returns the `relationship-locale-tag` chip (a `p-tag`) rendered in the
     * Locales cell of the given data row.
     */
    localeTag(rowIndex = 0): Locator {
        return this.dataRows().nth(rowIndex).getByTestId('relationship-locale-tag');
    }

    /**
     * Returns the `status-tag` chip (a `p-tag`, rendered by
     * `dot-contentlet-status-badge`) in the Status cell of the given data row.
     */
    statusTag(rowIndex = 0): Locator {
        return this.dataRows().nth(rowIndex).getByTestId('status-tag');
    }

    // ─── Add Button State ────────────────────────────────────────────

    /**
     * Asserts "Existing Content" menu option is disabled (single mode with item already selected).
     * The "+" button itself stays enabled, but the menu item has aria-disabled="true".
     */
    async expectRelateExistingDisabled(): Promise<void> {
        await this.addButton.click();

        const existingOption = this.page.getByRole('menuitem', { name: 'Existing Content' });
        await expect(existingOption).toBeVisible();
        await expect(existingOption).toBeDisabled();

        // Close menu
        await this.page.keyboard.press('Escape');
    }

    /**
     * Asserts "New Content" menu option is disabled.
     */
    async expectNewContentDisabled(): Promise<void> {
        await this.addButton.click();

        const newContentOption = this.page.getByRole('menuitem', { name: /New Content/ });
        await expect(newContentOption).toBeVisible();
        await expect(newContentOption).toBeDisabled();

        // Close menu
        await this.page.keyboard.press('Escape');
    }

    /**
     * Asserts the "+" button is enabled.
     */
    async expectAddButtonEnabled(): Promise<void> {
        await expect(this.addButton).toBeEnabled();
    }

    // ─── Drag Handles ────────────────────────────────────────────────

    /**
     * Gets all drag handle locators.
     */
    getDragHandles(): Locator {
        return this.table.getByTestId('relationship-drag-handle');
    }

    /**
     * Asserts drag handles are visible for all rows.
     */
    async expectDragHandlesVisible(): Promise<void> {
        const handles = this.getDragHandles();
        const count = await handles.count();
        expect(count).toBeGreaterThan(0);
        for (let i = 0; i < count; i++) {
            await expect(handles.nth(i)).toBeVisible();
        }
    }

    /**
     * Asserts no drag handles are visible (disabled state).
     */
    async expectDragHandlesHidden(): Promise<void> {
        const handles = this.getDragHandles();
        await expect(handles).toHaveCount(0);
    }

    // ─── Pagination ──────────────────────────────────────────────────

    /**
     * Gets the pagination container locator.
     */
    getPagination(): Locator {
        return this.page.getByTestId('relationship-table-pagination');
    }

    /**
     * Asserts pagination controls are visible.
     */
    async expectPaginationVisible(): Promise<void> {
        await expect(this.getPagination()).toBeVisible();
    }

    /**
     * Asserts pagination controls are not visible.
     */
    async expectPaginationHidden(): Promise<void> {
        await expect(this.getPagination()).toBeHidden();
    }

    /**
     * Clicks the "Next" page button in the main table pagination.
     */
    async clickNextPage(): Promise<void> {
        const pagination = this.getPagination();
        await pagination.locator('button').last().click();
    }

    /**
     * Clicks the "Previous" page button in the main table pagination.
     */
    async clickPreviousPage(): Promise<void> {
        const pagination = this.getPagination();
        await pagination.locator('button').first().click();
    }

    // ─── Disabled State ──────────────────────────────────────────────

    /**
     * Asserts the relationship field is in disabled state:
     * - Add button is disabled
     * - Delete buttons are disabled
     * - Rows show reduced opacity
     */
    async expectDisabled(): Promise<void> {
        await expect(this.addButton.getByRole('button')).toBeDisabled();
        const deleteButtons = this.table.getByTestId('relationship-delete-button');
        const count = await deleteButtons.count();
        for (let i = 0; i < count; i++) {
            await expect(deleteButtons.nth(i).getByRole('button')).toBeDisabled();
        }
    }
}
