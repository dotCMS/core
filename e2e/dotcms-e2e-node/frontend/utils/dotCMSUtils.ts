import { Page, expect, Locator } from "@playwright/test";
import { loginLocators } from "../locators/globalLocators";

export class dotCMSUtils {
  /**
   *  Login to dotCMS
   * @param page
   * @param username
   * @param password
   */
  async login(page: Page, username: string, password: string) {
    await page.goto("/dotAdmin");
    await page.waitForLoadState();
    const userNameInputLocator = page.locator(loginLocators.userNameInput);
    await userNameInputLocator.fill(username);

    const passwordInputLocator = page.locator(loginLocators.passwordInput);
    await passwordInputLocator.fill(password);

    const loginBtnLocator = page.getByTestId(loginLocators.loginBtn);
    await loginBtnLocator.click();
    
    const gettingStartedLocator = page.getByRole("link", {
      name: "Getting Started",
    });
    await expect(gettingStartedLocator).toBeVisible();
  }

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