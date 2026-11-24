import { expect } from '@playwright/test';
import { createFolder } from '@requests/folders';

import { ContentDriveBasics } from './helpers/content-drive-basics';
import { type DriveSeed, removeSeed, seedContent, seedDrive } from './helpers/content-drive-seed';

import { test } from '../../fixtures/content-drive.fixture';

/**
 * Journey: organising Content Drive. A folder is created from the New menu and renamed through
 * Folder Settings, and content is moved into a folder by dragging its row onto the folder's. (A
 * drop moves content only: Content Drive has no folder move.)
 *
 * The folder dialog's validation and path preview are unit tested; these check that each change
 * reaches the server and the listing shows it.
 */
test.describe('Content Drive folders', () => {
    let seed: DriveSeed | null = null;

    test.afterEach(async ({ request }) => {
        await removeSeed(request, seed);
        seed = null;
    });

    test('creates a folder from the New menu and renames it @critical', async ({
        adminPage,
        apiHelpers,
        request,
        testSuffix
    }) => {
        const site = await apiHelpers.getDefaultSite();
        seed = await seedDrive(request, site.hostname, 'e2e-folders', testSuffix);
        const created = `created-${testSuffix}`;
        const renamed = `renamed-${testSuffix}`;

        const basics = new ContentDriveBasics(adminPage);
        await basics.drive.goTo();
        await basics.drive.openFolder(seed.folderName);

        await basics.chooseNew('Folder');
        await basics.folderNameField.fill(created);
        await basics.folderDialogSubmit.click();
        await basics.drive.expectListContainsTitle(created);

        await basics.rightClickAction(created, 'Folder Settings');
        await basics.folderNameField.fill(renamed);
        await basics.folderDialogSubmit.click();

        await basics.drive.expectListContainsTitle(renamed);
        await expect(basics.drive.row(created)).toHaveCount(0);
    });

    test('moves content into a folder by dragging its row onto the folder @critical', async ({
        adminPage,
        apiHelpers,
        request,
        testSuffix
    }) => {
        const site = await apiHelpers.getDefaultSite();
        seed = await seedDrive(request, site.hostname, 'e2e-move', testSuffix);
        const target = `target-${testSuffix}`;
        const title = `Move me ${testSuffix}`;
        await createFolder(request, site.hostname, `/${seed.folderName}/${target}`);
        await seedContent(request, seed, title);

        const basics = new ContentDriveBasics(adminPage);
        await basics.drive.goTo();
        await basics.drive.openFolder(seed.folderName);
        await basics.drive.expectListContainsTitle(title);

        await basics.drive
            .row(title)
            .getByTestId('item-title-text')
            .dragTo(basics.drive.row(target).getByTestId('item-title-text'));

        await expect(basics.drive.row(title)).toHaveCount(0, { timeout: 30000 });

        await basics.drive.row(target).getByTestId('item-title-text').click();
        await basics.drive.expectListContainsTitle(title);
    });
});
