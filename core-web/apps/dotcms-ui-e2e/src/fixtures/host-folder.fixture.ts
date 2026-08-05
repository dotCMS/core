import { test as base } from './base.fixture';

import {
    type ContentType,
    type CreateContentTypePayload,
    createFakeContentType,
    deleteContentType
} from '../requests/contentType';
import { createFolders } from '../requests/folders';
import { getCurrentSite, getDefaultSite, type Site } from '../requests/sites';
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
    const basePath = parentPath ? `${parentPath}/` : '/';

    return Array.from({ length: count }, (_, index) => `${basePath}${prefix}-${index + 1}`);
}

export const test = base.extend<{
    apiHelpers: {
        createContentType: (payload: CreateContentTypePayload) => Promise<ContentType>;
        deleteContentType: (id: string) => Promise<void>;
        createFolders: (siteName: string, paths: string[]) => ReturnType<typeof createFolders>;
        buildFolderPaths: (prefix: string, count: number, parentPath?: string) => string[];
        getDefaultSite: () => Promise<Site>;
        getCurrentSite: () => Promise<Site>;
        hostFolderPayload: (suffix: string) => CreateContentTypePayload;
    };
}>({
    apiHelpers: async ({ request }, use) => {
        await use({
            createContentType: (payload) => createFakeContentType(request, payload),
            deleteContentType: (id) => deleteContentType(request, id),
            createFolders: (siteName, paths) => createFolders(request, siteName, paths),
            buildFolderPaths: (prefix, count, parentPath) =>
                buildFolderPaths(prefix, count, parentPath),
            getDefaultSite: () => getDefaultSite(request),
            getCurrentSite: () => getCurrentSite(request),
            hostFolderPayload: (suffix: string) => hostFolderContentTypePayload(suffix)
        });
    }
});

export { expect } from './base.fixture';
