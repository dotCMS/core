import { type APIRequestContext } from '@playwright/test';

import { type BaseApiHelpers, test as base } from './base.fixture';

import { createFilteredFolder, deleteFolders } from '../requests/folders';
import { waitForFolderJobsToSettle } from '../requests/jobs';
import { clearNotifications } from '../requests/notifications';

/** The shared helpers plus the teardown Content Drive needs for the folders it seeds. */
export interface ContentDriveApiHelpers extends BaseApiHelpers {
    deleteFolders: (siteName: string, paths: string[]) => Promise<void>;
    /**
     * Waits until this folder's uploads have finished, which a visible row does not prove.
     *
     * Scoped to one folder on purpose: waiting on every job would make one test wait out another's
     * uploads, and the suite runs two workers against a single instance.
     */
    waitForFolderJobsToSettle: (siteName: string, folderPath: string) => Promise<void>;
    /** Dismisses every notification, so "one arrived" is a claim about this run. */
    clearNotifications: () => Promise<void>;
    /** Seeds a folder that only admits the given file-name globs, for the folder-filter refusal. */
    createFilteredFolder: (siteName: string, path: string, fileMasks: string[]) => Promise<void>;
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
                deleteFolders(request, siteName, paths),
            createFilteredFolder: (siteName: string, path: string, fileMasks: string[]) =>
                createFilteredFolder(request, siteName, path, fileMasks),
            clearNotifications: () => clearNotifications(request),
            waitForFolderJobsToSettle: (siteName: string, folderPath: string) =>
                waitForFolderJobsToSettle(request, siteName, folderPath)
        });
    }
});

export { expect } from './base.fixture';
