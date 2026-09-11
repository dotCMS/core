import { expect, type Locator, type Page } from '@playwright/test';

/**
 * Locator wrapper for the Key/Value field (`dot-edit-content-key-value`).
 * Scopes interactions to `data-testid="field-{variable}"`.
 */
export class KeyValueField {
    readonly root: Locator;
    readonly keyInput: Locator;
    readonly valueInput: Locator;
    readonly saveButton: Locator;
    readonly keyCells: Locator;
    readonly rows: Locator;

    /**
     * @param scope Either the Edit Content field variable name, or an explicit
     *   root Locator for the other consumers of the shared editor (the Field
     *   Variables dialog and the Apps custom-properties panel).
     */
    constructor(
        private page: Page,
        scope: string | Locator = 'keyValueField'
    ) {
        this.root = typeof scope === 'string' ? page.getByTestId(`field-${scope}`) : scope;
        this.keyInput = this.root.getByTestId('key-input');
        this.valueInput = this.root.getByTestId('value-input');
        this.saveButton = this.root.getByTestId('save-button');
        this.keyCells = this.root.getByTestId('dot-key-value-key');
        this.rows = this.root.locator('tr.dot-key-value-table-row');
    }

    async expectVisible(): Promise<void> {
        await expect(this.keyInput).toBeVisible({ timeout: 15000 });
        await expect(this.valueInput).toBeVisible();
        await expect(this.saveButton).toBeVisible();
    }

    async addEntry(key: string, value: string): Promise<void> {
        await this.keyInput.fill(key);
        await this.valueInput.fill(value);
        await this.saveButton.click();
        await expect(this.exactKeyCell(key)).toHaveCount(1, { timeout: 10000 });
    }

    async expectEntryCount(count: number): Promise<void> {
        await expect(this.keyCells).toHaveCount(count, { timeout: 10000 });
    }

    /**
     * The cell for `key`, matched on the `title` attribute rather than the rendered
     * text. The cell wraps its key in branches and comments, so its text content
     * carries whitespace that an anchored regex will not match, and a long key is
     * clipped on screen anyway. `title` is the key, exactly.
     */
    private exactKeyCell(key: string): Locator {
        return this.keyCells.filter({ has: this.page.locator(`[title="${key}"]`) });
    }

    private rowForKey(key: string): Locator {
        return this.exactKeyCell(key).locator(
            'xpath=ancestor::tr[contains(@class,"dot-key-value-table-row")][1]'
        );
    }

    /**
     * The value as rendered at rest. Since #37191 an existing row shows plain
     * text; the input only exists while that row is being edited.
     */
    private valueOutputForKey(key: string): Locator {
        return this.rowForKey(key).getByTestId('dot-key-value-value-output');
    }

    private valueInputForKey(key: string): Locator {
        return this.rowForKey(key).getByTestId('dot-key-value-input');
    }

    async expectEntry(key: string, value?: string): Promise<void> {
        const keyCell = this.exactKeyCell(key);
        await expect(keyCell).toHaveCount(1, { timeout: 10000 });
        await expect(keyCell).toHaveText(key);

        if (value !== undefined) {
            await expect(this.valueOutputForKey(key)).toHaveText(value, { timeout: 10000 });
        }
    }

    async expectKeyAbsent(key: string): Promise<void> {
        await expect(this.exactKeyCell(key)).toHaveCount(0);
    }

    async editEntryValue(key: string, newValue: string): Promise<void> {
        await this.valueOutputForKey(key).click();

        const valueInput = this.valueInputForKey(key);
        await expect(valueInput).toBeVisible({ timeout: 10000 });
        await valueInput.fill(newValue);
        await valueInput.press('Enter');

        // Committing returns the row to its at-rest text presentation.
        await expect(this.valueOutputForKey(key)).toHaveText(newValue, { timeout: 10000 });
    }

