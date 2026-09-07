import { test as base, type Page } from '@playwright/test';

import { createFolders } from '../requests/folders';
import { getCurrentSite, getDefaultSite, type Site } from '../requests/sites';

/**
 * Shared Playwright fixtures used across portlet/field e2e suites.
 * Feature fixtures should `import { test as base } from './base.fixture'` and extend.
 */
export const test = base.extend<{
    adminPage: Page;
    testSuffix: string;
    apiHelpers: {
        createFolders: (siteName: string, paths: string[]) => Promise<void>;
        getDefaultSite: () => Promise<Site>;
        getCurrentSite: () => Promise<Site>;
    };
}>({
    adminPage: async ({ page }, use) => {
        await use(page);
    },

    testSuffix: async ({}, use) => {
        await use(crypto.randomUUID().slice(0, 8));
    },

    apiHelpers: async ({ request }, use) => {
        await use({
            createFolders: (siteName, paths) => createFolders(request, siteName, paths),
            getDefaultSite: () => getDefaultSite(request),
            getCurrentSite: () => getCurrentSite(request)
        });
    }
});

export { expect } from '@playwright/test';
