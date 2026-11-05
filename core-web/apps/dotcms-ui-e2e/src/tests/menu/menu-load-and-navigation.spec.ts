import { DotNavigationPage } from '@pages';
import { expect, test } from '@playwright/test';
import { getGroupHolding } from '@requests/layouts';

import {
    escapeRegExp,
    menuLinkOf,
    mIdOf,
    NEW_TAB_MODIFIERS,
    startWithBar
} from './helpers/menu-state';

/**
 * Journey: the admin navigation loads once and its clicks behave as they always have (#38078).
 *
 * These journeys pin what users see today, so they pass on the code before the menu consolidation
 * and must keep passing after it. The group that holds Templates (Pages, Templates, Containers,
 * Publishing Queue) is the fixture: every starter has it, it holds both Angular portlets and a
 * legacy JSP one, and it needs no data created. Groups are found by the portlet they hold and not
 * by label, because the starters label them differently (`Site` or `Site Manager`).
 */
/**
 * A portlet of the group that is not the Templates one (`Content Model` in a demo instance, `Schema`
 * in the empty starter). Content Types is never moved by the layout tests that run in parallel,
 * unlike Tags, which they take out of its group for a while.
 */
const MODEL_PORTLET = 'content-types-angular';

test.describe('Admin navigation — load and clicks', () => {
    test('click semantics: Ctrl-click and Cmd-click do not navigate @critical', async ({
        page,
        context,
        request
    }) => {
        await startWithBar(page, 'expanded');
        const site = await getGroupHolding(request, 'templates');
        const nav = new DotNavigationPage(page);
        await nav.goto('/pages');
        await nav.openGroup(site.tabName);
        const before = page.url();

        for (const modifier of NEW_TAB_MODIFIERS) {
            const popup = context.waitForEvent('page', { timeout: 1500 }).catch(() => null);
            await nav.clickItem('Templates', [modifier], site.tabName);
            // The browser may open the link in a new tab; the admin tab itself must stay where it was.
            (await popup)?.close();
            expect(page.url()).toBe(before);
            await expect(nav.activeItem()).toHaveText('Pages');
        }
    });

    test('click semantics: clicking the open Angular item keeps the user on it @critical', async ({
        page,
        request
    }) => {
        await startWithBar(page, 'expanded');
        const site = await getGroupHolding(request, 'templates');
        const nav = new DotNavigationPage(page);
        await nav.goto('/pages');
        await nav.openGroup(site.tabName);
        // Opened from the bar, the URL carries mId; that is what makes the item "the open one".
        await nav.clickItem('Templates', [], site.tabName);
        await expect(nav.activeItem()).toHaveText('Templates');
        const before = page.url();

        await nav.clickItem('Templates', [], site.tabName);

        await expect(nav.activeItem()).toHaveText('Templates');
        expect(page.url()).toBe(before);
    });

    test('click semantics: clicking the open legacy item reloads its iframe @critical', async ({
        page,
        request
    }) => {
        await startWithBar(page, 'expanded');
        const site = await getGroupHolding(request, 'templates');
        const nav = new DotNavigationPage(page);
        await nav.goto('/pages');
        await nav.openGroup(site.tabName);
        await nav.clickItem('Publishing Queue', [], site.tabName);
        await expect(nav.activeItem()).toHaveText('Publishing Queue');
        await expect(page.locator('#detailFrame')).toBeVisible();

        const frameReloaded = page.waitForEvent('framenavigated', {
            predicate: (frame) => frame !== page.mainFrame()
        });
        await nav.clickItem('Publishing Queue', [], site.tabName);

        await frameReloaded;
        await expect(nav.activeItem()).toHaveText('Publishing Queue');
    });

    test('click semantics: expanded bar, clicking a closed group opens it and goes to its first item @critical', async ({
        page,
        request
    }) => {
        await startWithBar(page, 'expanded');
        const model = await getGroupHolding(request, MODEL_PORTLET);
        const firstLink = menuLinkOf(model.menuItems[0]);
        const nav = new DotNavigationPage(page);
        await nav.goto('/pages');

        await nav.openGroup(model.tabName);

        await expect(page).toHaveURL(new RegExp(escapeRegExp(firstLink)));
        expect(mIdOf(page.url())).toBe(model.id.substring(0, 4));
        await expect(nav.groupByName(model.tabName).getByTestId('nav-item-toggle')).toBeVisible();
    });

    test('click semantics: collapsed bar, clicking a group goes to its first item @critical', async ({
        page,
        request
    }) => {
        await startWithBar(page, 'collapsed');
        const model = await getGroupHolding(request, MODEL_PORTLET);
        const firstLink = menuLinkOf(model.menuItems[0]);
        const nav = new DotNavigationPage(page);
        await nav.goto('/pages');
        expect(await nav.isCollapsed()).toBe(true);

        await nav.clickGroupTitle(model.tabName);

        await expect(page).toHaveURL(new RegExp(escapeRegExp(firstLink)));
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
