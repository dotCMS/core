import { DotNavigationPage, LoginPage } from '@pages';
import { expect, test } from '@playwright/test';
import { createBackendUser, deleteUser, signIn, TestUser } from '@requests/users';
import { admin1 } from '@utils/credentials';

import { startWithBar } from './helpers/menu-state';

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
    test('login-as shows the navigation of the impersonated user', async ({
        page,
        playwright,
        baseURL
    }) => {
        // The empty starter has only the administrator, so the person to impersonate is created.
        // Its role holds every layout but `Getting Started`, so its navigation is not the admin's.
        const suffix = Date.now();
        const user: TestUser = {
            id: `e2e-menu-login-as-${suffix}`,
            firstName: 'Menu',
            lastName: `Impersonated${suffix}`,
            email: `e2e-menu-login-as-${suffix}@dotcms.com`,
            password: 'e2e-menu-pass'
        };
        // With no saved login, `request` has no session. The administrator signs in on a context
        // of its own, apart from the browser's session the login-as below turns into the user's.
        const asAdmin = await playwright.request.newContext({ baseURL });
        await signIn(asAdmin, admin1.username, admin1.password);
        await createBackendUser(asAdmin, user);

        try {
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
                .filter({ hasText: user.lastName })
                .first()
                .click();
            // An administrator account asks to confirm the signed-in administrator's password.
            await page.getByTestId('dot-login-as-password-input').fill(admin1.password);
            await page.getByTestId('dot-login-as-submit-button').click();

            await nav.waitForGroups();
            // A third session, signed in as the user itself: what the server gives that user is
            // read apart from the browser session under test.
            const asUser = await playwright.request.newContext({ baseURL });
            await signIn(asUser, user.email, user.password);
            const impersonatedMenu = await asUser.get('/api/v1/menu');
            expect(impersonatedMenu.status()).toBe(200);
            const expected = ((await impersonatedMenu.json()).entity as { tabName: string }[]).map(
                (group) => group.tabName
            );
            await asUser.dispose();

            // The shell reloads after a login-as; a locator assertion retries across that reload.
            await expect(nav.nav.getByTestId('nav-item-label')).toHaveText(expected);
            expect(expected).not.toEqual(adminGroups);
        } finally {
            await deleteUser(asAdmin, user.id);
            await asAdmin.dispose();
        }
    });
});
