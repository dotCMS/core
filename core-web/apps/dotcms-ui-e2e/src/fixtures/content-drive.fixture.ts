import { type APIRequestContext } from '@playwright/test';

import { type BaseApiHelpers, test as base } from './base.fixture';

import { deleteFolders } from '../requests/folders';

/** The shared helpers plus the teardown Content Drive needs for the folders it seeds. */
export interface ContentDriveApiHelpers extends BaseApiHelpers {
    deleteFolders: (siteName: string, paths: string[]) => Promise<void>;
}

/**
 * Content Drive e2e fixture — the shared base helpers, plus folder teardown.
 *
 * `deleteFolders` is not a convenience. Folders seeded on the shared demo site survive the run, and
 * a tree level and a search result set are both paged at 40 and sorted by name, so enough leaked
 * folders eventually push the one a later run just created off the first page. Tests then fail while
 * the product is fine.
 *
 * The argument types are spelled out because overriding a fixture with a widened type collapses
 * Playwright 1.36's inference for this callback, leaving every parameter an implicit `any`.
 */
export const test = base.extend<{ apiHelpers: ContentDriveApiHelpers }>({
    apiHelpers: async (
        {
            request,
            apiHelpers
        }: {
            request: APIRequestContext;
            apiHelpers: BaseApiHelpers;
        },
        use: (helpers: ContentDriveApiHelpers) => Promise<void>
    ) => {
        await use({
            ...apiHelpers,
            deleteFolders: (siteName: string, paths: string[]) =>
                deleteFolders(request, siteName, paths)
        });
    }
});

export { expect } from './base.fixture';
