import { DotNavigationPage } from '@pages';
import { expect, test } from '@playwright/test';
import {
    getGroupHolding,
    getLayouts,
    getMenu,
    restoreLayoutTools,
    setLayoutTools
} from '@requests/layouts';

import { failMenuRequests, restoreMenuRequests, trackMenuRequests } from './helpers/menu-requests';
import { mIdOf, startWithBar } from './helpers/menu-state';

/**
 * Journey: the navigation asks for its menu once, follows live menu changes, and keeps the user's
 * place (#38078).
 *
 * The backend pushes `UPDATE_PORTLET_LAYOUTS` when a section's tools change and every open admin
 * session replaces its menu. That is what these tests provoke, so everything that **counts menu
 * requests** or **changes a layout** lives in this one file and runs serially: nothing in here can
 * see a reload caused by another test of this file. Other files never change layouts, and the
 * layouts are put back in `finally`, so a failing test does not leave the instance altered.
 */
test.describe.configure({ mode: 'serial' });

/**
 * A portlet of the group that is not the Templates one: `Content Model` in a demo instance, `Schema`
 * in the empty starter. The tests below take Tags out of its layout for a while, so the group is
 * found through Content Types, which stays put.
 */
const MODEL_PORTLET = 'content-types-angular';

test.describe('Admin navigation — request count and live updates', () => {
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
        page,
        request
    }) => {
        await startWithBar(page, 'expanded');
        const site = await getGroupHolding(request, 'templates');
        const tracker = trackMenuRequests(page);
        const nav = new DotNavigationPage(page);

        await nav.goto('/pages');
        await expect(nav.activeItem()).toBeVisible();
        await page.waitForLoadState('load');
        tracker.reset();

        await nav.openGroup(site.tabName);
        for (const label of ['Templates', 'Containers', 'Pages']) {
            await nav.clickItem(label, [], site.tabName);
            await expect(nav.activeItem()).toHaveText(label);
            expect(mIdOf(page.url())).toMatch(/^[0-9a-f]{4}$/);
        }

        expect(tracker.count()).toBe(0);
    });

    test('a layout update adds the tool with one reload and keeps the highlight', async ({
        page,
        request
    }) => {
        await startWithBar(page, 'expanded');
        const model = await getGroupHolding(request, MODEL_PORTLET);
        const layouts = await getLayouts(request);
        const target = layouts.find((layout) => layout.id === model.id);
        expect(target, 'the layout of the group that holds Content Types').toBeDefined();
        const original = target?.portletIds ?? [];
        const nav = new DotNavigationPage(page);
        const tracker = trackMenuRequests(page);

        try {
            await nav.goto('/templates');
            await expect(nav.activeItem()).toHaveText('Templates');
            const links = nav.groupLinks(model.tabName);
            const before = await links.count();
            tracker.reset();

            await setLayoutTools(request, target?.id ?? '', [...original, 'query-tool']);

            await expect(links).toHaveCount(before + 1);
            expect(tracker.count()).toBe(1);
            await expect(nav.activeItem()).toHaveText('Templates');
        } finally {
            await restoreLayoutTools(request, target?.id ?? '', original);
        }
    });

    test('a layout update that removes the open portlet leaves nothing highlighted', async ({
        page,
        request
    }) => {
        await startWithBar(page, 'expanded');
        const layouts = await getLayouts(request);
        const target = layouts.find((layout) => layout.portletIds.includes('tags'));
        expect(target, 'the layout that holds the Tags portlet').toBeDefined();
        const original = target?.portletIds ?? [];
        const nav = new DotNavigationPage(page);

        try {
            // Tags lives in one layout only, so removing it removes the portlet from the menu.
            await nav.goto('/tags');
            await expect(nav.activeItem()).toHaveCount(1);
            const label = (await nav.activeItem().innerText()).trim();

            await setLayoutTools(
                request,
                target?.id ?? '',
                original.filter((portletId) => portletId !== 'tags')
            );

            await expect(nav.itemByLabel(label)).toHaveCount(0);
            await expect(nav.activeItem()).toHaveCount(0);
        } finally {
            await restoreLayoutTools(request, target?.id ?? '', original);
        }
    });

    test('a portlet present in two layouts is highlighted in the layout named by mId @critical', async ({
        page,
        request
    }) => {
        await startWithBar(page, 'expanded');
        const siteGroup = await getGroupHolding(request, 'templates');
        const otherGroup = await getGroupHolding(request, MODEL_PORTLET);
        const layouts = await getLayouts(request);
        const site = layouts.find((layout) => layout.id === siteGroup.id);
        const other = layouts.find((layout) => layout.id === otherGroup.id);
        expect(site, 'a layout with the templates portlet').toBeDefined();
        expect(other, 'a layout with the content types portlet').toBeDefined();
        expect(other?.id).not.toBe(site?.id);
        const original = other?.portletIds ?? [];

        try {
            await setLayoutTools(request, other?.id ?? '', [...original, 'templates']);
            const nav = new DotNavigationPage(page);

            await nav.goto(`/templates?mId=${other?.id.substring(0, 4)}`);
            await expect(
                nav.groupSection(otherGroup.tabName).locator('a.dot-nav-sub__link--active')
            ).toHaveText('Templates');

            await nav.goto(`/templates?mId=${site?.id.substring(0, 4)}`);
            await expect(
                nav.groupSection(siteGroup.tabName).locator('a.dot-nav-sub__link--active')
            ).toHaveText('Templates');
        } finally {
            await restoreLayoutTools(request, other?.id ?? '', original);
        }
    });

    test('a portlet removed from the menu is rejected and goes to the first portlet', async ({
        page,
        request
    }) => {
        await startWithBar(page, 'expanded');
        const layouts = await getLayouts(request);
        const target = layouts.find((layout) => layout.portletIds.includes('tags'));
        expect(target, 'the layout that holds the Tags portlet').toBeDefined();
        const original = target?.portletIds ?? [];
        const nav = new DotNavigationPage(page);

        try {
            await nav.goto('/tags');
            await expect(page).toHaveURL(/tags/);

            await setLayoutTools(
                request,
                target?.id ?? '',
                original.filter((portletId) => portletId !== 'tags')
            );
            await expect(nav.nav.getByRole('link', { name: 'Tags', exact: true })).toHaveCount(0);
            await page.goto('/dotAdmin/#/tags');

            // The guard now answers from the updated menu: the portlet is gone, so the user lands on
            // the first portlet of the menu instead.
            await expect(page).not.toHaveURL(/tags/);
        } finally {
            await restoreLayoutTools(request, target?.id ?? '', original);
        }
    });

    test('a legacy portlet reads its iframe url at open time and an Angular one asks for nothing', async ({
        page,
        request
    }) => {
        await startWithBar(page, 'expanded');
        const site = await getGroupHolding(request, 'templates');
        const tracker = trackMenuRequests(page);
        const nav = new DotNavigationPage(page);
        await nav.goto('/pages');
        await expect(nav.activeItem()).toHaveText('Pages');
        await nav.openGroup(site.tabName);
        tracker.reset();

        // The legacy url carries `r=<currentTimeMillis>`, so it is requested when the portlet opens.
        await nav.clickItem('Publishing Queue', [], site.tabName);
        // The frame is visible a moment before its `src` is set, so the url is waited for.
        await expect(page.locator('#detailFrame')).toHaveAttribute('src', /[?&]r=\d+/);
        const firstSrc = await page.locator('#detailFrame').getAttribute('src');
        expect(tracker.count()).toBe(1);

        // An Angular portlet navigates with its menuLink and never goes through that url.
        tracker.reset();
        await nav.clickItem('Templates', [], site.tabName);
        await expect(nav.activeItem()).toHaveText('Templates');
        expect(tracker.count()).toBe(0);

        // Opening the legacy portlet again reads a new url, with a different timestamp.
        await nav.clickItem('Publishing Queue', [], site.tabName);
        await expect(page.locator('#detailFrame')).toBeVisible();
        await expect
            .poll(() => page.locator('#detailFrame').getAttribute('src'))
            .not.toBe(firstSrc);
        expect(tracker.count()).toBe(1);
    });

    test('a first load that fails leaves an empty navigation and the shell keeps running', async ({
        page
    }) => {
        const errors: string[] = [];
        page.on('pageerror', (error) => errors.push(error.message));
        await failMenuRequests(page);

        await page.goto('/dotAdmin/#/pages');
        await page.waitForLoadState('load');

        await expect(page.getByTestId('nav-item')).toHaveCount(0);
        expect(errors).toEqual([]);
    });

    test('a reload that fails keeps the navigation that was showing', async ({ page, request }) => {
        await startWithBar(page, 'expanded');
        const errors: string[] = [];
        page.on('pageerror', (error) => errors.push(error.message));
        const model = await getGroupHolding(request, MODEL_PORTLET);
        const layouts = await getLayouts(request);
        const target = layouts.find((layout) => layout.id === model.id);
        expect(target, 'the layout of the group that holds Content Types').toBeDefined();
        const original = target?.portletIds ?? [];
        const nav = new DotNavigationPage(page);
        const tracker = trackMenuRequests(page);

        try {
            await nav.goto('/pages');
            const before = await nav.groupNames();
            tracker.reset();
            await failMenuRequests(page);

            await setLayoutTools(request, target?.id ?? '', [...original, 'query-tool']);
            await expect.poll(() => tracker.count()).toBeGreaterThan(0);

            expect(await nav.groupNames()).toEqual(before);
            expect(errors).toEqual([]);
        } finally {
            await restoreMenuRequests(page);
            await restoreLayoutTools(request, target?.id ?? '', original);
        }
    });
});
