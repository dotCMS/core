import { ContentDrivePage } from '@pages';
import { expect } from '@playwright/test';
import { createContentlet } from '@requests/contentlets';
import { createFakeContentType, deleteContentType } from '@requests/contentType';
import { createFolder } from '@requests/folders';
import {
    createFakePayloadHostFolderField,
    createFakePayloadTextField
} from '@utils/dot-content-types.mock';

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
    let contentTypeId: string | null = null;
    let folderName: string;
    let siteName: string;

    test.afterEach(async ({ request, apiHelpers }) => {
        // Deleting the type deletes its contentlets.
        if (contentTypeId) {
            await deleteContentType(request, contentTypeId);
            contentTypeId = null;
        }

        await apiHelpers.deleteFolders(siteName, [`/${folderName}`]);
    });

    test('updates the row padlock when content is locked and unlocked in the panel @critical', async ({
        adminPage,
        apiHelpers,
        request,
        testSuffix
    }) => {
        const site = await apiHelpers.getDefaultSite();
        siteName = site.hostname;
        folderName = `e2e-lock-${testSuffix}`;
        const folderInode = await createFolder(request, siteName, `/${folderName}`);

        // A Host/Folder field, or the server ignores `hostFolder` and files the content under
        // System Host, outside the folder the test opens.
        const contentType = await createFakeContentType(request, {
            name: `E2E Lock ${testSuffix}`,
            fields: [
                createFakePayloadTextField({ name: 'Title', variable: 'title', sortOrder: 1 }),
                createFakePayloadHostFolderField({
                    name: 'Site Or Folder',
                    variable: 'siteOrFolder',
                    sortOrder: 2
                })
            ]
        });
        contentTypeId = contentType.id;

        const title = `Lock from panel ${testSuffix}`;
        await createContentlet(request, {
            contentType: contentType.variable,
            title,
            siteOrFolder: folderInode
        });

        const drive = new ContentDrivePage(adminPage);
        const sidePanel = new SidePanel(adminPage);

        await drive.goTo();
        await drive.openFolder(folderName);

        const row = drive.row(title);
        await expect(row.getByTestId('lock-open-icon')).toBeVisible({ timeout: 20000 });

        await drive.openItem(title);
        await sidePanel.expectOpen();
        const lockButton = sidePanel.front.getByTestId('sidebar-lock-button');

        await lockButton.click();
        await expect(row.getByTestId('lock-icon')).toBeVisible({ timeout: 15000 });

        await lockButton.click();
        await expect(row.getByTestId('lock-open-icon')).toBeVisible({ timeout: 15000 });
    });
});
