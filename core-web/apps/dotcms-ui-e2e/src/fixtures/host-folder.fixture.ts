import { test as base, type Page } from '@playwright/test';

import {
    type ContentType,
    type CreateContentTypePayload,
    createFakeContentType,
    deleteContentType
} from '../requests/contentType';
import { createFolders } from '../requests/folders';
import { getCurrentSite, getSites, type Site } from '../requests/sites';
import {
    createFakePayloadHostFolderField,
    createFakePayloadTextField
} from '../utils/dot-content-types.mock';

// ─── Content Type Payload Builder ────────────────────────────────

function hostFolderContentTypePayload(suffix: string): CreateContentTypePayload {
    return {
        name: `HostFolderTest${suffix}`,
        fields: [
            createFakePayloadTextField({
                name: 'Title',
                variable: 'title',
                sortOrder: 1
            }),
            createFakePayloadHostFolderField({
                name: 'Site Or Folder',
                variable: 'siteOrFolder',
                required: true,
                sortOrder: 2
            })
        ]
    };
}

// ─── Fixture ─────────────────────────────────────────────────────

function buildFolderPaths(prefix: string, count: number, parentPath = ''): string[] {
    const base = parentPath ? `${parentPath}/` : '/';

    return Array.from({ length: count }, (_, index) => `${base}${prefix}-${index + 1}`);
}

export const test = base.extend<{
    adminPage: Page;
    testSuffix: string;
    apiHelpers: {
        createContentType: (payload: CreateContentTypePayload) => Promise<ContentType>;
        deleteContentType: (id: string) => Promise<void>;
        createFolders: (siteName: string, paths: string[]) => Promise<void>;
        createManyFolders: (siteName: string, paths: string[]) => Promise<void>;
        buildFolderPaths: (prefix: string, count: number, parentPath?: string) => string[];
        getDefaultSite: () => Promise<Site>;
        getCurrentSite: () => Promise<Site>;
        hostFolderPayload: (suffix: string) => CreateContentTypePayload;
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
            createContentType: (payload) => createFakeContentType(request, payload),
            deleteContentType: (id) => deleteContentType(request, id),
            createFolders: (siteName, paths) => createFolders(request, siteName, paths),
            createManyFolders: (siteName, paths) => createFolders(request, siteName, paths),
            buildFolderPaths: (prefix, count, parentPath) =>
                buildFolderPaths(prefix, count, parentPath),
            getDefaultSite: async () => {
                const sites = await getSites(request);
                const site = sites.find((s) => s.default);
                if (!site) {
                    throw new Error('No default site found');
                }
                return site;
            },
            getCurrentSite: () => getCurrentSite(request),
            hostFolderPayload: (suffix: string) => hostFolderContentTypePayload(suffix)
        });
    }
});

export { expect } from '@playwright/test';
