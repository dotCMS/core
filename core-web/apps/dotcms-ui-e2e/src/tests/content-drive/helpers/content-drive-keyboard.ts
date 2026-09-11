import { expect, type Locator, type Page } from '@playwright/test';

/**
 * Keyboard driver for the Content Drive listing (#32591).
 *
 * Everything here goes through real key and pointer events rather than through the component, which
 * is the whole reason these tests exist: three bugs in this feature shipped past a green Jest suite
 * because a synthesised event was unlike the browser's. See
 * `docs/frontend/KEYBOARD_SHORTCUTS.md` → "Testing shortcuts".
 */
export class ContentDriveKeyboard {
    readonly rows: Locator;
    readonly selectedRows: Locator;
    readonly tabbableRows: Locator;

    constructor(private page: Page) {
        this.rows = page.getByTestId('item-row');
        this.selectedRows = page.locator('[data-testid="item-row"][aria-selected="true"]');
        this.tabbableRows = page.locator('[data-testid="item-row"][tabindex="0"]');
    }

    /**
     * Presses a registry shortcut.
     *
     * `Control` on every platform, deliberately: the registry conflates Command and Control into a
     * single `mod` modifier (`event.metaKey || event.ctrlKey`), so one press covers both, and
     * Playwright 1.36 has no `ControlOrMeta` to write it with.
     */
    async pressShortcut(key: string) {
        await this.page.keyboard.press(`Control+${key}`);
    }

    row(index: number): Locator {
        return this.rows.nth(index);
    }

    /**
     * Clicks a row so the table selects it and the row takes focus.
     *
     * The status cell, deliberately. The checkbox cell stops propagation so the row underneath does
     * not also select, and the title cell opens the item (`titleOpensItem` defaults to true) — a
     * click there navigates away instead of selecting. The status cell carries no handler of its
     * own, so the click reaches the row, which is what a user clicking blank space in a row does.
     */
    private rowBody(index: number): Locator {
        return this.row(index).getByTestId('item-status');
    }

    async clickRow(index: number) {
        await this.rowBody(index).click();
    }

    async shiftClickRow(index: number) {
        await this.rowBody(index).click({ modifiers: ['Shift'] });
    }

    async expectRowCount(count: number) {
        await expect(this.rows).toHaveCount(count, { timeout: 20000 });
    }

    async expectSelectedCount(count: number) {
        await expect(this.selectedRows).toHaveCount(count, { timeout: 10000 });
    }

    async expectRowFocused(index: number) {
        await expect(this.row(index)).toBeFocused({ timeout: 10000 });
    }

    /**
     * Exactly one row in the tab order, the roving tab stop.
     *
     * Both halves are asserted. Counting only the tabbable row would pass while every *other* row
     * was also tabbable, which is precisely the tab trap this feature fixed.
     */
    async expectSingleTabStop(totalRows: number) {
        await expect(this.tabbableRows).toHaveCount(1, { timeout: 10000 });
        await expect(this.page.locator('[data-testid="item-row"][tabindex="-1"]')).toHaveCount(
            totalRows - 1,
            { timeout: 10000 }
        );
    }
}
