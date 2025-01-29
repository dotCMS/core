import { Locator, Page } from "@playwright/test";

export class SideMenuPage {
  constructor(private page: Page) {}

  /**
   * Navigate to the content portlet providing the menu, group and tool locators
   * @param menu
   * @param group
   * @param tool
   */
  async navigate(menu: Locator, group: Locator, tool: Locator) {
    await menu.click();
    await group.click();
    await tool.click();
  }
}
