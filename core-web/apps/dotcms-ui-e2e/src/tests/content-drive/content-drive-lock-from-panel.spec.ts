import { ContentDrivePage } from '@pages';
import { expect } from '@playwright/test';

import { type DriveSeed, removeSeed, seedContent, seedDrive } from './helpers/content-drive-seed';

import { test } from '../../fixtures/content-drive.fixture';
import { SidePanel } from '../edit-content/side-panel/helpers/side-panel';

/**
 * #38379: locking or unlocking content from the edit side panel updates its row's padlock in
 * Content Drive while the panel is still open. The panel used to report saves and deletes only,
 * so the row kept the old padlock until the next reload.
 *
 * Which listings reload on a lock, and that Content Drive patches the row instead, are unit
 * tested; this covers the chain from the panel's button to the row, which no unit test spans.
 */
test.describe('Content Drive lock from the side panel', () => {
    let seed: DriveSeed | null = null;

    test.afterEach(async ({ request }) => {
        await removeSeed(request, seed);
        seed = null;
    });

    test('updates the row padlock when content is locked and unlocked in the panel @critical', async ({
        adminPage,
        apiHelpers,
        request,
        testSuffix
    }) => {
        const site = await apiHelpers.getDefaultSite();
        seed = await seedDrive(request, site.hostname, 'e2e-lock', testSuffix);
        const title = `Lock from panel ${testSuffix}`;
        await seedContent(request, seed, title);

        const drive = new ContentDrivePage(adminPage);
        const sidePanel = new SidePanel(adminPage);

        await drive.goTo();
        await drive.openFolder(seed.folderName);

        const row = drive.row(title);
        await expect(row.getByTestId('lock-open-icon')).toBeVisible({ timeout: 20000 });

        await drive.openItem(title);
        await sidePanel.expectOpen();
        const lockButton = sidePanel.front.getByTestId('sidebar-lock-button');

        await lockButton.click();
        await expect(lockButton).toContainText('Unlock', { timeout: 15000 });
        await expect(row.getByTestId('lock-icon')).toBeVisible({ timeout: 15000 });

        await lockButton.click();
        await expect(lockButton).toContainText('Lock for Editing', { timeout: 15000 });
        await expect(row.getByTestId('lock-open-icon')).toBeVisible({ timeout: 15000 });
    });
});
