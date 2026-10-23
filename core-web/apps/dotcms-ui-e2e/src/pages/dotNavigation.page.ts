import { Locator, Page, expect } from '@playwright/test';

/**
 * The admin side navigation (`dot-main-nav`): tool groups with their portlet links.
 *
 * A group is a `nav-item`; its portlet links are `<a>` elements inside the group's sub-nav. The
 * links carry no test id, so they are found by role inside the navigation and the active one by the
 * `dot-nav-sub__link--active` class the template sets — the only CSS used here, kept in one place
 * so it is a one-line change when the template gains a test id.
 */
export class DotNavigationPage {
    constructor(private readonly page: Page) {}

    /** Opens the admin shell and waits until the navigation has at least one group. */
    async goto(hash = '') {
        await this.page.goto(`/dotAdmin/#${hash}`);
        await this.waitForGroups();
    }

    /** The side navigation landmark (the page has other `navigation` regions, e.g. breadcrumbs). */
    get nav(): Locator {
        return this.page.locator('dot-main-nav').getByRole('navigation');
    }

    /** Every tool group, in the order the bar shows them. */
    get groups(): Locator {
        return this.nav.getByTestId('nav-item');
    }

    async waitForGroups() {
        await expect(this.groups.first()).toBeVisible({ timeout: 30000 });
    }

    /** The group whose label is exactly `name`. */
    groupByName(name: string): Locator {
        return this.groups.filter({
            has: this.page.getByTestId('nav-item-label').getByText(name, { exact: true })
        });
    }

    /**
     * The whole `dot-nav-item` of a group: its title **and** its portlet links. The `nav-item` test id
     * is only the title row, so links have to be looked up from here.
     */
    groupSection(name: string): Locator {
        return this.nav.locator('dot-nav-item').filter({
            has: this.page.getByTestId('nav-item-label').getByText(name, { exact: true })
        });
    }

    /** The portlet links of one group. */
    groupLinks(name: string): Locator {
        return this.groupSection(name).locator('a.dot-nav-sub__link');
    }

    /** Labels of the groups in on-screen order. */
    async groupNames(): Promise<string[]> {
        const labels = await this.nav.getByTestId('nav-item-label').allInnerTexts();

        return labels.map((label) => label.trim());
    }

    /**
     * The portlet link labelled `label`: the first match across groups, or inside the group named
     * `group` when a portlet is in several groups (Publishing Queue is in two in the empty starter).
     */
    itemByLabel(label: string, group?: string): Locator {
        const scope = group ? this.groupSection(group) : this.nav;

        return scope.getByRole('link', { name: label, exact: true }).first();
    }

    /** The portlet link that is currently highlighted. */
    activeItem(): Locator {
        return this.nav.locator('a.dot-nav-sub__link--active');
    }

    /** The header button that collapses/expands the bar. */
    get collapseToggle(): Locator {
        return this.page.getByTestId('dot-nav-header-toggle-button');
    }

    /** True when the bar is collapsed (the `<nav>` carries the `collapsed` class). */
    async isCollapsed(): Promise<boolean> {
        return (await this.nav.getAttribute('class'))?.split(/\s+/).includes('collapsed') ?? false;
    }

    /** True when the group is the open one (its `nav-item` carries `dot-nav__item--active`). */
    async isGroupOpen(name: string): Promise<boolean> {
        const classes = await this.groupByName(name).getAttribute('class');

        return classes?.split(/\s+/).includes('dot-nav__item--active') ?? false;
    }

    /**
     * Opens a group on an expanded bar so its links can be clicked. Clicking an open group closes it,
     * and the group of the current portlet is already open, so it only clicks when it is closed.
     */
    async openGroup(name: string) {
        if (!(await this.isGroupOpen(name))) {
            await this.groupByName(name).getByTestId('nav-item-main').click();
        }
        await expect(this.groupByName(name)).toHaveClass(/dot-nav__item--active/);
    }

    /**
     * Clicks a group's title as the user does: on an expanded bar it toggles/opens the group, on a
     * collapsed bar it goes to the group's first item. The collapsed bar draws its hover flyout over
     * the title, so a coordinate click would land on the flyout; the event is sent to the title itself.
     */
    async clickGroupTitle(name: string) {
        await this.groupByName(name).getByTestId('nav-item-main').dispatchEvent('click');
    }

    /** Clicks a portlet link. Pass modifiers to test Ctrl/Cmd-click, `group` to pick the group. */
    async clickItem(label: string, modifiers: ('Control' | 'Meta')[] = [], group?: string) {
        await this.itemByLabel(label, group).click({ modifiers });
    }
}
