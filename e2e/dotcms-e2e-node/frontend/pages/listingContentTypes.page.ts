import { Page } from "@playwright/test";

export class ListingContentTypesPage {
  constructor(private page: Page) {}

  async goTo() {
    await this.page.goto("/content-types-angular");
  }
}
