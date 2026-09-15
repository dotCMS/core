import { ContentDrivePage } from '@pages';
import { expect } from '@playwright/test';

import { ContentDriveTree } from './helpers/content-drive-tree';

import { test } from '../../fixtures/content-drive.fixture';

/**
 * Journey: Content Drive browse scopes (#37426).
 *
 * The sidebar offers three selections rather than one: All Site Content at the top, the site
 * hierarchy in the middle, and System Host at the bottom. These are the two independent tests the
 * spec defines for user stories 1 and 2, plus the selection rule that binds them — exactly one
 * entry is ever current.
 *
 * Kept to what only a browser can answer. Which items each scope returns is pinned by the
 * integration tests against the endpoint; what these cover is that the entries exist, that
 * choosing one changes what is listed, and that the selection cannot land in two places at once.
 */
test.describe('Content Drive Browse Scopes', () => {
    test('offers all site content and System Host around the hierarchy @critical', async ({
        adminPage,
        apiHelpers
    }) => {
        const site = await apiHelpers.getDefaultSite();

        const drive = new ContentDrivePage(adminPage);
        const tree = new ContentDriveTree(adminPage);

        await drive.goTo();

        // The site row is still the tree's first node: the two new entries are buttons outside the
        // hierarchy and carry no `tree-node-label`, which is what keeps this assertion working.
        await drive.expectSiteHostname(site.hostname);
        await tree.expectVisible();

        await expect(drive.allSiteContentRow).toBeVisible();
        await expect(drive.systemHostRow).toBeVisible();
    });

    test('puts the entries above and below the hierarchy, not inside it @critical', async ({
        adminPage
    }) => {
        // Structure, not decoration: System Host stays reachable however many folders are
        // expanded, because the hierarchy between them is the only part that scrolls.
        const drive = new ContentDrivePage(adminPage);
        await drive.goTo();

        const all = await drive.allSiteContentRow.boundingBox();
        const systemHost = await drive.systemHostRow.boundingBox();

        expect(all).toBeTruthy();
        expect(systemHost).toBeTruthy();
        expect(all?.y ?? 0).toBeLessThan(systemHost?.y ?? 0);
        expect(systemHost?.y ?? 0).toBeGreaterThan((all?.y ?? 0) + (all?.height ?? 0));
    });

    test('keeps exactly one entry current as the user moves between them @critical', async ({
        adminPage
    }) => {
        const drive = new ContentDrivePage(adminPage);
        await drive.goTo();

        await drive.selectSystemHost();
        await drive.expectSelectedEntry('system-host');
        expect(await drive.isEntrySelected('all')).toBe(false);

        await drive.selectAllSiteContent();
        await drive.expectSelectedEntry('all');
        expect(await drive.isEntrySelected('system-host')).toBe(false);
    });

    test('lists content from inside folders under all site content @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        // The distinction the feature exists for: a file inside a folder is absent from the site
        // root and present in the flat view.
        const site = await apiHelpers.getDefaultSite();
        const folderName = `cd-scope-${testSuffix}`;
        await apiHelpers.createFolders(site.hostname, [`/${folderName}`]);

        const drive = new ContentDrivePage(adminPage);
        await drive.goTo();
        await drive.openFolder(folderName);
        await drive.dropFilesOnList([`scoped-${testSuffix}.png`]);
        await drive.expectUploadedTitle(folderName, `scoped-${testSuffix}.png`);

        await drive.selectAllSiteContent();
        await drive.expectListContainsTitle(`scoped-${testSuffix}.png`);
    });

    test('restores the tree selection when the user goes back to a folder @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        // Reported from the browser: Back restored the URL and the listing, while the tree showed
        // nothing selected — so the sidebar stopped agreeing with what it was displaying. The two
        // standalone entries were never affected, because they derive their state from the
        // location; the tree's is stored, and nothing brought it back in line.
        const site = await apiHelpers.getDefaultSite();
        const folderName = `cd-back-${testSuffix}`;
        await apiHelpers.createFolders(site.hostname, [`/${folderName}`]);

        const drive = new ContentDrivePage(adminPage);
        const tree = new ContentDriveTree(adminPage);

        await drive.goTo();
        await drive.openFolder(folderName);
        await tree.expectFolderSelected(folderName);

        await drive.selectSystemHost();
        await drive.expectSelectedEntry('system-host');

        await adminPage.goBack();

        await tree.expectFolderSelected(folderName);
        expect(await drive.isEntrySelected('system-host')).toBe(false);
    });

    test('carries the selection in the URL so a reload reopens it @critical', async ({
        adminPage
    }) => {
        // One value says where the drive is browsing, so a shared link cannot disagree with
        // itself about which entry was open.
        const drive = new ContentDrivePage(adminPage);
        await drive.goTo();

        await drive.selectSystemHost();
        expect(adminPage.url()).toContain('SYSTEM_HOST');

        await adminPage.reload();
        await drive.expectSelectedEntry('system-host');
    });
});
