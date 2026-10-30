import { DotNavigationPage } from '@pages';
import { expect, test } from '@playwright/test';
import { getGroupHolding, getLayouts } from '@requests/layouts';

import { startWithBar } from './helpers/menu-state';

/**
 * Journey: a browser reload and a deep link bring the user back where they were (#38078).
 *
 * After F5 the Angular memory is gone: only the collapsed/expanded choice comes back (from
 * `localStorage`), and the highlighted portlet is whichever one the URL names, resolved once the
 * menu has loaded. These pin today's behavior and must keep passing through the consolidation.
 */
test.describe('Admin navigation — reload and deep links', () => {
    test('browser reload restores the expanded bar and highlights the URL portlet @critical', async ({
        page,
        request
    }) => {
        await startWithBar(page, 'expanded');
        const site = await getGroupHolding(request, 'templates');
        const nav = new DotNavigationPage(page);
        await nav.goto('/pages');
        await nav.openGroup(site.tabName);
        await nav.clickItem('Templates', [], site.tabName);
        await expect(nav.activeItem()).toHaveText('Templates');

        await page.reload();
        await nav.waitForGroups();

        expect(await nav.isCollapsed()).toBe(false);
        await expect(nav.activeItem()).toHaveText('Templates');
    });

    test('browser reload restores the collapsed bar @critical', async ({ page }) => {
        await startWithBar(page, 'expanded');
        const nav = new DotNavigationPage(page);
        await nav.goto('/templates');
        await nav.collapseToggle.click();
        await expect.poll(() => nav.isCollapsed()).toBe(true);

        await page.reload();
        await nav.waitForGroups();

        expect(await nav.isCollapsed()).toBe(true);
    });

    test('a deep link without mId highlights the matching portlet @critical', async ({ page }) => {
        await startWithBar(page, 'expanded');
        const nav = new DotNavigationPage(page);

        await nav.goto('/templates');

        await expect(nav.activeItem()).toHaveText('Templates');
    });

    test('a deep link with mId highlights the portlet of that layout @critical', async ({
        page,
        request
    }) => {
        await startWithBar(page, 'expanded');
        const layouts = await getLayouts(request);
        const site = layouts.find((layout) => layout.portletIds.includes('templates'));
        expect(site, 'no layout holds the templates portlet').toBeDefined();
        const nav = new DotNavigationPage(page);

        await nav.goto(`/templates?mId=${site?.id.substring(0, 4)}`);

        await expect(nav.activeItem()).toHaveText('Templates');
    });
});
