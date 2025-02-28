import { Page, expect, Locator } from "@playwright/test";
import { loginLocators } from "@locators/globalLocators";

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
    await waitForVisibleAndCallback(userNameInputLocator, () =>
      userNameInputLocator.fill(username),
    );
    const passwordInputLocator = page.locator(loginLocators.passwordInput);
    await waitForVisibleAndCallback(passwordInputLocator, () =>
      passwordInputLocator.fill(password),
    );
    const loginBtnLocator = page.getByTestId(loginLocators.loginBtn);
    await waitForVisibleAndCallback(loginBtnLocator, () =>
      loginBtnLocator.click(),
    );
    const gettingStartedLocator = page.getByRole("link", {
      name: "Getting Started",
    });
    await waitForVisibleAndCallback(gettingStartedLocator, () =>
      expect(gettingStartedLocator).toBeVisible(),
    );
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

/**
 * Wait for the locator to be in the provided state
 * @param locator
 * @param state
 */
export const waitFor = async (
  locator: Locator,
  state: "attached" | "detached" | "visible" | "hidden",
): Promise<void> => {
  await locator.waitFor({ state: state });
};

/**
 * Wait for the locator to be visible
 * @param locator
 * @param state
 * @param callback
 */
export const waitForAndCallback = async (
  locator: Locator,
  state: "attached" | "detached" | "visible" | "hidden",
  callback: () => Promise<void>,
): Promise<void> => {
  await waitFor(locator, state);
  await callback();
};

/**
 * Wait for the locator to be visible and execute the callback
 * @param locator
 * @param callback
 */
export const waitForVisibleAndCallback = async (
  locator: Locator,
  callback: () => Promise<void>,
): Promise<void> => {
  await waitForAndCallback(locator, "visible", callback);
};
