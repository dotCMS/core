import { Page } from '@playwright/test';

import { SideMenuComponent } from '@components/sideMenu.component';

export class LoginPage {
    constructor(private page: Page) {}

    /**
     *  Login to dotCMS
     * @param page
     * @param username
     * @param password
     */
    async login(username: string, password: string) {
        await this.page.goto('/dotAdmin');
        await this.page.waitForLoadState();

        const userNameInputLocator = this.page.getByTestId('userNameInput');
        await userNameInputLocator.waitFor({ state: 'visible', timeout: 10000 });
        await userNameInputLocator.click();
        await userNameInputLocator.fill(username);
        await userNameInputLocator.press('Tab');

        const passwordInputLocator = this.page.getByTestId('password');
        await passwordInputLocator.waitFor({ state: 'visible', timeout: 10000 });
        await passwordInputLocator.fill(password);

        const responsePromise = this.page.waitForResponse((response) => {
            return response.url().includes('/api/v1/authentication');
        });

        const loginBtnLocator = this.page.getByTestId('submitButton');
        await loginBtnLocator.click();
        await responsePromise;
    }

    async loginAndOpenSideMenu(username: string, password: string) {
        await this.login(username, password);

        const sideMenu = new SideMenuComponent(this.page);
        await sideMenu.openMenu();
    }
}
