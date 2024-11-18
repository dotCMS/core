import {Page, Locator} from '@playwright/test';

export class GroupEntriesLocators {
    readonly SITE: Locator;
    readonly CONTENT: Locator;

    constructor(page: Page) {
        this.SITE = page.getByText('Site', {exact: true});
        this.CONTENT = page.getByRole('complementary').getByText('Content', {exact: true});
    }
}

/**
 * Locators for the tools in the menu
 */
export class ToolEntriesLocators {
    readonly SEARCH_ALL: Locator;
    readonly CONTENT_ACTIVITIES: Locator;
    readonly CONTENT_BANNERS: Locator;
    readonly CONTENT_BLOGS: Locator;
    readonly CONTENT_CALL_TO_ACTIONS: Locator;
    readonly CONTENT_DESTINATIONS: Locator

    constructor(page: Page) {
        this.SEARCH_ALL = page.getByRole('link', { name: 'Search All' });
        this.CONTENT_ACTIVITIES = page.getByRole('link', {name: 'Activities'});
        this.CONTENT_BANNERS = page.getByRole('link', {name: 'Banners'});
        this.CONTENT_BLOGS = page.getByRole('link', {name: 'Blogs'});
        this.CONTENT_CALL_TO_ACTIONS = page.getByRole('link', {name: 'Call to Actions'});
        this.CONTENT_DESTINATIONS = page.getByRole('link', {name: 'Destinations'});
    }
}

/**
 * Locators for the menu entries
 */
export class MenuEntriesLocators {
    readonly EXPAND: Locator;
    readonly COLLAPSE: Locator;

    constructor(page: Page) {
        this.EXPAND = page.locator('button[ng-reflect-ng-class="[object Object]"]');
        this.COLLAPSE =  page.locator('button[ng-reflect-ng-class="[object Object]"]');

    }
}