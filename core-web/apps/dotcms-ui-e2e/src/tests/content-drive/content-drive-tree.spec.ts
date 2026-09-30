import { ContentDrivePage } from '@pages';
import { type Page } from '@playwright/test';

import { ContentDriveTree } from './helpers/content-drive-tree';

import { type ContentDriveApiHelpers, expect, test } from '../../fixtures/content-drive.fixture';

/**
 * Journey: Content Drive shared folder tree (#36733)
 * Critical happy paths only — load, expand, select → list, sidebar toggle.
 */
test.describe('Content Drive Folder Tree', () => {
    test('loads tree with site hostname and seeded folder @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const site = await apiHelpers.getDefaultSite();
        const folderName = `cd-root-${testSuffix}`;
        await apiHelpers.createFolders(site.hostname, [`/${folderName}`]);

        const drive = new ContentDrivePage(adminPage);
        const tree = new ContentDriveTree(adminPage);

        await drive.goTo();
        await drive.expectSiteHostname(site.hostname);
        await tree.expectVisible();
        await tree.expectFolderVisible(folderName);
    });

    test('expands nested folder and shows child @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const site = await apiHelpers.getDefaultSite();
        const parentName = `cd-parent-${testSuffix}`;
        const childName = `cd-child-${testSuffix}`;
        await apiHelpers.createFolders(site.hostname, [`/${parentName}/${childName}`]);

        const drive = new ContentDrivePage(adminPage);
        const tree = new ContentDriveTree(adminPage);

        await drive.goTo();
        await tree.expectFolderVisible(parentName);
        await tree.expectFolderNotVisible(childName);

        await tree.expandFolder(parentName);
        await tree.expectFolderVisible(childName);
    });

    test('shows a folder icon that opens and closes with the row', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        // #37362: folder rows lost their icon when the four folder trees were unified in #36848.
        // The icon must also revert on collapse — the half of the report that was never about
        // Content Drive.
        const site = await apiHelpers.getDefaultSite();
        const parentName = `cd-icon-${testSuffix}`;
        const childName = `cd-icon-child-${testSuffix}`;
        await apiHelpers.createFolders(site.hostname, [`/${parentName}/${childName}`]);

        const drive = new ContentDrivePage(adminPage);
        const tree = new ContentDriveTree(adminPage);

        await drive.goTo();
        await tree.expectFolderVisible(parentName);

        await tree.expectFolderIconVisible(parentName);
        await tree.expectFolderIconExpanded(parentName, false);

        await tree.expandFolder(parentName);
        await tree.expectFolderIconExpanded(parentName, true);

        await tree.collapseFolder(parentName);
        await tree.expectFolderIconExpanded(parentName, false);
    });

    test('selects folder and shows child folder in list @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const site = await apiHelpers.getDefaultSite();
        const parentName = `cd-select-${testSuffix}`;
        const childName = `cd-select-child-${testSuffix}`;
        await apiHelpers.createFolders(site.hostname, [`/${parentName}/${childName}`]);

        const drive = new ContentDrivePage(adminPage);
        const tree = new ContentDriveTree(adminPage);

        await drive.goTo();
        await tree.expectFolderVisible(parentName);
        await tree.selectFolder(parentName);
        await tree.expectFolderSelected(parentName);
        await drive.expectListContainsTitle(childName);
    });

    test('shows each folder opened from the table in the tree, loading its branch as it goes @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        // The tree only holds what it has fetched. Opening a folder from the table whose node was
        // never loaded used to clear the tree's selection, so the sidebar showed nothing.
        const site = await apiHelpers.getDefaultSite();
        const parentName = `cd-open-${testSuffix}`;
        const middleName = `cd-open-mid-${testSuffix}`;
        const leafName = `cd-open-leaf-${testSuffix}`;
        await apiHelpers.createFolders(site.hostname, [`/${parentName}/${middleName}/${leafName}`]);

        try {
            const drive = new ContentDrivePage(adminPage);
            const tree = new ContentDriveTree(adminPage);

            await drive.goTo();
            await tree.selectFolder(parentName);
            await drive.expectListContainsTitle(middleName);

            await drive.listTitles.filter({ hasText: middleName }).first().dblclick();
            await tree.expectFolderSelected(middleName);
            await drive.expectListContainsTitle(leafName);

            await drive.listTitles.filter({ hasText: leafName }).first().dblclick();
            await tree.expectFolderSelected(leafName);
        } finally {
            await apiHelpers.deleteFolders(site.hostname, [`/${parentName}`]);
        }
    });

    test('keeps an open branch open while it loads the branch of a folder opened from the table', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        // Loading the missing branch used to rebuild the whole tree, which collapsed every other
        // branch the author had open and read as the sidebar blinking.
        const site = await apiHelpers.getDefaultSite();
        const openName = `cd-kept-${testSuffix}`;
        const openChildName = `cd-kept-child-${testSuffix}`;
        const parentName = `cd-open2-${testSuffix}`;
        const middleName = `cd-open2-mid-${testSuffix}`;
        await apiHelpers.createFolders(site.hostname, [
            `/${openName}/${openChildName}`,
            `/${parentName}/${middleName}`
        ]);

        try {
            const drive = new ContentDrivePage(adminPage);
            const tree = new ContentDriveTree(adminPage);

            await drive.goTo();
            await tree.expandFolder(openName);
            await tree.expectFolderVisible(openChildName);

            await tree.selectFolder(parentName);
            await drive.listTitles.filter({ hasText: middleName }).first().dblclick();

            await tree.expectFolderSelected(middleName);
            await tree.expectFolderVisible(openChildName);
        } finally {
            await apiHelpers.deleteFolders(site.hostname, [`/${openName}`, `/${parentName}`]);
        }
    });

    test('scrolls the tree to a folder opened from the table', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        // Named to sort after the site's own folders, and after enough folders of its own that it
        // sits below the fold whatever the site holds: a fresh instance has too few to push it
        // there, and the test then fails on its precondition rather than on the scroll.
        const site = await apiHelpers.getDefaultSite();
        const parentName = `zz-cd-scroll-${testSuffix}`;
        const middleName = `zz-cd-scroll-mid-${testSuffix}`;
        const fillerNames = Array.from(
            { length: 40 },
            (_, index) => `zz-cd-pad-${testSuffix}-${String(index).padStart(2, '0')}`
        );
        await apiHelpers.createFolders(site.hostname, [
            ...fillerNames.map((name) => `/${name}`),
            `/${parentName}/${middleName}`
        ]);

        try {
            const drive = new ContentDrivePage(adminPage);
            const tree = new ContentDriveTree(adminPage);

            await drive.goTo();
            await tree.selectFolder(parentName);
            await drive.expectListContainsTitle(middleName);

            // Back to the top, so only the portlet can bring the folder into view.
            await adminPage.getByTestId('hierarchy-scroll').evaluate((container) => {
                [container, ...Array.from(container.querySelectorAll('*'))].forEach((element) => {
                    element.scrollTop = 0;
                });
            });
            const parentRow = adminPage
                .getByTestId('sidebar')
                .getByTestId('tree-node-label')
                .filter({ hasText: parentName });
            await expect(parentRow).not.toBeInViewport();

            await drive.listTitles.filter({ hasText: middleName }).first().dblclick();

            await tree.expectFolderSelected(middleName);
            await expect(
                adminPage
                    .getByTestId('sidebar')
                    .getByTestId('tree-node-label')
                    .filter({ hasText: middleName })
            ).toBeInViewport({ timeout: 10000 });
        } finally {
            await apiHelpers.deleteFolders(site.hostname, [
                `/${parentName}`,
                ...fillerNames.map((name) => `/${name}`)
            ]);
        }
    });

    /**
     * Opens, from the table, a folder whose branch the tree has already loaded, and expects the
     * tree to show it selected and scrolled into view.
     *
     * The selection sync used to swap the table's stand-in for the tree's own node before the
     * sidebar saw it, so the sidebar neither opened the branch nor scrolled.
     *
     * @param arrange what the author does to the loaded branch before opening the folder
     */
    const expectLoadedFolderRevealed =
        (arrange: (tree: ContentDriveTree, parentName: string) => Promise<void>) =>
        async ({
            adminPage,
            apiHelpers,
            testSuffix
        }: {
            adminPage: Page;
            apiHelpers: ContentDriveApiHelpers;
            testSuffix: string;
        }) => {
            const site = await apiHelpers.getDefaultSite();
            const parentName = `zz-cd-loaded-${testSuffix}`;
            const childName = `zz-cd-loaded-child-${testSuffix}`;
            await apiHelpers.createFolders(site.hostname, [`/${parentName}/${childName}`]);

            try {
                const drive = new ContentDrivePage(adminPage);
                const tree = new ContentDriveTree(adminPage);

                await drive.goTo();
                await tree.expandFolder(parentName);
                await tree.expectFolderVisible(childName);
                await arrange(tree, parentName);

                await tree.selectFolder(parentName);
                await drive.expectListContainsTitle(childName);
                await adminPage.getByTestId('hierarchy-scroll').evaluate((container) => {
                    [container, ...Array.from(container.querySelectorAll('*'))].forEach(
                        (element) => {
                            element.scrollTop = 0;
                        }
                    );
                });

                await drive.listTitles.filter({ hasText: childName }).first().dblclick();

                await tree.expectFolderSelected(childName);
                await tree.expectFolderInView(childName);
            } finally {
                await apiHelpers.deleteFolders(site.hostname, [`/${parentName}`]);
            }
        };

    test(
        'scrolls to a folder opened from the table whose branch is already loaded',
        expectLoadedFolderRevealed(async () => undefined)
    );

    test(
        'shows and scrolls to a folder opened from the table under a collapsed parent',
        expectLoadedFolderRevealed((tree, parentName) => tree.collapseFolder(parentName))
    );

    test('keeps the site root in view when the drive reloads on it', async ({ adminPage }) => {
        // The reveal on load centred the selected row. For the site root, at the top of the tree,
        // centring scrolled it out of view instead.
        const drive = new ContentDrivePage(adminPage);

        await drive.goTo();
        await drive.currentSiteHostname.click();
        await expect(adminPage).toHaveURL(/path=%2F|path=\//);

        await adminPage.reload();
        await drive.goTo();

        await expect(drive.currentSiteHostname).toBeInViewport();
        await expect(drive.allSiteContentRow).toBeInViewport();
    });

    test('toggles sidebar tree collapsed and expanded @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const site = await apiHelpers.getDefaultSite();
        const folderName = `cd-toggle-${testSuffix}`;
        await apiHelpers.createFolders(site.hostname, [`/${folderName}`]);

        const drive = new ContentDrivePage(adminPage);

        await drive.goTo();
        await drive.expectTreeExpanded();

        await drive.toggleTree();
        await drive.expectTreeCollapsed();

        await drive.toggleTree();
        await drive.expectTreeExpanded();
    });
});
