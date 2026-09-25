import { ContentDrivePage } from '@pages';

import { test } from '../../fixtures/content-drive.fixture';

/**
 * Journey: Content Drive bulk folder delete (#37063)
 *
 * **Skipped until the server half exists.** The endpoint, the queue and the folder-delete
 * announcements are specified and merged but not implemented — see
 * `specs/37063-bulk-folder-delete-backend/`. The frontend is built and unit-tested against that
 * contract, so this file is authored now, reviewed now, and enabled in the PR that stacks onto
 * branch `37063-content-drive-bulk-folder-delete-backend` once it carries an implementation.
 *
 * Written rather than deferred because the two things it covers are the two a unit test cannot
 * reach, and both are the point of the feature:
 *
 * 1. **The run survives a reload.** jsdom has no page to reload; the in-flight set being
 *    re-established from the server is only provable in a real browser against a real queue.
 * 2. **Both surfaces settle together.** The listing and the sidebar tree load independently, and a
 *    tree still offering a folder the listing has dropped is exactly what a component spec, holding
 *    one of the two, cannot see.
 *
 * Remove the `.skip` and delete this paragraph when the backend lands (tasks.md T084).
 */
// A delete is asynchronous end to end: the request, then a queued job, then the completion signal
// that refreshes both surfaces. A measured 20,000-file folder took about eight minutes, so the
// fixtures here stay deliberately small and the budget still has to be generous.
test.describe.configure({ timeout: 300000 });

// The suite lints skipped tests as errors, and rightly — a skip with no expiry is how a test rots
// quietly. This one has both a reason and a removal task (tasks.md T084), so it is exempted here
// rather than by weakening the rule, and the disable comes out with the `.skip` in the same commit.
// eslint-disable-next-line playwright/no-skipped-test
test.describe.skip('Content Drive bulk folder delete', () => {
    test('deletes every selected folder and reports the server’s counts @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const site = await apiHelpers.getDefaultSite();
        const folders = [`/cd-del-a-${testSuffix}`, `/cd-del-b-${testSuffix}`];
        await apiHelpers.createFolders(site.hostname, folders);

        const drive = new ContentDrivePage(adminPage);
        await drive.goTo();

        // Select both folders, delete, confirm.
        // Expected: the confirmation names the count and says the contents go too; the rows are
        // marked; the outcome reports 2 deleted; neither folder remains in the listing OR the tree.
    });

    test('keeps a folder marked as in-flight across a reload @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const site = await apiHelpers.getDefaultSite();
        const folder = `/cd-del-big-${testSuffix}`;
        await apiHelpers.createFolders(site.hostname, [folder]);

        const drive = new ContentDrivePage(adminPage);
        await drive.goTo();

        // Start the delete on a folder large enough to still be running, then reload.
        // Expected: the row is still marked and still inert, and the tree node with it — the state
        // re-established from the queue rather than remembered by the page that started it.
    });

    test('leaves a folder usable when its delete failed @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const site = await apiHelpers.getDefaultSite();
        const folder = `/cd-del-locked-${testSuffix}`;
        await apiHelpers.createFolders(site.hostname, [folder]);

        const drive = new ContentDrivePage(adminPage);
        await drive.goTo();

        // Provoke a per-folder failure (locked content inside), then wait for the run to settle.
        // Expected: the folder is named in the outcome with a reason, the marking clears, and the
        // folder is usable again — the case the "active" listing's failed runs would otherwise
        // leave marked forever (contract CR-10).
    });
});
