import { Page, expect } from "@playwright/test";

export class LoginPage {
  constructor(private page: Page) {}
  /**
   *  Login to dotCMS
   * @param page
   * @param username
   * @param password
   */
  async login(username: string, password: string) {
    await this.page.goto("/dotAdmin");
    await this.page.waitForLoadState();

    const userNameInputLocator = this.page.locator('input[id="inputtext"]');
    await userNameInputLocator.fill(username);

    const passwordInputLocator = this.page.locator('input[id="password"]');
    await passwordInputLocator.fill(password);

    const loginBtnLocator = this.page.getByTestId("submitButton");
    await loginBtnLocator.click();

    const gettingStartedLocator = this.page.getByRole("link", {
      name: "Getting Started",
    });
    await expect(gettingStartedLocator).toBeVisible();
  }
}
