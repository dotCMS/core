import { Page } from "@playwright/test";

export class PagesListPage {
  constructor(private page: Page) {}

  /**
   * Navigate to the pages list page
   */
  async navigateTo() {
    await this.page.goto("/dotAdmin/#/pages");
  }

  getPageListItems() {
    return this.page.getByTestId("pages-listing-panel").getByRole("row");
  }
}
