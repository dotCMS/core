import { Page } from "@playwright/test";

export class TextFieldPage {
  constructor(private page: Page) {}

  async fill(variableName: string, value: string) {
    const input = this.page.locator(`input#${variableName}`);
    await input.fill(value);
  }
}