    /** Escape must discard the edit and restore what was there before. */
    async cancelEntryEdit(key: string, typed: string, original: string): Promise<void> {
        await this.valueOutputForKey(key).click();

        const valueInput = this.valueInputForKey(key);
        await expect(valueInput).toBeVisible({ timeout: 10000 });
        await valueInput.fill(typed);
        await valueInput.press('Escape');

        await expect(this.valueOutputForKey(key)).toHaveText(original, { timeout: 10000 });
    }

    async deleteEntryByKey(key: string): Promise<void> {
        const row = this.rowForKey(key);
        // Row actions are revealed on hover — see FR-017.
        await row.hover();
        await row.getByTestId('dot-key-value-delete-button').click();
    }

    /** Asserts the full list, in order. Only usable where the field starts empty. */
    async expectKeyOrder(keys: string[]): Promise<void> {
        await expect(this.keyCells).toHaveText(keys, { timeout: 10000 });
    }

    /**
     * Asserts `keys` appear in this relative order, ignoring anything else present.
     *
     * The Apps consumer edits a real app configuration that already holds
     * properties, so asserting the whole list there would be asserting someone
     * else's data.
     */
    async expectRelativeKeyOrder(keys: string[]): Promise<void> {
        await expect(async () => {
            const all = await this.keyCells.allInnerTexts();
            const positions = keys.map((key) => all.map((t) => t.trim()).indexOf(key));

            expect(positions).not.toContain(-1);
            expect(positions).toEqual([...positions].sort((a, b) => a - b));
        }).toPass({ timeout: 10000 });
    }

    /** Drags the row owning `key` onto the row currently at `targetIndex`. */
    async dragRowTo(key: string, targetIndex: number): Promise<void> {
        await this.dragRowOnto(key, this.rows.nth(targetIndex));
    }

    /** Drags the row owning `key` onto the row owning `targetKey`. */
    async dragRowToKey(key: string, targetKey: string): Promise<void> {
        await this.dragRowOnto(key, this.rowForKey(targetKey));
    }

    private async dragRowOnto(key: string, target: Locator): Promise<void> {
        const row = this.rowForKey(key);
        const handle = row.getByTestId('dot-key-value-drag-handle');

        // The handle is `opacity-0` until hover — present and focusable, but not
        // something a pointer drag should start from unrevealed.
        await row.hover();
        await expect(handle).toHaveCSS('opacity', '1', { timeout: 10000 });

        /*
         * Drop into the half of the target row that faces the travel direction.
         *
         * PrimeNG decides "insert above" vs "insert below" from which half of the
         * target the pointer is in, then subtracts one when inserting below. Dropping
         * on the exact centre — Playwright's default — lands on the "above" side, and
         * for a one-row move downwards that resolves to the row's own index: a silent
         * no-op that still reports a successful drag.
         */
        const [source, destination] = await Promise.all([row.boundingBox(), target.boundingBox()]);

        const movingDown = !!source && !!destination && destination.y > source.y;
        const height = destination?.height ?? 0;

        await handle.dragTo(target, {
            targetPosition: {
                x: (destination?.width ?? 0) / 2,
                y: movingDown ? height * 0.75 : height * 0.25
            }
        });
    }

    /**
     * Asserts a row action is present but not revealed (FR-017).
     *
     * Not `toBeHidden()`: these controls use `opacity-0` precisely so they stay in
     * the accessibility tree and the tab order (FR-019, research R-03), and
     * Playwright counts a transparent element as visible. Opacity is the real
     * assertion.
     */
    async expectActionUnrevealed(key: string, testId: string): Promise<void> {
        await expect(this.rowForKey(key).getByTestId(testId)).toHaveCSS('opacity', '0');
    }

    async expectActionRevealedOnHover(key: string, testId: string): Promise<void> {
        const row = this.rowForKey(key);
        await row.hover();
        await expect(row.getByTestId(testId)).toHaveCSS('opacity', '1', { timeout: 10000 });
    }
}
