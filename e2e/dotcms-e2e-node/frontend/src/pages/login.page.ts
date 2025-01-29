import { Page, expect } from "@playwright/test";
import { waitForVisibleAndCallback } from "@utils/dotCMSUtils";

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
    await waitForVisibleAndCallback(userNameInputLocator, () =>
      userNameInputLocator.fill(username),
    );
    const passwordInputLocator = this.page.locator('input[id="password"]');
    await waitForVisibleAndCallback(passwordInputLocator, () =>
      passwordInputLocator.fill(password),
    );
    const loginBtnLocator = this.page.getByTestId("submitButton");
    await waitForVisibleAndCallback(loginBtnLocator, () =>
      loginBtnLocator.click(),
    );
    const gettingStartedLocator = this.page.getByRole("link", {
      name: "Getting Started",
    });
    await waitForVisibleAndCallback(gettingStartedLocator, () =>
      expect(gettingStartedLocator).toBeVisible(),
    );
  }
}
