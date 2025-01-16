import { Page } from "@playwright/test";

export class NewEditContentFormPage {
  constructor(private page: Page) {}

  async fillTextField(text: string) {
    await this.page.getByTestId("textField").fill(text);
  }

  async save() {
    await this.page.getByRole("button", { name: "Save" }).click();
  }

  async goToContent(id: string) {
    await this.page.goto(`/dotAdmin/#/content/${id}`);
  }
}
