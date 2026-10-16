import { DotNavigationPage } from '@pages';
import { expect, test } from '@playwright/test';
import { getMenu } from '@requests/layouts';

import { menuLinkOf, mIdOf, startWithBar } from './helpers/menu-state';

/**
 * Journey: routes the user's menu does not hold are rejected the way they always were (#38078).
 *
 * The starter admin has Content Drive but neither Content Search (`content`) nor Site Browser in
 * its menu, which is exactly the situation the guard's redirect rules describe.
 */
test.describe('Admin navigation — guards', () => {
    test('a portlet absent from the menu goes to the first portlet @critical', async ({
        page,
        request
    }) => {
        await startWithBar(page, 'expanded');
        const nav = new DotNavigationPage(page);

        await nav.goto('/c/a-portlet-the-menu-does-not-have');
        const groups = await getMenu(request);
        const first = groups[0].menuItems[0];
        const firstLink = menuLinkOf(first);

        await expect(page).toHaveURL(new RegExp(firstLink.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
        expect(mIdOf(page.url())).toBe(groups[0].id.substring(0, 4));
        await expect(nav.activeItem()).toHaveCount(1);
    });

    for (const legacyUrl of ['/c/content', '/c/site-browser']) {
        test(`${legacyUrl} lands on Content Drive when it is not in the menu @critical`, async ({
            page
        }) => {
            await startWithBar(page, 'expanded');
            const nav = new DotNavigationPage(page);

            await nav.goto(legacyUrl);

            await expect(page).toHaveURL(/content-drive/);
        });
    }
});
