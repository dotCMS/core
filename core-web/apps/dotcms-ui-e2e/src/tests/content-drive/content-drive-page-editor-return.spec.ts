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
    let folderName: string;
    let siteName: string;

    test.afterEach(async ({ request, apiHelpers }) => {
        if (uvePage) {
            await removeUvePage(request, uvePage);
            uvePage = null;
        }

        await apiHelpers.deleteFolders(siteName, [`/${folderName}`]);
    });

    test('restores the folder and the tree state through the Content Drive crumb @critical', async ({
        adminPage,
        apiHelpers,
        request,
        testSuffix
    }) => {
        const site = await apiHelpers.getDefaultSite();
        siteName = site.hostname;
        folderName = `e2e-uve-return-${testSuffix}`;
        const folderInode = await createFolder(request, siteName, `/${folderName}`);
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
        await expect(adminPage).toHaveURL(/[?&]isTreeExpanded=false/);
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
