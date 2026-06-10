import { test as base, type Page } from '@playwright/test';

import { createFakeHostFolderField, createFakeTextField } from '@dotcms/utils-testing';

import {
    type ContentType,
    type CreateContentTypePayload,
    createFakeContentType,
    deleteContentType
} from '../requests/contentType';
import { createFolders } from '../requests/folders';
import { getSites, type Site } from '../requests/sites';

// ─── Content Type Payload Builder ────────────────────────────────

function hostFolderContentTypePayload(suffix: string): CreateContentTypePayload {
    const titleField = createFakeTextField({
        name: 'Title',
        variable: 'title',
        sortOrder: 1
    });
    const siteOrFolderField = createFakeHostFolderField({
        name: 'Site Or Folder',
        variable: 'siteOrFolder',
        required: true,
        sortOrder: 2
    });

    return {
        name: `HostFolderTest${suffix}`,
        fields: [titleField, siteOrFolderField]
    };
}

// ─── Fixture ─────────────────────────────────────────────────────

export const test = base.extend<{
    adminPage: Page;
    testSuffix: string;
    apiHelpers: {
        createContentType: (payload: CreateContentTypePayload) => Promise<ContentType>;
        deleteContentType: (id: string) => Promise<void>;
        createFolders: (siteName: string, paths: string[]) => Promise<void>;
        getDefaultSite: () => Promise<Site>;
        hostFolderPayload: (suffix: string) => CreateContentTypePayload;
    };
}>({
    adminPage: async ({ page }, use) => {
        await use(page);
    },

    // eslint-disable-next-line no-empty-pattern
    testSuffix: async ({}, use) => {
        await use(crypto.randomUUID().slice(0, 8));
    },

    apiHelpers: async ({ request }, use) => {
        await use({
            createContentType: (payload) => createFakeContentType(request, payload),
            deleteContentType: (id) => deleteContentType(request, id),
            createFolders: (siteName, paths) => createFolders(request, siteName, paths),
            getDefaultSite: async () => {
                const sites = await getSites(request);
                const site = sites.find((s) => s.default);
                if (!site) {
                    throw new Error('No default site found');
                }
                return site;
            },
            hostFolderPayload: (suffix: string) => hostFolderContentTypePayload(suffix)
        });
    }
});

export { expect } from '@playwright/test';
