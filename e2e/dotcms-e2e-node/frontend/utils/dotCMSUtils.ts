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
        await page.fill(loginLocators.userNameInput, username);
        await page.fill(loginLocators.passwordInput, password); 
        await page.getByTestId(loginLocators.loginBtn).click();
        await expect(page.getByRole('link', { name: 'Getting Started' })).toBeVisible();
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