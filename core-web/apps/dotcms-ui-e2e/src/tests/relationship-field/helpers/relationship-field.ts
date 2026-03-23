import { type Locator, type Page, expect } from '@playwright/test';

/**
 * Page Object Model for the Relationship Field component.
 * Encapsulates interactions with the main relationship table, add menu,
 * delete buttons, drag handles, and pagination.
 */
export class RelationshipFieldComponent {
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

        this.addButton = this.root.getByRole('button', { name: '' }).first();
        this.rows = this.table.locator('tbody tr');
    }

    // ─── Menu Actions ────────────────────────────────────────────────

    /**
     * Opens the "+" menu and clicks "Existing Content" to open the selection dialog.
     */
    async clickRelateExisting(): Promise<void> {
        await this.addButton.click();
        const existingOption = this.page.getByLabel('Existing Content').locator('a');
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
        const menu = this.page.locator('.p-menu-overlay, .p-menu').last();
        await expect(menu).toBeVisible();
        return menu;
    }

    /**
     * Gets the "Existing Content" menu item locator from an open menu.
     */
    getRelateExistingMenuItem(menu: Locator): Locator {
        return menu.getByLabel('Existing Content');
    }

    /**
     * Gets the "Create New" menu item locator from an open menu.
     */
    getCreateNewMenuItem(menu: Locator): Locator {
        return menu.getByLabel('New Content');
    }

    // ─── Table Interactions ──────────────────────────────────────────

    /**
     * Returns the number of rows in the relationship table (excludes empty message row).
     */
    async getRowCount(): Promise<number> {
        // Wait for the table to be stable
        await this.table.waitFor({ state: 'visible' });
        const rows = this.table.locator('tbody tr:not(:has(.pi-folder-open))');
        return rows.count();
    }

    /**
     * Asserts the table displays the expected number of rows.
     */
    async expectRowCount(count: number): Promise<void> {
        const rows = this.table.locator('tbody tr:not(:has(.pi-folder-open))');
        await expect(rows).toHaveCount(count);
    }

    /**
     * Asserts the table is empty (shows empty message).
     */
    async expectEmpty(): Promise<void> {
        const emptyMessage = this.table.locator('.pi-folder-open');
        await expect(emptyMessage).toBeVisible();
    }

    /**
     * Gets the text content of a specific row's title column.
     */
    async getRowTitle(rowIndex: number): Promise<string> {
        const row = this.table.locator('tbody tr:not(:has(.pi-folder-open))').nth(rowIndex);
        const titleCell = row.locator('td').nth(1);
        const text = await titleCell.textContent();
        return text?.trim() ?? '';
    }

    /**
     * Clicks the delete button on a specific row.
     */
    async deleteRow(rowIndex: number): Promise<void> {
        const deleteButtons = this.table.getByTestId('relationship-delete-button');
        await deleteButtons.nth(rowIndex).click();
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
        await this.expectAddButtonDisabled();
        const deleteButtons = this.table.getByTestId('relationship-delete-button');
        const count = await deleteButtons.count();
        for (let i = 0; i < count; i++) {
            const btn = deleteButtons.nth(i).locator('button');
            await expect(btn).toBeDisabled();
        }
    }
}
