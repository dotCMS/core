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

    test('leaves the hierarchy unselected on System Host, and stays there @critical', async ({
        adminPage
    }) => {
        // Reported from the browser as "the root of the site selected and the system host
        // selected". Two faults met here. The hierarchy load resolved the reserved location
        // `SYSTEM_HOST` as the folder path `/SYSTEM_HOST/`, found nothing, and fell back to
        // selecting the site row -- so two entries read as current. Then the shell, which derives
        // the location *from* the selected node, took that site row and rewrote the location back
        // to the site root, pushing the author out of the scope they had just chosen.
        //
        // A reload is the case that made it reliable: on a cold start the location is already
        // System Host when the tree is built, so the fallback ran every time.
        const drive = new ContentDrivePage(adminPage);
        const tree = new ContentDriveTree(adminPage);

        await drive.goTo();
        await drive.selectSystemHost();
        await adminPage.reload();

        await drive.expectSelectedEntry('system-host');
        await tree.expectNothingSelected();
        expect(adminPage.url()).toContain('SYSTEM_HOST');
    });

    test('offers no new folder on System Host @critical', async ({ adminPage }) => {
        // System Host lists no folders, and its entry opens no tree, so one created there could
        // never be shown again by this portlet. The dialog could not even name where it would
        // land: the location is a reserved word, and pasting it after the hostname produced
        // `//demo.dotcms.comSYSTEM_HOST/`.
        const drive = new ContentDrivePage(adminPage);
        await drive.goTo();

        await drive.selectSystemHost();
        const onSystemHost = await drive.openNewMenu();

        expect(onSystemHost.join(' ')).not.toContain('Folder');
        // The other half of the rule: System Host holds content, and adding some is the whole
        // reason the scope accepts new items at all.
        expect(onSystemHost.join(' ')).toContain('Content');
    });

    test('names a path that resolves when creating a folder @critical', async ({
        adminPage,
        apiHelpers
    }) => {
        // The preview is built from the location the drive is open on, and only one of the three
        // kinds of location is a folder path. Asserted as "starts with the site and carries no
        // reserved word" rather than as an exact string, so it holds for whichever site the run
        // lands on.
        const site = await apiHelpers.getDefaultSite();
        const drive = new ContentDrivePage(adminPage);
        await drive.goTo();

        await drive.selectAllSiteContent();
        await drive.openNewMenu();
        await adminPage.getByRole('menuitem', { name: 'Folder' }).click();

        const path = await drive.folderDialogPath();

        expect(path).not.toContain('SYSTEM_HOST');
        expect(path).toBe(`//${site.hostname}/`);
    });

    test('refreshes the listing after an upload lands on System Host @critical', async ({
        adminPage,
        testSuffix
    }) => {
        // The grid reloads only when the finished run names the folder on screen, and both sides
        // of that comparison were built from the switcher's site glued to the location. On System
        // Host that gave `//demo.dotcms.comSYSTEM_HOST` for the listing and `//demo.dotcms.com`
        // for the batch -- neither naming where the files actually went, and never equal. The
        // files arrived; the listing they arrived in sat stale.
        const drive = new ContentDrivePage(adminPage);
        const title = `sys-upload-${testSuffix}.png`;

        await drive.goTo();
        await drive.selectSystemHost();
        await drive.chooseFilesForUpload([title]);

        // No reload of our own: the refresh arriving by itself is the whole assertion.
        await drive.expectListContainsTitle(title);
    });

    test('reports a run in flight and stops when it settles @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        // The toast replaced the toolbar indicator, and it is raised `sticky` so it cannot time
        // itself out. That makes ending it the store's job rather than PrimeNG's, which is the
        // half worth pinning: a run that never clears leaves the portlet claiming work is in
        // flight forever.
        const site = await apiHelpers.getDefaultSite();
        const folderName = `cd-toast-${testSuffix}`;
        await apiHelpers.createFolders(site.hostname, [`/${folderName}`]);

        const drive = new ContentDrivePage(adminPage);

        try {
            await drive.goTo();
            await drive.openFolder(folderName);
            await drive.chooseFilesForUpload([`toast-${testSuffix}.png`]);

            // Its own words, not the workflow sentence: an upload puts files INTO a place rather
            // than applying an action TO content, which is what "Applying Upload to ..." claimed.
            await drive.expectStatusToastContaining('Uploading');
            await drive.expectStatusToastGone();
        } finally {
            await apiHelpers.deleteFolders(site.hostname, [`/${folderName}`]);
        }
    });

    test('leaves the page controls clickable while a run is reported @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        // The status toast is a fixed box at the bottom centre of the viewport, which is where the
        // paginator lives. It reported an upload from directly on top of the page controls and
        // swallowed the click, so the page never changed and the listing sat on one page while the
        // paginator read as another.
        //
        // Nothing in the toast is clickable, so nothing in it should take a click. Asked of the
        // browser's own hit-testing at the toast's centre, which needs nothing enabled underneath
        // -- a freshly seeded folder is empty, and an empty listing disables its page controls.
        const site = await apiHelpers.getDefaultSite();
        const folderName = `cd-click-${testSuffix}`;
        await apiHelpers.createFolders(site.hostname, [`/${folderName}`]);

        const drive = new ContentDrivePage(adminPage);

        try {
            await drive.goTo();
            await drive.openFolder(folderName);
            await drive.chooseFilesForUpload([`click-${testSuffix}.png`]);
            await drive.expectStatusToastContaining('Uploading');

            expect(await drive.statusToastTakesClicksAtItsCentre()).toBe(false);
        } finally {
            await apiHelpers.deleteFolders(site.hostname, [`/${folderName}`]);
        }
    });

    test('says what all site content is showing, and only there @critical', async ({
        adminPage
    }) => {
        // The bar carries the sentence and the System Host toggle that used to be a chip in the
        // filter row. Only all site content gets one: the site root is this site's root and
        // nothing else, and System Host is shared content and nothing else, so neither leaves a
        // sentence anything to qualify.
        const drive = new ContentDrivePage(adminPage);
        await drive.goTo();

        await drive.selectAllSiteContent();
        await expect(drive.scopeBar).toBeVisible();
        await expect(drive.scopeBarToggle).toBeVisible();

        await drive.selectSystemHost();
        expect(await drive.scopeBarIsOpen()).toBe(false);
    });

    test('flips the sentence with the System Host toggle @critical', async ({ adminPage }) => {
        // The sentence and the switch are one statement: if the toggle can say "included" while
        // the words say "excluded", the bar is worse than no bar.
        const drive = new ContentDrivePage(adminPage);
        await drive.goTo();
        await drive.selectAllSiteContent();

        const before = (await drive.scopeBarSummary.innerText()).trim();
        await drive.toggleSystemHostInScopeBar();
        const after = (await drive.scopeBarSummary.innerText()).trim();

        expect(after).not.toBe(before);
        // Whichever way round the run starts, the pair must be the two halves of the same choice.
        expect([before, after].sort()).toEqual(
            [
                'All Files in site (System Host shared files excluded)',
                'All Files in site (System Host shared files included)'
            ].sort()
        );
    });

    test('carries the toggle into the URL so a reload keeps it @critical', async ({
        adminPage
    }) => {
        // The filter is written either way rather than cleared, so the applied state is spelled
        // out rather than implied by an absent key that happens to read as on.
        const drive = new ContentDrivePage(adminPage);
        await drive.goTo();
        await drive.selectAllSiteContent();

        await drive.toggleSystemHostInScopeBar();
        const summary = (await drive.scopeBarSummary.innerText()).trim();

        await adminPage.reload();
        await expect(drive.scopeBarSummary).toHaveText(summary);
    });
});
