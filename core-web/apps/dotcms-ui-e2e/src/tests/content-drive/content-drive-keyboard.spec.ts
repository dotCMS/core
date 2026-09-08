import { ContentDrivePage } from '@pages';
import { type Page } from '@playwright/test';

import { ContentDriveKeyboard } from './helpers/content-drive-keyboard';
import { ContentDriveTree } from './helpers/content-drive-tree';

import { type ContentDriveApiHelpers, expect, test } from '../../fixtures/content-drive.fixture';

/**
 * Journey: Content Drive keybindings (#32591)
 *
 * These are here because the unit suite cannot see the bugs this feature actually shipped. Three of
 * them got past a green Jest run and were found by hand in the browser: a synthesised `MouseEvent`
 * defaults to `cancelable: false`, so `preventDefault()` was a no-op; a `click` dispatched without
 * its `mousedown` carried no modifier; and holding Shift fires its own `keydown` before the arrow,
 * which reset the range base so shrinking could never deselect. Every one of those is a difference
 * between a constructed event and a real one, which is exactly the gap a browser test closes.
 *
 * Two of the assertions are also **regressions that predate the feature**: rows were a full-page tab
 * trap on load, and the listing was keyboard-unreachable entirely after a column sort.
 */

/** Rows the listing is seeded with. Three is the minimum that lets a range both grow and shrink. */
const ROW_COUNT = 3;

/** The one folder each seeding test creates. Derived from the suffix so teardown can find it. */
const seededFolder = (testSuffix: string) => `cd-keys-${testSuffix}`;

