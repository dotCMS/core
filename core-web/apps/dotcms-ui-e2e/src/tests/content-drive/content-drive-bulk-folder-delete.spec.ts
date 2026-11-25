import { ContentDrivePage } from '@pages';
import { type Page } from '@playwright/test';

import {
    FOLDER_RUN_URL,
    FolderRunPreview,
    acceptSubmissionsWithoutRunning,
    advertiseFolderCeilings,
    expectSeen,
    recordFromNextLoad,
    refuseFolderRunsWith
} from './helpers/content-drive-folder-runs';

import { type ContentDriveApiHelpers, expect, test } from '../../fixtures/content-drive.fixture';

/**
 * Journey: Content Drive bulk folder delete (#37063).
 *
 * What a unit test cannot reach, and the point of the feature:
 *
 * 1. **Both surfaces settle together.** The listing and the sidebar tree load independently, and a
 *    tree still offering a folder the listing has dropped is exactly what a component spec, holding
 *    one of the two, cannot see.
 * 2. **The run survives a reload.** jsdom has no page to reload; the in-flight set being
 *    re-established from the server is only provable in a real browser against a real queue.
 *
 * The ceiling (#37062) is advertised by an intercepted configuration, and the submissions those
 * tests make are answered without reaching the server, so nothing is deleted by them.
 */
// A delete is asynchronous end to end: the request, then a queued job, then the completion signal
// that refreshes both surfaces. A measured 20,000-file folder took about eight minutes, so the
// fixtures here stay deliberately small and the budget still has to be generous.
test.describe.configure({ timeout: 300000 });

/** Where Content Drive reads the deletes still running when it loads. */
const ACTIVE_DELETES_URL = '/api/v1/jobs/folderBulkDelete/active';

/** Where Content Drive loads its listing. */
const DRIVE_SEARCH_URL = '/api/v1/drive/search';

/** The title of every listing row marked as busy. */
const BUSY_ROW_TITLE =
    '[data-testid="item-row"]:has([data-testid="row-busy"]) [data-testid="item-title-text"]';

/**
 * Seeds a container folder holding the given subfolders, opens it, and removes it however the
 * test ends.
 *
 * Everything lives inside the container, so deleting it is the whole cleanup, including for a test
 * whose delete did not finish. Per test rather than in an `afterEach`, because this file runs fully
 * parallel.
 */
async function inSeededContainer(
    {
        adminPage,
        apiHelpers,
        name,
        children,
        beforeOpen
    }: {
        adminPage: Page;
        apiHelpers: ContentDriveApiHelpers;
        name: string;
        children: string[];
        /** Runs before the drive loads, which is when a route has to be in place. */
        beforeOpen?: () => Promise<void>;
    },
    body: (drive: ContentDrivePage, preview: FolderRunPreview) => Promise<void>
): Promise<void> {
    const site = await apiHelpers.getDefaultSite();
    const container = `/${name}`;

    await apiHelpers.createFolders(site.hostname, [
        container,
        ...children.map((child) => `${container}/${child}`)
    ]);

    try {
        const drive = new ContentDrivePage(adminPage);
        await beforeOpen?.();
        await drive.goTo();
        await drive.openFolder(name);
        await body(drive, new FolderRunPreview(adminPage));
    } finally {
        await apiHelpers.deleteFolders(site.hostname, [container]);
    }
}

test.describe('Content Drive bulk folder delete', () => {
    test('deletes every selected folder and reports the server’s counts @critical', ({
        adminPage,
        apiHelpers,
        testSuffix
    }) =>
        inSeededContainer(
            {
                adminPage,
                apiHelpers,
                name: `cd-del-${testSuffix}`,
                children: ['alpha', 'beta']
            },
            async (drive, preview) => {
                await preview.open('DELETE_FOLDER', ['alpha', 'beta']);

                // The confirmation names the count and says the contents go too.
                await expect(adminPage.getByTestId('delete-warning')).toContainText(
                    'Deleting 2 folder(s) also permanently deletes everything inside them'
                );

                await preview.execute();

                await drive.expectOutcomeContaining('ran on 2 item');
                await preview.expectGone('alpha');
                await preview.expectGone('beta');
            }
        ));

    test('keeps a folder marked as in-flight across a reload @critical', ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const name = `cd-del-reload-${testSuffix}`;

        return inSeededContainer(
            { adminPage, apiHelpers, name, children: ['big'] },
            async (_drive, preview) => {
                const site = await apiHelpers.getDefaultSite();

                // Re-established from the queue's active listing, not remembered by the page. The
                // run must still be going when the reloaded page reads that listing, and a delete
                // this size ends within a second or two, often before the page is back. So the read
                // is held until the folder is listed, the delete queued, and only then does the read
                // reach the server. That narrows the race rather than closing it: the queue could
                // still pick up and finish the run within the read's own round trip.
                const listed = adminPage.waitForResponse(
                    (response) => response.url().includes(DRIVE_SEARCH_URL) && response.ok()
                );
                await adminPage.route(
                    (url) => url.pathname === ACTIVE_DELETES_URL,
                    async (route) => {
                        await listed;
                        await apiHelpers.submitFolderDelete(site.hostname, [`/${name}/big`]);
                        await route.continue();
                    },
                    { times: 1 }
                );

                // The marker lasts only until the run ends, which can be too brief for an
                // assertion's retries to catch, so it is recorded from the moment the page loads.
                const busyRows = await recordFromNextLoad(adminPage, BUSY_ROW_TITLE);

                await adminPage.reload();
                await expectSeen(busyRows, 'big');

                await preview.expectGone('big');
            }
        );
    });

    test('runs only as many folders as one delete may carry', ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        let submitted: string[][] = [];

        return inSeededContainer(
            {
                adminPage,
                apiHelpers,
                name: `cd-del-cap-${testSuffix}`,
                children: ['alpha', 'beta'],
                // The ceiling is a server setting, so the configuration advertises one of 1. The
                // submission is answered as accepted without running: nothing is deleted.
                beforeOpen: async () => {
                    await advertiseFolderCeilings(adminPage, { delete: 1 });
                    submitted = await acceptSubmissionsWithoutRunning(
                        adminPage,
                        FOLDER_RUN_URL.delete
                    );
                }
            },
            async (_drive, preview) => {
                await preview.open('DELETE_FOLDER', ['alpha', 'beta']);

                // The preview lists only what this run carries, and still lets it run.
                await preview.expectListedCount(1);

                await preview.execute();

                await expect.poll(() => submitted.length).toBe(1);
                expect(submitted[0]).toHaveLength(1);
            }
        );
    });

    test('names the limit when the server refuses a delete as too large', ({
        adminPage,
        apiHelpers,
        testSuffix
    }) =>
        inSeededContainer(
            {
                adminPage,
                apiHelpers,
                name: `cd-del-limit-${testSuffix}`,
                children: ['alpha', 'beta'],
                // Under the advertised ceiling, so the client lets it through, and the server
                // refuses it anyway: its limit changed, or the client could not know it.
                beforeOpen: async () => {
                    await advertiseFolderCeilings(adminPage, { delete: 5 });
                    await refuseFolderRunsWith(
                        adminPage,
                        FOLDER_RUN_URL.delete,
                        400,
                        'OVER_MAX_PATHS'
                    );
                }
            },
            async (drive, preview) => {
                await preview.open('DELETE_FOLDER', ['alpha', 'beta']);
                await preview.execute();

                await drive.expectToastContaining('You can delete up to 5 folders at a time');
            }
        ));
});
