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

    constructor(
        private page: Page,
        readonly fieldVariable = 'keyValueField'
    ) {
        this.root = page.getByTestId(`field-${fieldVariable}`);
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

    private static escapeRegExp(value: string): string {
        return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    }

    private exactKeyCell(key: string): Locator {
        return this.keyCells.filter({
            hasText: new RegExp(`^${KeyValueField.escapeRegExp(key)}$`)
        });
    }

    private valueInputForKey(key: string): Locator {
        return this.exactKeyCell(key)
            .locator('xpath=ancestor::tr[contains(@class,"dot-key-value-table-row")][1]')
            .getByTestId('dot-key-value-input');
    }

    async expectEntry(key: string, value?: string): Promise<void> {
        const keyCell = this.exactKeyCell(key);
        await expect(keyCell).toHaveCount(1, { timeout: 10000 });
        await expect(keyCell).toHaveText(key);

        if (value !== undefined) {
            await expect
                .poll(async () => this.valueInputForKey(key).inputValue(), { timeout: 10000 })
                .toBe(value);
        }
    }

    async expectKeyAbsent(key: string): Promise<void> {
        await expect(this.exactKeyCell(key)).toHaveCount(0);
    }

    async editEntryValue(key: string, newValue: string): Promise<void> {
        const valueInput = this.valueInputForKey(key);
        await expect(valueInput).toBeVisible({ timeout: 10000 });
        await valueInput.fill(newValue);
        await valueInput.press('Enter');
        await expect
            .poll(async () => valueInput.inputValue(), { timeout: 10000 })
            .toBe(newValue);
    }

    async deleteEntry(index: number): Promise<void> {
        await this.root.getByTestId('dot-key-value-delete-button').nth(index).click();
    }
}
