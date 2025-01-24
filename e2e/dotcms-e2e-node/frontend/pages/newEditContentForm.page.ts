import { expect, Page } from "@playwright/test";

export class NewEditContentFormPage {
  private textField = this.page.getByTestId("textField");
  private siteOrFolderField = this.page.getByTestId("field-siteOrFolderField");

  constructor(private page: Page) {}

  async fillTextField(text: string) {
    await this.textField.fill(text);
  }

  async selectSiteOrFolderField() {
    await this.siteOrFolderField.click();

    const treeNode = this.page.locator(".p-treenode");
    const textContent = await treeNode.first().textContent();
    await treeNode.first().click();

    const label = this.page.locator(".p-treeselect-label");
    await expect(label).toHaveText(`//${textContent}`);
    return textContent;
  }

  async save() {
    await this.page.getByRole("button", { name: "Save" }).click();
  }

  async goToContent(id: string) {
    await this.page.goto(`/dotAdmin/#/content/${id}`);
  }
}
