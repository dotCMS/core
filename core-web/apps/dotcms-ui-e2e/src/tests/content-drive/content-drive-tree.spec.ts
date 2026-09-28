import { ContentDrivePage } from '@pages';

import { ContentDriveTree } from './helpers/content-drive-tree';

import { test } from '../../fixtures/content-drive.fixture';

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
