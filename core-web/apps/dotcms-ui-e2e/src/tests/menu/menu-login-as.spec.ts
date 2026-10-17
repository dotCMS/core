import { DotNavigationPage, LoginPage } from '@pages';
import { expect, test } from '@playwright/test';
import { admin1 } from '@utils/credentials';
import { generateBase64Credentials } from '@utils/generateBase64Credential';

import { startWithBar } from './helpers/menu-state';

import { limited1 } from '../../utils/credentials';

/**
 * Journey: logging in as another user brings that user's navigation (#38078).
 *
 * The admin shell reloads after a login-as; what has to hold is that the bar then shows the menu of
 * the impersonated user and not the administrator's.
 */
/**
 * **Its own session.** The suite shares one saved login, i.e. one server-side session: a login-as
 * on it would turn every other test, running in parallel, into the impersonated user. This test
 * signs in again in a fresh browser context and impersonates there.
 */
test.use({ storageState: { cookies: [], origins: [] } });

test.describe('Admin navigation — login as', () => {
    test('login-as shows the navigation of the impersonated user', async ({ page, request }) => {
        await startWithBar(page, 'expanded');
        await new LoginPage(page).login(admin1.username, admin1.password);
        const nav = new DotNavigationPage(page);
        await nav.goto('/pages');
        const adminGroups = await nav.groupNames();

        await page.getByTestId('avatar').click();
        await page.getByRole('menuitem', { name: 'Sign In As' }).click();
        // The dialog is appended to <body>, so its controls are not inside the test id of the p-dialog.
        await page.getByTestId('dot-login-as-user-dropdown').click();
        await page
            .getByTestId('dot-login-as-user-item')
            .filter({ hasText: 'Chris' })
            .first()
            .click();
        // The starter does not ask the administrator for this user's password.
        await page.getByTestId('dot-login-as-submit-button').click();

        await nav.waitForGroups();
        const limitedMenu = await request.get('/api/v1/menu', {
            headers: {
                Authorization: generateBase64Credentials(limited1.username, limited1.password)
            }
        });
        const expected = ((await limitedMenu.json()).entity as { tabName: string }[]).map(
            (group) => group.tabName
        );

        // The shell reloads after a login-as; a locator assertion retries across that reload.
        await expect(nav.nav.getByTestId('nav-item-label')).toHaveText(expected);
        expect(expected).not.toEqual(adminGroups);
    });
});
