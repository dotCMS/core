import { ContentDrivePage } from '@pages';

import { test } from '../../fixtures/content-drive.fixture';

/**
 * Journey: Content Drive bulk file upload (#37166)
 *
 * These exist because jsdom cannot operate a file chooser at all, so a unit test can assert that
 * the component would submit several files but never that the browser lets the author choose them.
 * The `multiple` attribute and a multi-file drop are only provable here.
 *
 * The real drag from the desktop into the browser stays a manual check: Playwright can synthesise a
 * `DataTransfer` inside the page, which exercises the same handler, but it cannot drive the OS.
 */
// A batch is asynchronous end to end: the request, then a queued job, then the completion signal
// that refreshes the grid. Twenty seconds of that is a coin toss on a loaded runner, and the
// default budget cannot hold a wait of its own size.
test.describe.configure({ timeout: 180000 });

test.describe('Content Drive bulk upload', () => {
    test('uploads every file chosen through the file chooser @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const site = await apiHelpers.getDefaultSite();
        const folderName = `cd-bulk-${testSuffix}`;
        await apiHelpers.createFolders(site.hostname, [`/${folderName}`]);

        const drive = new ContentDrivePage(adminPage);
        await drive.goTo();
        await drive.openFolder(folderName);

        const names = [`a-${testSuffix}.png`, `b-${testSuffix}.png`, `c-${testSuffix}.png`];
        await drive.chooseFilesForUpload(names);

        // Every one of them, which is the defect: the input carried no `multiple`, so only the
        // first ever reached the server.
        for (const name of names) {
            await drive.expectUploadedTitle(folderName, name);
        }
    });

    test('uploads every file dropped onto a folder @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const site = await apiHelpers.getDefaultSite();
        const folderName = `cd-drop-${testSuffix}`;
        await apiHelpers.createFolders(site.hostname, [`/${folderName}`]);

        const drive = new ContentDrivePage(adminPage);
        await drive.goTo();
        await drive.openFolder(folderName);

        const names = [`d-${testSuffix}.png`, `e-${testSuffix}.png`];
        await drive.dropFilesOnList(names);

        // The drop path already delivered a whole FileList, so this guards the half of the defect
        // that was never about the input attribute.
        for (const name of names) {
            await drive.expectUploadedTitle(folderName, name);
        }
    });

    test('does not warn that only one file will be uploaded', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const site = await apiHelpers.getDefaultSite();
        const folderName = `cd-nowarn-${testSuffix}`;
        await apiHelpers.createFolders(site.hostname, [`/${folderName}`]);

        const drive = new ContentDrivePage(adminPage);
        await drive.goTo();
        await drive.openFolder(folderName);

        await drive.chooseFilesForUpload([`f-${testSuffix}.png`, `g-${testSuffix}.png`]);

        await drive.expectNoSingleFileWarning();
    });
});
