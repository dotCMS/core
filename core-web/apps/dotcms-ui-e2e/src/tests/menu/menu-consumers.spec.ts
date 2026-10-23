import { DotNavigationPage } from '@pages';
import { expect, test } from '@playwright/test';

import { startWithBar } from './helpers/menu-state';

/**
 * Journey: the places that ask "is this portlet in the menu?" (#38078).
 *
 * My Account starts its "Show Getting Started" checkbox checked only when the `starter` portlet is in
 * the user's menu. The starter admin has it (the `Getting Started` group).
 */
test.describe('Admin navigation — consumers of the menu', () => {
    test('My Account shows Getting Started checked when starter is in the menu', async ({
        page
    }) => {
        await startWithBar(page, 'expanded');
        const nav = new DotNavigationPage(page);
        await nav.goto('/pages');
        await expect(nav.groupByName('Getting Started')).toBeVisible();

        await page.getByTestId('avatar').click();
        await page.getByRole('menuitem', { name: 'My Account' }).click();

        const checkbox = page.getByTestId('dot-my-account-show-starter-checkbox');
        await expect(checkbox).toBeVisible();
        await expect(checkbox.getByRole('checkbox')).toBeChecked();
    });
});
