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
