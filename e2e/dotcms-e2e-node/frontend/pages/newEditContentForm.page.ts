import { expect, Page } from "@playwright/test";

export class NewEditContentFormPage {
  constructor(private page: Page) {}

  async fillTextField(text: string) {
    await this.page.getByTestId("textField").fill(text);
  }

  async save() {
    await this.page.getByRole("button", { name: "Save" }).click();
    const response = await this.page.waitForResponse(
      "**/api/v1/workflow/actions/**",
    );
    const jsonData = await response.json();

    expect(jsonData.entity.inode).not.toBeNull();
    return jsonData.entity.inode;
  }

  async goToContent(id: string) {
    await this.page.goto(`/dotAdmin/#/content/${id}`);
  }
}
