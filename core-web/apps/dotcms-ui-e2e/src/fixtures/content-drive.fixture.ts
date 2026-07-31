import { test as base, type Page } from '@playwright/test';

import { type CreatedFolder, createFolders } from '../requests/folders';
import { getCurrentSite, getSites, type Site } from '../requests/sites';

export const test = base.extend<{
    adminPage: Page;
    testSuffix: string;
    apiHelpers: {
        createFolders: (siteName: string, paths: string[]) => Promise<CreatedFolder[]>;
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
            getDefaultSite: async () => {
                const sites = await getSites(request);
                const site = sites.find((s) => s.default);
                if (!site) {
                    throw new Error('No default site found');
                }
                return site;
            },
            getCurrentSite: () => getCurrentSite(request)
        });
    }
});

export { expect } from '@playwright/test';
