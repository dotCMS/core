import { expect, Page } from "@playwright/test";

export class TextFieldPage {
    constructor(private page: Page) {}

    async fill(vairableName: string, value: string) {
        const input = this.page.locator(`input#${vairableName}`);
        await input.fill(value);

        await expect(input).toHaveValue(value);
    }
}
