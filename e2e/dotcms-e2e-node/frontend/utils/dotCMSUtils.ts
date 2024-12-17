import { Page, expect, Locator } from '@playwright/test';
import { loginLocators } from '../locators/globalLocators';

export class dotCMSUtils {
    page: Page;

    /**
     *  Login to dotCMS
     * @param page
     * @param username 
     * @param password 
     */
    async login(page: Page, username: string, password: string) {
        await page.goto('/dotAdmin');
        await page.waitForLoadState()
        await page.fill(loginLocators.userNameInput, username);
        await page.fill(loginLocators.passwordInput, password);
        const loginBtnLocator = page.getByTestId(loginLocators.loginBtn);
        await waitForVisibleAndCallback(loginBtnLocator, () => loginBtnLocator.click());
        const gettingStartedLocator = page.getByRole('link', { name: 'Getting Started' });
        await waitForVisibleAndCallback(gettingStartedLocator, () => expect(gettingStartedLocator).toBeVisible());
    }

    /**
     * Navigate to the content portlet providing the menu, group and tool locators
     * @param menu
     * @param group
     * @param tool
     */
    async navigate(menu : Locator, group : Locator, tool : Locator) {
        await menu.click();
        await group.click();
        await tool.click();
    }
};


export const waitFor = async (locator: Locator, state: "attached" | "detached" | "visible" | "hidden"): Promise<void> => {
    await locator.waitFor({state: state});
}

export const waitForAndCallback = async (locator: Locator, state: "attached" | "detached" | "visible" | "hidden", callback: () => Promise<void>): Promise<void> => {
    await waitFor(locator, state);
    await callback();
};

export const waitForVisibleAndCallback = async (locator: Locator, callback: () => Promise<void>): Promise<void> => {
    await waitForAndCallback(locator, 'visible', callback);
};