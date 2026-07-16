import { expect, Page } from "@playwright/test";

export class NewEditContentFormPage {
  constructor(private page: Page) {}

  async fillTextField(text: string) {
    const textFieldLocator = this.page.getByTestId("textField");
    await textFieldLocator.fill(text);
  }

  get siteOrFolderFieldLocator() {
    return this.page.getByTestId("field-siteOrFolderField");
  }

  async selectSiteOrFolderField() {
    const siteOrFolderFieldLocator = this.siteOrFolderFieldLocator;
    await siteOrFolderFieldLocator.click();

    const treeNodeLocator = this.page.locator(".p-treenode");
    const textContent = await treeNodeLocator.first().textContent();
    await treeNodeLocator.first().click();

    const labelLocator = this.page.locator(".p-treeselect-label");
    await expect(labelLocator).toHaveText(`//${textContent}`);
    return textContent;
  }

  async save() {
    const saveButtonLocator = this.page.getByRole("button", {
      name: "Save",
    });
    await expect(saveButtonLocator).toBeVisible();

    const responsePromise = this.page.waitForResponse((response) => {
      return (
        response.status() === 200 &&
        response.url().includes("/api/v1/workflow/actions/")
      );
    });
    await saveButtonLocator.click();
    await responsePromise;
  }

  async goToContent(id: string) {
    await this.page.goto(`/dotAdmin/#/content/${id}`);
  }

  async goToNew(contentType: string) {
    await this.page.goto(`/dotAdmin/#c/content/new/${contentType}`);
  }
}
