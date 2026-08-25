import { test as base } from './base.fixture';

import {
    type ContentType,
    type CreateContentTypePayload,
    createFakeContentType,
    deleteContentType
} from '../requests/contentType';
import { createFolders, deleteFolders } from '../requests/folders';
import { createSite, deleteSite, getDefaultSite, type Site } from '../requests/sites';
import {
    createFakePayloadImageField,
    createFakePayloadTextField
} from '../utils/dot-content-types.mock';

// ─── Content Type Payload Builder ────────────────────────────────

/**
 * An Image field is the cheapest way into the AssetPicker: "Select Existing Image" opens the same
 * dialog the Story Block and the WYSIWYG open, and it needs no editor bootstrapping to reach.
 */
function assetPickerContentTypePayload(suffix: string): CreateContentTypePayload {
    return {
        name: `AssetPickerSidebarTest${suffix}`,
        fields: [
            createFakePayloadTextField({
                name: 'Title',
                variable: 'title',
                sortOrder: 1
            }),
            createFakePayloadImageField({
                name: 'Image',
                variable: 'image',
                sortOrder: 2
            })
        ]
    };
}

// ─── Fixture ─────────────────────────────────────────────────────

/**
 * Fixtures for the AssetPicker sidebar journey (#37208).
 *
 * The sidebar is about **which site** and **which folder**, so the two things this adds over
 * `host-folder.fixture` are a **second site** and a way to seed nested folders on either of them:
 *
 * - "changing the site re-scopes the tree and the asset list" (FR-005) needs two sites to change
 *   between, and a demo install ships with exactly one.
 * - "results are scoped to the selected site only" (FR-011) is unfalsifiable on a single site — a
 *   passing assertion would prove nothing.
 *
 * Sites are created **and published** by `createSite`; an unpublished site is not browsable and
 * never reaches the selector. Tests own the teardown via `deleteSite`, because a leaked site stays
 * in every later run's site list and silently changes what the selector shows.
 */
export const test = base.extend<{
    apiHelpers: {
        createContentType: (payload: CreateContentTypePayload) => Promise<ContentType>;
        deleteContentType: (id: string) => Promise<void>;
        createFolders: (siteName: string, paths: string[]) => ReturnType<typeof createFolders>;
        deleteFolders: (siteName: string, paths: string[]) => Promise<void>;
        createSite: (hostname: string) => ReturnType<typeof createSite>;
        deleteSite: (identifier: string) => Promise<void>;
        getDefaultSite: () => Promise<Site>;
        assetPickerPayload: (suffix: string) => CreateContentTypePayload;
    };
}>({
    apiHelpers: async ({ request }, use) => {
        await use({
            createContentType: (payload) => createFakeContentType(request, payload),
            deleteContentType: (id) => deleteContentType(request, id),
            createFolders: (siteName, paths) => createFolders(request, siteName, paths),
            deleteFolders: (siteName, paths) => deleteFolders(request, siteName, paths),
            createSite: (hostname) => createSite(request, hostname),
            deleteSite: (identifier) => deleteSite(request, identifier),
            getDefaultSite: () => getDefaultSite(request),
            assetPickerPayload: (suffix: string) => assetPickerContentTypePayload(suffix)
        });
    }
});

export { expect } from './base.fixture';
