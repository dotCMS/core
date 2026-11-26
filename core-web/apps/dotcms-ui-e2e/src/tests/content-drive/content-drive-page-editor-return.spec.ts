import { ContentDrivePage, UveEditorPage } from '@pages';
import { expect } from '@playwright/test';
import { createFolder } from '@requests/folders';

import { test } from '../../fixtures/content-drive.fixture';
import {
    createUvePage,
    removeUvePage,
    type UvePage
} from '../edit-content/side-panel/helpers/uve-page';

/**
 * #38379: coming back from the page editor through the Content Drive crumb restores the folder
 * and the tree state the author left. Content Drive writes both to the address with `Location.go`,
 * which the router never sees, so the crumb kept the address Content Drive was first opened on.
 *
 * The breadcrumb store's handling is unit tested against mocks of the router and `Location`; this
 * runs it against the real ones, whose timing the mocks only model.
 */
/** The query of a hash-routed admin URL, as a plain object. */
function contentDriveQuery(url: string): Record<string, string> {
    const query = url.split('#')[1]?.split('?')[1] ?? '';

    return Object.fromEntries(new URLSearchParams(query));
}

test.describe('Content Drive after the page editor', () => {
    let uvePage: UvePage | null = null;
    let folder: { siteName: string; name: string } | null = null;

    test.afterEach(async ({ request, apiHelpers }) => {
        try {
            if (uvePage) {
                await removeUvePage(request, uvePage).catch(() => undefined);
            }
        } finally {
            uvePage = null;
            if (folder) {
                await apiHelpers.deleteFolders(folder.siteName, [`/${folder.name}`]);
                folder = null;
            }
        }
    });

    test('restores the folder and the tree state through the Content Drive crumb @critical', async ({
        adminPage,
        apiHelpers,
        request,
        testSuffix
    }) => {
        const site = await apiHelpers.getDefaultSite();
        const folderName = `e2e-uve-return-${testSuffix}`;
        folder = { siteName: site.hostname, name: folderName };
        const folderInode = await createFolder(request, site.hostname, `/${folderName}`);
        uvePage = await createUvePage(request, testSuffix, {
            contentTypeIds: [],
            hostFolder: folderInode
        });
        const pageTitle = uvePage.page.title;

        const drive = new ContentDrivePage(adminPage);
        const editor = new UveEditorPage(adminPage);

        await drive.goTo();
        await drive.openFolder(folderName);
        await drive.toggleTree();
        await drive.expectTreeCollapsed();
        // Every param Content Drive writes has landed, the language filter seeded last, before the
        // state the author leaves is taken.
        await expect(adminPage).toHaveURL(/[?&]isTreeExpanded=false/);
        await expect(adminPage).toHaveURL(/[?&]filters=/);
        const left = contentDriveQuery(adminPage.url());
        expect(left['path']).toBe(`/${folderName}/`);

        await drive.openItem(pageTitle);
        await expect(adminPage).toHaveURL(/\/edit-page\/content/, { timeout: 20000 });
        await expect(editor.navBar).toBeVisible({ timeout: 20000 });

        await drive.crumb('Content Drive').click();

        await expect(drive.toolbar).toBeVisible({ timeout: 20000 });
        await expect.poll(() => contentDriveQuery(adminPage.url())).toEqual(left);
        await drive.expectTreeCollapsed();
        await drive.expectListContainsTitle(pageTitle);
    });
});
