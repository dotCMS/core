import { type Page } from '@playwright/test';
import { ContentDrivePage } from '@pages';

import { type ContentDriveApiHelpers, test } from '../../fixtures/content-drive.fixture';

/**
 * Journey: what a bulk upload tells the author, including when it goes wrong (#37166).
 *
 * The happy path lives in `content-drive-bulk-upload.spec.ts`. This file covers the reporting, and
 * reporting is mostly failure: a batch that half-worked is the case an author has to act on, and
 * the case a unit test can only assert in terms of the copy it asked for, never in terms of what a
 * real server actually refused and why.
 *
 * ## How each refusal is triggered
 *
 * The set divides into refusals of the *submission*, where nothing is queued and the answer is
 * immediate, and failures of individual *files*, which come back in the outcome after the run.
 *
 * | What | Trigger | Automated here |
 * | --- | --- | --- |
 * | Too many files (`400`) | more than `CONTENT_BULK_UPLOAD_MAX_FILES` parts, default **100** | yes, by choosing 101 |
 * | Too much data (`413`) | over `CONTENT_BULK_UPLOAD_MAX_TOTAL_BYTES`, default **1 GiB** | no: set the key low (e.g. `1024`) and upload anything |
 * | `NAME_COLLISION` | a name already in the folder, case-insensitively | yes |
 * | duplicate resubmission | the *same* batch sent again, so every file collides | yes |
 * | `FOLDER_FILTER_MISMATCH` | the folder's own `fileMasks` glob, e.g. `*.jpg`, then upload a `.png` | yes |
 * | `DISALLOWED_FILE_TYPE` | the content type's binary field `allowedFileTypes`, e.g. `image/*`, then upload a `.txt` | no: it edits a shared content type, so it is a manual check |
 * | `OVER_SIZE_LIMIT` | the binary field's `maxFileLength`, or `CONTENT_BULK_UPLOAD_FALLBACK_MAX_FILE_BYTES`, default **200 MiB** | no: same reason, plus the file size |
 * | `PERMISSION_DENIED` | a user with read-only rights on the target folder | no: needs a second login |
 * | `STAGED_CONTENT_UNAVAILABLE` | the staged upload is gone before the job reads it | no: not reachable by hand |
 * | `UNCLASSIFIED` | anything the server did not anticipate | no, by definition |
 *
 * The ones marked no are not gaps in the client: each has unit coverage for the copy it renders,
 * and what cannot be proven here is that the *server* returns that reason, which is the backend's
 * own suite to run.
 */
/**
 * Seeds one folder, opens it, and deletes it however the test ends.
 *
 * Per test rather than in an `afterEach` over shared state: this file runs fully parallel, so a
 * describe-level list of what to clean up is a race. Teardown matters more here than in most
 * suites — these tests upload real files, and a leaked folder full of them eventually pushes a
 * later run's folder off a tree level that pages at 40.
 */
async function inSeededFolder(
    {
        adminPage,
        apiHelpers,
        name,
        fileMasks
    }: {
        adminPage: Page;
        apiHelpers: ContentDriveApiHelpers;
        name: string;
        fileMasks?: string[];
    },
    body: (drive: ContentDrivePage) => Promise<void>
): Promise<void> {
    const site = await apiHelpers.getDefaultSite();
    const path = `/${name}`;

    if (fileMasks) {
        await apiHelpers.createFilteredFolder(site.hostname, path, fileMasks);
    } else {
        await apiHelpers.createFolders(site.hostname, [path]);
    }

    try {
        const drive = new ContentDrivePage(adminPage);
        await drive.goTo();
        await drive.openFolder(name);
        await body(drive);
    } finally {
        await apiHelpers.deleteFolders(site.hostname, [path]);
    }
}

