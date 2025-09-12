import { Page } from "@playwright/test";

import { SideMenuComponent } from "../components/sideMenu.component";

export class LoginPage {
    constructor(private page: Page) { }

    /**
     *  Login to dotCMS
     * @param page
     * @param username
     * @param password
     */
    async login(username: string, password: string) {
        // Navigate to login page first
        await this.navigateToLogin();

        // Use data-testid selectors (more stable than id selectors)
        await this.page.getByTestId('userNameInput').click();
        await this.page.getByTestId('userNameInput').fill(username);
        await this.page.getByTestId('userNameInput').press('Tab');
        await this.page.getByTestId('password').fill(password);
        await this.page.getByTestId('submitButton').click();
    }

    /**
     * Navigate to login page based on environment
     */
    async navigateToLogin() {
        const currentEnv = process.env['CURRENT_ENV'] || 'dev';
        const loginUrl = currentEnv === 'ci' ? '/login/' : '/dotAdmin/#/public/login';

        await this.page.goto(loginUrl);
        await this.page.waitForLoadState();
    }

    /**
     * Navigate to admin dashboard (should redirect to login if not authenticated)
     */
    async navigateToAdmin() {
        const currentEnv = process.env['CURRENT_ENV'] || 'dev';
        const baseUrl = currentEnv === 'ci' ? 'http://localhost:8080' : 'http://localhost:4200';

        await this.page.goto(`${baseUrl}/dotAdmin/#/`);
        await this.page.waitForLoadState('networkidle');
        await this.page.waitForTimeout(2000); // Wait for any JavaScript redirects
    }

    /**
     * Check if user is logged in
     */
    async isLoggedIn(): Promise<boolean> {
        const currentUrl = this.page.url();
        return !currentUrl.includes('/login/') && !currentUrl.includes('/public/login');
    }

    /**
     * Verify login page structure and elements
     */
    async verifyLoginPageStructure(): Promise<{ inputs: number; buttons: number; hasPasswordField: boolean }> {
        const inputs = await this.page.locator('input').count();
        const buttons = await this.page.locator('button').count();
        const bodyHTML = await this.page.locator('body').innerHTML();
        const hasPasswordField = bodyHTML.includes('password');

        return {
            inputs,
            buttons,
            hasPasswordField
        };
    }

    /**
     * Check if login form elements are present using data-testid
     */
    async hasLoginFormElements(): Promise<{ hasUsernameField: boolean; hasPasswordField: boolean; hasSubmitButton: boolean }> {
        const hasUsernameField = await this.page.getByTestId('userNameInput').isVisible();
        const hasPasswordField = await this.page.getByTestId('password').isVisible();
        const hasSubmitButton = await this.page.getByTestId('submitButton').isVisible();

        return {
            hasUsernameField,
            hasPasswordField,
            hasSubmitButton
        };
    }

    async loginAndOpenSideMenu(username: string, password: string) {
        await this.login(username, password);

        const sideMenu = new SideMenuComponent(this.page);
        await sideMenu.openMenu();
    }
}
