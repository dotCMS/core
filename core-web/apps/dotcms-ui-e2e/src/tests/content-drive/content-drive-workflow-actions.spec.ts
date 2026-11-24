import { expect } from '@playwright/test';

import { ContentDriveBasics } from './helpers/content-drive-basics';
import { type DriveSeed, removeSeed, seedContent, seedDrive } from './helpers/content-drive-seed';

import { test } from '../../fixtures/content-drive.fixture';

/**
 * Journey: the content lifecycle from Content Drive's Workflow Center. Unpublish and Publish change
 * the row's status; Archive takes it out of the default listing, the Archived status brings it
 * back, and Delete removes it for good.
 *
 * Which actions the Workflow Center offers for a selection is unit tested; these fire them against
 * the server and check what the listing shows afterwards.
 */
test.describe('Content Drive workflow actions', () => {
    let seed: DriveSeed | null = null;

    test.afterEach(async ({ request }) => {
        await removeSeed(request, seed);
        seed = null;
    });

    test('unpublishes and publishes content @critical', async ({
        adminPage,
        apiHelpers,
        request,
        testSuffix
    }) => {
        const site = await apiHelpers.getDefaultSite();
        seed = await seedDrive(request, site.hostname, 'e2e-publish', testSuffix);
        const title = `Publish me ${testSuffix}`;
        await seedContent(request, seed, title);

        const basics = new ContentDriveBasics(adminPage);
        await basics.drive.goTo();
        await basics.drive.openFolder(seed.folderName);
        await expect(basics.status(title)).toContainText('Published', { timeout: 20000 });

        await basics.selectRow(title);
        await basics.runWorkflowAction('Unpublish');
        await expect(basics.status(title)).toHaveText('Draft', { timeout: 20000 });

        await basics.selectRow(title);
        await basics.runWorkflowAction('Publish');
        await expect(basics.status(title)).toContainText('Published', { timeout: 20000 });
    });

    test('archives content and deletes it from the archive @critical', async ({
        adminPage,
        apiHelpers,
        request,
        testSuffix
    }) => {
        const site = await apiHelpers.getDefaultSite();
        seed = await seedDrive(request, site.hostname, 'e2e-archive', testSuffix);
        const title = `Archive me ${testSuffix}`;
        await seedContent(request, seed, title);

        const basics = new ContentDriveBasics(adminPage);
        await basics.drive.goTo();
        await basics.drive.openFolder(seed.folderName);
        await basics.drive.expectListContainsTitle(title);

        // System Workflow archives only unpublished content.
        await basics.selectRow(title);
        await basics.runWorkflowAction('Unpublish');
        await expect(basics.status(title)).toHaveText('Draft', { timeout: 20000 });

        await basics.selectRow(title);
        await basics.runWorkflowAction('Archive');
        // The folder holds only this item, so the listing ends empty. The empty state never shows
        // while a listing loads, so this cannot pass on the skeleton.
        await expect(basics.emptyState).toBeVisible({ timeout: 20000 });

        await basics.chooseStatus('Archived');
        await expect(basics.status(title)).toHaveText('Archived', { timeout: 20000 });

        await basics.selectRow(title);
        await basics.runWorkflowAction('Delete');
        await expect(basics.emptyState).toBeVisible({ timeout: 20000 });
    });
});
