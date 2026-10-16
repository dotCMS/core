import { DotNavigationPage } from '@pages';
import { expect, test } from '@playwright/test';
import { getLayouts, getMenu } from '@requests/layouts';

import { trackMenuRequests } from './helpers/menu-requests';
import { mIdOf, startWithBar } from './helpers/menu-state';

/**
 * Journey: the admin navigation loads once and its clicks behave as they always have (#38078).
 *
 * These journeys pin what users see today, so they pass on the code before the menu consolidation
 * and must keep passing after it. The starter instance's `Site` group (Pages, Templates, Containers,
 * Publishing Queue) is the fixture: it is part of every starter, it holds both Angular portlets and
 * a legacy JSP one, and it needs no data created.
 */
test.describe('Admin navigation — load and clicks', () => {
    test('load shows the groups in tabOrder with one menu request @critical', async ({
        page,
        request
    }) => {
        const tracker = trackMenuRequests(page);
        const nav = new DotNavigationPage(page);

        await nav.goto('/pages');
        await expect(nav.activeItem()).toBeVisible();
        await page.waitForLoadState('load');
        const groups = await getMenu(request);

        // The groups on screen follow the menu response, and that response follows the layouts' tabOrder.
        expect(await nav.groupNames()).toEqual(groups.map((group) => group.tabName));
        const layouts = await getLayouts(request);
        const expectedOrder = layouts
            .filter((layout) => groups.some((group) => group.id === layout.id))
            .sort((a, b) => a.tabOrder - b.tabOrder)
            .map((layout) => layout.id);
        expect(groups.map((group) => group.id)).toEqual(expectedOrder);

        expect(tracker.count()).toBe(1);
    });

    test('moving across three portlets from the bar sends no further menu request @critical', async ({
        page
    }) => {
        await startWithBar(page, 'expanded');
        const tracker = trackMenuRequests(page);
        const nav = new DotNavigationPage(page);

        await nav.goto('/pages');
        await expect(nav.activeItem()).toBeVisible();
        await page.waitForLoadState('load');
        tracker.reset();

        await nav.openGroup('Site');
        for (const label of ['Templates', 'Containers', 'Pages']) {
            await nav.clickItem(label);
            await expect(nav.activeItem()).toHaveText(label);
            expect(mIdOf(page.url())).toMatch(/^[0-9a-f]{4}$/);
        }

        expect(tracker.count()).toBe(0);
    });

    test('click semantics: Ctrl-click and Cmd-click do not navigate @critical', async ({
        page,
        context
    }) => {
        await startWithBar(page, 'expanded');
        const nav = new DotNavigationPage(page);
        await nav.goto('/pages');
        await nav.openGroup('Site');
        const before = page.url();

        for (const modifier of ['Control', 'Meta'] as const) {
            const popup = context.waitForEvent('page', { timeout: 1500 }).catch(() => null);
            await nav.clickItem('Templates', [modifier]);
            // The browser may open the link in a new tab; the admin tab itself must stay where it was.
            (await popup)?.close();
            expect(page.url()).toBe(before);
            await expect(nav.activeItem()).toHaveText('Pages');
        }
    });

    test('click semantics: clicking the open Angular item keeps the user on it @critical', async ({
        page
    }) => {
        await startWithBar(page, 'expanded');
        const nav = new DotNavigationPage(page);
        await nav.goto('/pages');
        await nav.openGroup('Site');
        // Opened from the bar, the URL carries mId; that is what makes the item "the open one".
        await nav.clickItem('Templates');
        await expect(nav.activeItem()).toHaveText('Templates');
        const before = page.url();

        await nav.clickItem('Templates');

        await expect(nav.activeItem()).toHaveText('Templates');
        expect(page.url()).toBe(before);
    });

    test('click semantics: clicking the open legacy item reloads its iframe @critical', async ({
        page
    }) => {
        await startWithBar(page, 'expanded');
        const nav = new DotNavigationPage(page);
        await nav.goto('/pages');
        await nav.openGroup('Site');
        await nav.clickItem('Publishing Queue');
        await expect(nav.activeItem()).toHaveText('Publishing Queue');
        await expect(page.locator('#detailFrame')).toBeVisible();

        const frameReloaded = page.waitForEvent('framenavigated', {
            predicate: (frame) => frame !== page.mainFrame()
        });
        await nav.clickItem('Publishing Queue');

        await frameReloaded;
        await expect(nav.activeItem()).toHaveText('Publishing Queue');
    });

    test('click semantics: expanded bar, clicking a closed group opens it and goes to its first item @critical', async ({
        page
    }) => {
        await startWithBar(page, 'expanded');
        const nav = new DotNavigationPage(page);
        await nav.goto('/pages');
        const layouts = await getLayouts(page.request);
        const settings = layouts.find((layout) => layout.name === 'Content Model');
        expect(settings, 'the starter Content Model group is missing').toBeDefined();

        await nav.openGroup('Content Model');

        await expect(page).toHaveURL(/content-types-angular/);
        expect(mIdOf(page.url())).toBe(settings?.id.substring(0, 4));
        await expect(nav.groupByName('Content Model').getByTestId('nav-item-toggle')).toBeVisible();
    });

    test('click semantics: collapsed bar, clicking a group goes to its first item @critical', async ({
        page
    }) => {
        await startWithBar(page, 'collapsed');
        const nav = new DotNavigationPage(page);
        await nav.goto('/pages');
        expect(await nav.isCollapsed()).toBe(true);

        await nav.clickGroupTitle('Content Model');

        await expect(page).toHaveURL(/content-types-angular/);
    });

    test('click semantics: the collapse control toggles the bar and remembers it @critical', async ({
        page
    }) => {
        await startWithBar(page, 'expanded');
        const nav = new DotNavigationPage(page);
        await nav.goto('/pages');
        expect(await nav.isCollapsed()).toBe(false);

        await nav.collapseToggle.click();
        await expect.poll(() => nav.isCollapsed()).toBe(true);
        expect(await page.evaluate(() => localStorage.getItem('dotcms.menu.status'))).toBe('true');

        await nav.collapseToggle.click();
        await expect.poll(() => nav.isCollapsed()).toBe(false);
        expect(await page.evaluate(() => localStorage.getItem('dotcms.menu.status'))).toBe('false');
    });
});
