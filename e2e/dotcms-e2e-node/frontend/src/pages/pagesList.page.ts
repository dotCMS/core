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
    return this.page.getByTestId("pages-listing-panel")
    .locator("tbody")
    .getByRole("row");
  }

  getRowByTitle(title: string) {
    const rows = this.getPageListItems();
    return rows.filter({ hasText: title });
  }
}
