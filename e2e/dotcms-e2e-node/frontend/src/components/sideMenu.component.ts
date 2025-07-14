import { Page, expect } from "@playwright/test";

export class SideMenuComponent {
  constructor(private page: Page) {}

  async openMenu() {
    const menu = this.page.locator("nav[role='navigation']");
    const classes = await menu.getAttribute("class");
    if (classes.includes("collapsed")) {
      await this.expandMenu();
    }
  }

  async expandMenu() {
    const expandBtn = this.page.getByTestId("dot-nav-header-toggle-button");
    await expect(expandBtn).toBeVisible();
    await expandBtn.click();
  }

  /**
   * Navigate to the content portlet providing the menu, group and tool locators
   * @param menu
   * @param group
   * @param tool
   */
  async navigate(group: string, tool: string) {
    await this.openMenu();

    const navigationSidebar = this.page.getByTestId("navigation-sidebar");
    await navigationSidebar.getByText(group, { exact: true }).click();
    await navigationSidebar.getByRole("link", { name: tool }).click();
  }
}
