import { ContentDrivePage } from '@pages';
import { type Page } from '@playwright/test';
import { Portlet } from '@utils/portlets';

import { FolderDuplicate } from './helpers/content-drive-duplicate';

import { type ContentDriveApiHelpers, test } from '../../fixtures/content-drive.fixture';

/**
 * Journey: duplicating folders in Content Drive (#37062).
 *
 * What only a real browser against a real queue can prove: the run is submitted, the job finishes,
 * the pushed completion reaches the page, and both surfaces (the listing and the sidebar tree)
 * reload to show the duplicate. The durable notification is checked too, since it is what an
 * author who left the portlet finds.
 *
 * Two things are answered by an intercepted response rather than a real one, because neither can
 * be provoked on demand: each submission refusal (a licence, a configured ceiling), and a folder the
 * author may not add to (a limited user). The rest of each flow is real.
 *
 * Covered elsewhere, and why:
 *
 * - The server returning each refusal, and refusing a parent the author cannot add to, is the
 *   backend suite's (`FolderBulkDuplicateResourceIT`, `FolderBulkDuplicateProcessorIT`).
 * - A cancelled run needs a duplicate slow enough to stop, which no fixture here is.
 *   `FolderBulkDuplicateCancellationIT` covers it.
 * - Another author's open Content Drive is not expected to reload. The portlet reloads on the
 *   completion pushed to the submitter; the `COPY_FOLDER` event the copy also raises is not one it
 *   listens to.
 */
test.describe.configure({ timeout: 300000 });

/**
 * Seeds a container folder holding the given subfolders, opens it, and removes it however the
 * test ends.
 *
 * Everything lives inside the container, duplicates included, since a duplicate lands beside its
 * original. Deleting the container is therefore the whole cleanup. Per test rather than in an
 * `afterEach`, because this file runs fully parallel.
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
        beforeOpen?: (duplicate: FolderDuplicate) => Promise<void>;
    },
    body: (drive: ContentDrivePage, duplicate: FolderDuplicate) => Promise<void>
): Promise<void> {
    const site = await apiHelpers.getDefaultSite();
    const container = `/${name}`;

    await apiHelpers.createFolders(site.hostname, [
        container,
        ...children.map((child) => `${container}/${child}`)
    ]);

    try {
        await apiHelpers.clearNotifications();

        const drive = new ContentDrivePage(adminPage);
        const duplicate = new FolderDuplicate(adminPage);
        await beforeOpen?.(duplicate);
        await drive.goTo();
        await drive.openFolder(name);
        await body(drive, duplicate);
    } finally {
        await apiHelpers.deleteFolders(site.hostname, [container]);
    }
}

test.describe('Content Drive folder duplicate', () => {
    test('duplicates a folder from its right-click menu, and shows the copy @critical', ({
        adminPage,
        apiHelpers,
        testSuffix
    }) =>
        inSeededContainer(
            { adminPage, apiHelpers, name: `cd-dup-${testSuffix}`, children: ['source'] },
            async (drive, duplicate) => {
                await duplicate.fromContextMenu('source');

                // Reported while it runs, and cleared once it has finished.
                await drive.expectStatusToastContaining('in the background');
                await duplicate.expectDuplicateShown('source_copy');
                await drive.expectStatusToastGone();
                await drive.expectOutcomeContaining('ran on 1 item');
                await drive.expectNotificationContaining('1 folder(s) duplicated.');
            }
        ));

    test('duplicates several folders from the Action Center @critical', ({
        adminPage,
        apiHelpers,
        testSuffix
    }) =>
        inSeededContainer(
            {
                adminPage,
                apiHelpers,
                name: `cd-dup-many-${testSuffix}`,
                children: ['alpha', 'beta']
            },
            async (drive, duplicate) => {
                await duplicate.fromActionCenter(['alpha', 'beta']);

                await duplicate.expectDuplicateShown('alpha_copy');
                await duplicate.expectDuplicateShown('beta_copy');
                await drive.expectOutcomeContaining('ran on 2 item');
                await drive.expectNotificationContaining('2 folder(s) duplicated.');
            }
        ));

    test('carries what the folder holds into the duplicate', ({
        adminPage,
        apiHelpers,
        testSuffix
    }) =>
        inSeededContainer(
            {
                adminPage,
                apiHelpers,
                name: `cd-dup-deep-${testSuffix}`,
                children: ['source', 'source/inner']
            },
            async (drive, duplicate) => {
                await duplicate.fromContextMenu('source');
                await duplicate.expectDuplicateShown('source_copy');

                await drive.openFolder('source_copy');

                await drive.expectListContainsTitle('inner');
            }
        ));

    test('finds the outcome in the bell after leaving the drive', ({
        adminPage,
        apiHelpers,
        testSuffix
    }) =>
        inSeededContainer(
            { adminPage, apiHelpers, name: `cd-dup-away-${testSuffix}`, children: ['source'] },
            async (drive, duplicate) => {
                // Left straight away, before the run can finish: the toast belongs to a page the
                // author is no longer on, so the bell is what tells them.
                await duplicate.fromContextMenu('source');
                await adminPage.goto(Portlet.Content);

                await drive.expectNotificationContaining('1 folder(s) duplicated.');

                // Back with a real page load, as an author returning later would, which also
                // closes the notifications panel left open over the page.
                await adminPage.goto('about:blank');
                await drive.goTo();
                await drive.openFolder(`cd-dup-away-${testSuffix}`);
                // The listing, not the tree: opening a folder there selects it without expanding
                // it, so its children are not in the tree yet. The tree catching up as a run ends
                // is what the other tests check.
                await drive.expectListContainsTitle('source_copy');
            }
        ));

    test('does not offer Duplicate where the author cannot add folders', ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const name = `cd-dup-gate-${testSuffix}`;

        return inSeededContainer(
            {
                adminPage,
                apiHelpers,
                name,
                children: ['source'],
                beforeOpen: (duplicate) => duplicate.withoutAddChildrenOn(name)
            },
            // Every duplicate would land in the folder being browsed, where the author may not add.
            (_drive, duplicate) => duplicate.expectNotOffered('source')
        );
    });

    for (const refusal of [
        {
            errorCode: 'OVER_MAX_PATHS',
            status: 400,
            sentence: 'You selected more folders than one duplicate allows'
        },
        {
            errorCode: 'NOT_ENTITLED',
            status: 403,
            sentence: 'You are not allowed to duplicate folders in bulk'
        },
        { errorCode: 'EMPTY_SELECTION', status: 400, sentence: 'No folders were submitted' }
    ]) {
        test(`says in its own words why the server refused the duplicate: ${refusal.errorCode}`, ({
            adminPage,
            apiHelpers,
            testSuffix
        }) =>
            inSeededContainer(
                {
                    adminPage,
                    apiHelpers,
                    name: `cd-dup-refused-${testSuffix}`,
                    children: ['source'],
                    beforeOpen: (duplicate) =>
                        duplicate.refuseSubmissionsWith(refusal.status, refusal.errorCode)
                },
                async (drive, duplicate) => {
                    await duplicate.fromContextMenu('source');

                    await drive.expectToastContaining('The duplicate did not start');
                    await drive.expectToastContaining(refusal.sentence);
                    // Nothing is running, so nothing may still say it is.
                    await drive.expectStatusToastGone();
                }
            ));
    }
});
