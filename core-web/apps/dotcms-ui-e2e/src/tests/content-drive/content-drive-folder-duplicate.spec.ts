import { ContentDrivePage } from '@pages';
import { type Page } from '@playwright/test';

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
 * Covered elsewhere, and why:
 *
 * - The add-children gate and a parent the author cannot add to need a second, limited login. The
 *   refusal itself is proven by `FolderBulkDuplicateProcessorIT`, and the gate's rendering by the
 *   portlet's unit tests.
 * - A cancelled run needs a duplicate slow enough to stop, which no fixture here is.
 *   `FolderBulkDuplicateCancellationIT` covers it.
 * - Each refusal's wording is unit-tested against the copy it renders. What cannot be shown here is
 *   the server returning that reason, which the backend suite owns.
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
        children
    }: {
        adminPage: Page;
        apiHelpers: ContentDriveApiHelpers;
        name: string;
        children: string[];
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
        await drive.goTo();
        await drive.openFolder(name);
        await body(drive, new FolderDuplicate(adminPage));
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
});
