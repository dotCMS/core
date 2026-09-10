import { ContentDrivePage } from '@pages';
import { type Page } from '@playwright/test';

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

// Same reason as the happy-path spec: every assertion here waits on a queued job and a pushed
// signal, and one of these tests died as a *test* timeout because its inner wait was the size of
// the whole budget.
test.describe.configure({ timeout: 180000 });

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
                // FILEASSET, deliberately, and this is the only test in the file that pins a base
                // type. A dotAsset has no name to collide on: its title is derived from the binary, so
                // the server has no equivalent lookup and a repeated name legitimately creates a second
                // asset. Written against DOTASSET this test asserted a refusal the product never makes.
                const taken = `taken-${testSuffix}.png`;
                await drive.chooseFilesForUpload([taken], 'FILEASSET');
                await drive.expectUploadedTitle(`cd-collide-${testSuffix}`, taken);

                // One name taken, one free. The outcome has to name which, because "1 of 2 failed"
                // leaves the author to work out the difference themselves.
                await drive.chooseFilesForUpload([taken, `free-${testSuffix}.png`], 'FILEASSET');

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

            // FILEASSET, for the same reason the collision test above pins it: the whole premise
            // here is that every file in the resubmitted batch collides, and only a fileAsset has a
            // name to collide on. Sent as dotAssets these uploaded a second time and the folder
            // ended up holding two of everything, which is the product behaving correctly and the
            // test asserting a refusal that was never on offer.
            await drive.chooseFilesForUpload(batch, 'FILEASSET');
            await drive.expectUploadedTitle(`cd-retry-${testSuffix}`, batch[0]);

            // By the counts this is a total failure: every file collides. Reporting it that way
            // sends the author to delete and re-upload files that are already correctly there,
            // which the spec calls worse than offering no retry at all.
            await drive.chooseFilesForUpload(batch, 'FILEASSET');

            await drive.expectOutcomeContaining('already uploaded');

            // And the folder has to agree with the message. "Nothing was duplicated" is a claim
            // about what is in the folder, so one row per name is the part worth asserting.
            await drive.expectTitleCount(batch[0], 1);
            await drive.expectTitleCount(batch[1], 1);
        }));

    test('tells a resubmitted dotAsset batch that it now has two copies', ({
        adminPage,
        apiHelpers,
        testSuffix
    }) =>
        inSeededFolder({ adminPage, apiHelpers, name: `cd-dup-${testSuffix}` }, async (drive) => {
            // The FILEASSET test above is the mirror of this one. FR-040b: a dotAsset's asset_name
            // is generated per contentlet, so the unique index that refuses a file asset's second
            // copy can never contend, and the batch genuinely runs again. The team accepted the
            // duplicates; what the client must not do is call that "nothing was duplicated".
            const batch = [`dup-a-${testSuffix}.png`, `dup-b-${testSuffix}.png`];

            await drive.chooseFilesForUpload(batch, 'DOTASSET');
            await drive.expectUploadedTitle(`cd-dup-${testSuffix}`, batch[0]);

            await drive.chooseFilesForUpload(batch, 'DOTASSET');

            // Says a second copy exists, rather than claiming nothing happened.
            await drive.expectOutcomeContaining('uploaded again');

            // And the folder agrees: two of each, which is the accepted cost rather than a defect.
            await drive.expectTitleCount(batch[0], 2);
            await drive.expectTitleCount(batch[1], 2);
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
            // One over the default ceiling, and refused without being sent. The client reads the
            // ceiling off the configuration the server advertises, so the author is told in the
            // chooser rather than after uploading 101 files to be turned away.
            await drive.expectNothingUploadedWhile(async () => {
                await drive.chooseGeneratedFilesForUpload(101, `many-${testSuffix}`);

                // Both numbers, in one sentence. "Fewer" makes an author with 140 files retry with
                // 120, then 110, uploading the whole batch each time to be refused again — and
                // "fewer files" and "smaller files" are different instructions, so the ceiling
                // that refused them has to be the one named.
                await drive.expectToastContaining('101 files, and one upload allows 100');
            });
        }));

    test('reaches the author who left the portlet before it finished', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        // Longer than the default: this one waits for the server to finish the batch, not just to
        // accept it, and the default budget cannot hold an inner wait of its own size.
        test.setTimeout(150000);

        // Durable and per user, so without this the assertion passes against the last run's
        // notifications. A bulk upload's notification carries counts and no file name, so nothing
        // in the text could distinguish them.
        await apiHelpers.clearNotifications();

        await inSeededFolder(
            { adminPage, apiHelpers, name: `cd-leave-${testSuffix}` },
            async (drive) => {
                // Acceptance, not the advisory: this test is about the outcome following the author
                // out of the portlet, and the message left behind is another test's subject.
                await drive.chooseFilesAndAwaitAcceptance([`leave-${testSuffix}.png`]);

                // Away from the portlet entirely, which is the case the story is named for. The
                // toast cannot follow: it belongs to a component that is destroyed on navigation.
                // What follows is the durable notification, and this is the only test that can tell
                // the difference.
                await adminPage.goto('/dotAdmin/#/pages');
                await adminPage.waitForLoadState('domcontentloaded');

                // The message, which is what the panel renders. Counts only: the notification names no
                // files, which is the difference between it and the toast inside the portlet.
                await drive.expectNotificationContaining('file(s) uploaded');
            }
        );
    });
});
