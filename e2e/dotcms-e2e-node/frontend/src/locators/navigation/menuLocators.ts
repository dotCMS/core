import { Page, Locator } from "@playwright/test";

export class GroupEntriesLocators {
  readonly SITE: Locator;
  readonly CONTENT: Locator;
  readonly SCHEMA: Locator;

  constructor(page: Page) {
    this.SITE = page.getByText("Site", { exact: true });
    this.CONTENT = page
      .getByRole("complementary")
      .getByText("Content", { exact: true });
    this.SCHEMA = page.getByText("Schema");
  }
}

/**
 * Locators for the tools in the menu
 */
export class ToolEntriesLocators {
  readonly SEARCH_ALL: Locator;
  readonly CONTENT_TYPES: Locator;
  readonly CATEGORIES: Locator;

  constructor(page: Page) {
    this.SEARCH_ALL = page.getByRole("link", { name: "Search All" });
    this.CONTENT_TYPES = page.getByRole("link", { name: "Content Types" });
    this.CATEGORIES = page.getByRole("link", { name: "Categories" });
  }
}

/**
 * Locators for the menu entries
 */
export class MenuEntriesLocators {
  readonly EXPAND: Locator;
  readonly COLLAPSE: Locator;

  constructor(page: Page) {
    this.EXPAND = page.getByTestId("dot-nav-header-toggle-button");
    this.COLLAPSE = page
      .locator('button[ng-reflect-ng-class="[object Object]"]')
      .first();
  }
}