test.describe('Content Drive bulk upload outcomes', () => {
    test('tells the author the batch is theirs to leave, and keeps reporting it @critical', ({
        adminPage,
        apiHelpers,
        testSuffix
    }) =>
        inSeededFolder({ adminPage, apiHelpers, name: `cd-bg-${testSuffix}` }, async (drive) => {
            await drive.chooseFilesForUpload([`bg-a-${testSuffix}.png`, `bg-b-${testSuffix}.png`]);

            // The moment the guarantee changes: before the handle, leaving loses the batch;
            // after it, leaving costs nothing, and the interface has to say so because nothing
            // else can.
            await drive.expectHandedToBackground();
        }));

    test('names the file that collided, and still uploads the rest', ({
        adminPage,
        apiHelpers,
        testSuffix
    }) =>
        inSeededFolder(
            { adminPage, apiHelpers, name: `cd-collide-${testSuffix}` },
            async (drive) => {
                const taken = `taken-${testSuffix}.png`;
                await drive.chooseFilesForUpload([taken]);
                await drive.expectListContainsTitle(taken);

                // One name taken, one free. The outcome has to name which, because "1 of 2 failed"
                // leaves the author to work out the difference themselves.
                await drive.chooseFilesForUpload([taken, `free-${testSuffix}.png`]);

                await drive.expectOutcomeContaining(taken);
            }
        ));

    test('reports a whole batch sent twice as already uploaded, not as a total failure', ({
        adminPage,
        apiHelpers,
        testSuffix
    }) =>
        inSeededFolder({ adminPage, apiHelpers, name: `cd-retry-${testSuffix}` }, async (drive) => {
            const batch = [`retry-a-${testSuffix}.png`, `retry-b-${testSuffix}.png`];
            await drive.chooseFilesForUpload(batch);
            await drive.expectListContainsTitle(batch[0]);

            // By the counts this is a total failure: every file collides. Reporting it that way
            // sends the author to delete and re-upload files that are already correctly there,
            // which the spec calls worse than offering no retry at all.
            await drive.chooseFilesForUpload(batch);

            await drive.expectOutcomeContaining('already uploaded');
        }));

    test("says the folder's own rule refused the file, not that the type is wrong", ({
        adminPage,
        apiHelpers,
        testSuffix
    }) =>
        inSeededFolder(
            { adminPage, apiHelpers, name: `cd-filter-${testSuffix}`, fileMasks: ['*.jpg'] },
            async (drive) => {
                await drive.chooseFilesForUpload([`refused-${testSuffix}.png`]);

                // The distinction this asserts: the same PNG is fine one folder over, so the fix is
                // to rename or move it. Copy about file types would send the author to change the
                // wrong thing.
                await drive.expectOutcomeContaining('file names');
            }
        ));

    test('refuses a batch with more files than one upload allows', ({
        adminPage,
        apiHelpers,
        testSuffix
    }) =>
        inSeededFolder({ adminPage, apiHelpers, name: `cd-many-${testSuffix}` }, async (drive) => {
            // One over the default ceiling. Nothing is queued, so the author must be told which
            // ceiling they crossed: "fewer files" and "smaller files" are different
            // instructions.
            await drive.chooseGeneratedFilesForUpload(101, `many-${testSuffix}`);

            await drive.expectToastContaining('more files than');
        }));

    test('reaches the author who left the portlet before it finished', ({
        adminPage,
        apiHelpers,
        testSuffix
    }) =>
        inSeededFolder({ adminPage, apiHelpers, name: `cd-leave-${testSuffix}` }, async (drive) => {
            await drive.chooseFilesForUpload([`leave-${testSuffix}.png`]);
            await drive.expectHandedToBackground();

            // Away from the portlet entirely, which is the case the story is named for. The
            // toast cannot follow: it belongs to a component that is destroyed on navigation.
            // What follows is the durable notification, and this is the only test that can tell
            // the difference.
            await adminPage.goto('/dotAdmin/#/pages');
            await adminPage.waitForLoadState('domcontentloaded');

            await drive.expectNotificationContaining('Upload Finished');
        }));
});
