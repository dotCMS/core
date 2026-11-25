import { expect } from '@playwright/test';

import { ContentDriveBasics } from './helpers/content-drive-basics';
import { type DriveSeed, removeSeed, seedDrive } from './helpers/content-drive-seed';

import { test } from '../../fixtures/content-drive.fixture';
import { SidePanel } from '../edit-content/side-panel/helpers/side-panel';

/**
 * Journey: create content from Content Drive's New menu. The type picked in the picker opens in
 * the side panel, files into the folder being browsed, and the saved content is listed there.
 */
test.describe('Content Drive create content', () => {
    let seed: DriveSeed | null = null;

    test.afterEach(async ({ request }) => {
        await removeSeed(request, seed);
        seed = null;
    });

    test('creates content from the New menu in the folder being browsed @critical', async ({
        adminPage,
        apiHelpers,
        request,
        testSuffix
    }) => {
        const site = await apiHelpers.getDefaultSite();
        seed = await seedDrive(request, site.hostname, 'e2e-create', testSuffix);
        const title = `Created from New ${testSuffix}`;

        const basics = new ContentDriveBasics(adminPage);
        const sidePanel = new SidePanel(adminPage);

        await basics.drive.goTo();
        await basics.drive.openFolder(seed.folderName);

        await basics.chooseNew('All Content Types');
        await basics.pickContentType(seed.contentType.name);

        await sidePanel.expectOpen();
        await sidePanel.form.fillTextField(title);
        await sidePanel.form.save();
        await sidePanel.close();
        await sidePanel.expectClosed();

        await basics.drive.expectListContainsTitle(title);
        // Filed in this folder, not under System Host: the seed type lives on System Host, and the
        // listing also shows System Host content, marked as shared.
        await expect(basics.drive.row(title).getByTestId('shared-asset-hint')).toHaveCount(0);
        await expect(basics.status(title)).toHaveText(/Draft|Published/);
    });
});