test.describe('Content Drive Keyboard', () => {
    // Keyed off the per-test suffix rather than a describe-level variable, so this stays correct
    // under `fullyParallel`. The two tests that seed nothing simply ask for a folder that was never
    // created, which `deleteFolders` is already best-effort about.
    test.afterEach(async ({ apiHelpers, testSuffix }) => {
        const site = await apiHelpers.getDefaultSite();
        await apiHelpers.deleteFolders(site.hostname, [`/${seededFolder(testSuffix)}`]);
    });

    /**
     * Seeds a folder with `ROW_COUNT` children and browses into it, so the listing holds a known
     * number of rows rather than whatever content the install happens to carry.
     */
    async function openSeededListing(
        adminPage: Page,
        apiHelpers: ContentDriveApiHelpers,
        testSuffix: string
    ) {
        const site = await apiHelpers.getDefaultSite();
        const parentName = seededFolder(testSuffix);
        await apiHelpers.createFolders(
            site.hostname,
            ['a', 'b', 'c'].map((child) => `/${parentName}/${child}-${testSuffix}`)
        );

        const drive = new ContentDrivePage(adminPage);
        const tree = new ContentDriveTree(adminPage);
        const keyboard = new ContentDriveKeyboard(adminPage);

        await drive.goTo();
        await tree.expectFolderVisible(parentName);
        await tree.selectFolder(parentName);
        await keyboard.expectRowCount(ROW_COUNT);

        return { drive, keyboard };
    }

    test('focuses the search box with the search shortcut @critical', async ({ adminPage }) => {
        const drive = new ContentDrivePage(adminPage);

        await drive.goTo();
        await expect(drive.searchField).not.toBeFocused();

        await adminPage.keyboard.press('/');

        await expect(drive.searchField).toBeFocused();
    });

    /**
     * The browser default has to be suppressed or a quick-find bar opens on this key and steals both
     * the keystroke and the focus the shortcut just placed. Asserted by typing after the shortcut:
     * if a find bar had taken the key, the characters would land there instead of in the search box.
     */
    test('does not let the browser act on the search shortcut @critical', async ({ adminPage }) => {
        const drive = new ContentDrivePage(adminPage);

        await drive.goTo();
        await adminPage.keyboard.press('/');
        await adminPage.keyboard.type('blog');

        await expect(drive.searchField).toBeFocused();
        await expect(drive.searchField).toHaveValue('blog');
    });

    /**
     * The reason a bare printable key needs the registry's typing rule. A synthesised event cannot
     * show this at all: only a real browser turns the keypress into a character in the field.
     */
    test('types a slash into the search box instead of re-firing @critical', async ({
        adminPage
    }) => {
        const drive = new ContentDrivePage(adminPage);

        await drive.goTo();
        await adminPage.keyboard.press('/');
        await expect(drive.searchField).toBeFocused();

        await adminPage.keyboard.type('a/b');

        await expect(drive.searchField).toHaveValue('a/b');
    });

    /**
     * Ticking a row leaves focus on its checkbox, which is an `<input>`. A blanket "any input is
     * typing" rule swallowed the search key there, and since the checkboxes are the primary way to
     * select rows, "tick a few, then search" is an ordinary sequence that did nothing at all.
     *
     * The focus assertion before the keypress is load-bearing: without it the test would pass
     * vacuously if the click left focus somewhere else.
     */
    test('reaches the search shortcut from a row checkbox @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const { drive, keyboard } = await openSeededListing(adminPage, apiHelpers, testSuffix);

        const checkbox = keyboard.row(0).getByTestId('item-checkbox').locator('input');
        await checkbox.click();
        await keyboard.expectSelectedCount(1);
        await expect(checkbox).toBeFocused();

        await adminPage.keyboard.press('/');

        await expect(drive.searchField).toBeFocused();
    });

    /**
     * The search key is not on its own key everywhere. `/` is `Shift+7` on German QWERTZ and
     * Spanish, `Shift+:` on French AZERTY, so the browser reports the character *and* the Shift that
     * produced it. Folding that modifier into the lookup left the shortcut dead for those users
     * while looking perfectly fine on a US machine.
     *
     * Driven through CDP rather than `keyboard.press`, which maps keys through a US layout and
     * cannot express "the `/` character, with Shift held". This is still a real browser input event,
     * not a constructed DOM one — it goes through the same path a physical keypress does.
     */
    test('reaches the search shortcut on a layout that needs shift for the slash @critical', async ({
        adminPage
    }) => {
        const drive = new ContentDrivePage(adminPage);

        await drive.goTo();
        await expect(drive.searchField).not.toBeFocused();

        const cdp = await adminPage.context().newCDPSession(adminPage);
        await cdp.send('Input.dispatchKeyEvent', {
            type: 'keyDown',
            key: '/',
            code: 'Digit7',
            text: '/',
            modifiers: 8 // Shift
        });

        await expect(drive.searchField).toBeFocused();
    });

    test('focuses the search box with the alias too', async ({ adminPage }) => {
        const drive = new ContentDrivePage(adminPage);
        const keyboard = new ContentDriveKeyboard(adminPage);

        await drive.goTo();
        await expect(drive.searchField).not.toBeFocused();

        await keyboard.pressShortcut('k');

        await expect(drive.searchField).toBeFocused();
    });

    test('toggles the folder tree with the tree shortcut', async ({ adminPage }) => {
        const drive = new ContentDrivePage(adminPage);
        const keyboard = new ContentDriveKeyboard(adminPage);

        await drive.goTo();
        await drive.expectTreeExpanded();

        await keyboard.pressShortcut('b');
        await drive.expectTreeCollapsed();

        await keyboard.pressShortcut('b');
        await drive.expectTreeExpanded();
    });

    test('leaves exactly one row in the tab order @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        // Pre-existing defect, not one this feature introduced: with no row index bound the tab-stop
        // comparison was `undefined === undefined`, so every row was a tab stop and getting past the
        // listing took one press of Tab per row.
        const { keyboard } = await openSeededListing(adminPage, apiHelpers, testSuffix);

        await keyboard.expectSingleTabStop(ROW_COUNT);
    });

    test('keeps the listing reachable after a column sort @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        // The other half of the same defect, and the reason the tab stop is written after every
        // render rather than bound: the table clears its own anchor at the end of a sort, and the
        // comparison became `null === undefined`, leaving *no* row reachable at all.
        const { drive, keyboard } = await openSeededListing(adminPage, apiHelpers, testSuffix);

        await drive.sortByFirstColumn();
        await keyboard.expectRowCount(ROW_COUNT);

        await keyboard.expectSingleTabStop(ROW_COUNT);
    });

    test('moves focus through the listing with the arrow keys @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const { keyboard } = await openSeededListing(adminPage, apiHelpers, testSuffix);

        await keyboard.clickRow(0);
        await keyboard.expectRowFocused(0);

        await adminPage.keyboard.press('ArrowDown');
        await keyboard.expectRowFocused(1);

        await adminPage.keyboard.press('ArrowUp');
        await keyboard.expectRowFocused(0);
    });

    test('grows and shrinks a range with shift and the arrow keys @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const { keyboard } = await openSeededListing(adminPage, apiHelpers, testSuffix);

        await keyboard.clickRow(0);
        await keyboard.expectSelectedCount(1);

        await adminPage.keyboard.press('Shift+ArrowDown');
        await keyboard.expectSelectedCount(2);

        await adminPage.keyboard.press('Shift+ArrowDown');
        await keyboard.expectSelectedCount(ROW_COUNT);

        // The shrink is the half a synthesised sequence could not catch: without Shift's own keydown
        // the range base was recaptured every step, so coming back toward the anchor deselected
        // nothing and the selection stayed at three.
        await adminPage.keyboard.press('Shift+ArrowUp');
        await keyboard.expectSelectedCount(2);
    });

    test('selects a range with shift and a click', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const { keyboard } = await openSeededListing(adminPage, apiHelpers, testSuffix);

        await keyboard.clickRow(0);
        await keyboard.expectSelectedCount(1);

        await keyboard.shiftClickRow(ROW_COUNT - 1);

        await keyboard.expectSelectedCount(ROW_COUNT);
    });

    test('clears the selection with escape @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const { keyboard } = await openSeededListing(adminPage, apiHelpers, testSuffix);

        await keyboard.clickRow(0);
        await keyboard.expectSelectedCount(1);

        await adminPage.keyboard.press('Escape');

        await keyboard.expectSelectedCount(0);
    });

    /**
     * Escape clears the selection and stops there. It used to clear every active filter once the
     * selection was gone, which put a destructive, hard-to-undo action behind a stray press of the
     * most-reached-for key on the keyboard. Clearing filters is the "Clear all" control's job.
     */
    test('leaves the search term alone when escape clears the selection @critical', async ({
        adminPage,
        apiHelpers,
        testSuffix
    }) => {
        const { drive, keyboard } = await openSeededListing(adminPage, apiHelpers, testSuffix);

        await drive.searchField.fill('keep-me');
        await keyboard.clickRow(0);
        await keyboard.expectSelectedCount(1);

        await adminPage.keyboard.press('Escape');
        await keyboard.expectSelectedCount(0);

        await expect(drive.searchField).toHaveValue('keep-me');

        // And a second press, with nothing selected, still must not touch it.
        await adminPage.keyboard.press('Escape');

        await expect(drive.searchField).toHaveValue('keep-me');
    });
});
