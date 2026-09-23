import { ContentDrivePage } from '@pages';
import { type APIRequestContext } from '@playwright/test';
import { createFakePayloadTextField } from '@utils/dot-content-types.mock';
import { Portlet } from '@utils/portlets';

import { ContentDriveSearchScope } from './helpers/content-drive-search';
import { ContentDriveTree } from './helpers/content-drive-tree';

import { type ContentDriveApiHelpers, expect, test } from '../../fixtures/content-drive.fixture';
import { createContentlet, deleteContentlets } from '../../requests/contentlets';
import { createFakeContentType, deleteContentType } from '../../requests/contentType';

/**
 * Journey: Content Drive search scope — searching by Title or by All Fields (issue #37479)
 *
 * The query construction itself is the backend's half, covered by the Java integration suite.
 * What a browser adds is the wiring no lower suite sees end to end: the control driving real
 * `/api/v1/drive/search` requests, the rows those requests return, the address-bar round trip,
 * and the p-listbox toggle that emits `null` when the active option is re-clicked — the exact
 * kind of synthesized-event gap the keyboard spec exists for.
 */

/**
 * What one test's seeding produces. The term is the 8-character per-test suffix behind a `zq`
 * prefix — rare enough that the only matches in the drive are the rows this test planted.
 */
interface SeededWorld {
    term: string;
    /** A folder whose NAME contains the term, so folder rows match it (FR-011). */
    folderName: string;
    /** The default site's hostname, captured once so the teardown does not re-resolve it. */
    siteHostname: string;
    /** A contentlet the term reaches through its TITLE — it is the title's first token. */
    titleRowTitle: string;
    /** A contentlet the term reaches ONLY through its body field — its title never contains it. */
    fieldRowTitle: string;
    contentletIds: string[];
    contentTypeId: string;
}

/**
 * Seeds one test's search world through the REST API.
 *
 * The two contentlets are the pair that tells the scopes apart: in All Fields the term is found
 * via the catchall (the body token), so both rows come back; in Title scope the prefix match only
 * reaches the row whose title starts with the term, so the field row disappears. Both sit at the
 * site root, which is what makes the drive-wide search test readable without a second folder.
 */
async function seedSearchWorld(
    request: APIRequestContext,
    apiHelpers: ContentDriveApiHelpers,
    testSuffix: string
): Promise<SeededWorld> {
    const site = await apiHelpers.getDefaultSite();
    const term = `zq${testSuffix}`;
    const folderName = `${term}-folder`;

    await apiHelpers.createFolders(site.hostname, [`/${folderName}`]);

    const contentType = await createFakeContentType(request, {
        name: `ZqSearch${testSuffix}`,
        fields: [
            createFakePayloadTextField({ name: 'Title', variable: 'title', sortOrder: 1 }),
            createFakePayloadTextField({
                name: 'Search body',
                variable: 'searchBody',
                sortOrder: 2,
                // dotCMS does not index a field unless told to, and the whole point of this
                // field is to be the one the term reaches through the all-fields catchall. An
                // unindexed field makes the body row invisible in BOTH scopes, which fails the
                // test for the wrong reason.
                indexed: true
            })
        ]
    });

    const titleRow = await createContentlet(request, {
        contentType: contentType.variable,
        title: `${term} hero asset`,
        searchBody: 'a text no search term reaches'
    });
    const fieldRow = await createContentlet(request, {
        contentType: contentType.variable,
        title: `Herd notes ${testSuffix}`,
        searchBody: term
    });

    return {
        term,
        folderName,
        siteHostname: site.hostname,
        titleRowTitle: `${term} hero asset`,
        fieldRowTitle: `Herd notes ${testSuffix}`,
        contentletIds: [titleRow.identifier, fieldRow.identifier],
        contentTypeId: contentType.id
    };
}

test.describe('Content Drive Search Scope', () => {
    // These tests watch real round trips — each capture waits out the search debounce plus the
    // backend, and the teardown closes a recorded context and fires the API deletes — all inside
    // the default 60s budget. On a loaded CI runner (8 workers over a shared Docker backend, per
    // the pom's own notes) five of them finished their bodies right at the edge and ran out of
    // budget at teardown, three retries in a row. The doubled budget is not covering a broken
    // assertion — none fired — it sizes the budget to what watching the wire honestly costs; a
    // genuinely hung test still fails here, just with room to show which step hung.
    test.setTimeout(120_000);

    // Set by every test through `seed`, read by the teardown. Deliberately a describe-level
    // `let` used only for cleanup — each test seeds its own world before asserting on it, so
    // nothing is shared and `fullyParallel` stays honest.
    let world: SeededWorld | undefined;

    const seed = async (
        request: APIRequestContext,
        apiHelpers: ContentDriveApiHelpers,
        testSuffix: string
    ) => {
        world = await seedSearchWorld(request, apiHelpers, testSuffix);

        return world;
    };

    test.afterEach(async ({ request, apiHelpers }) => {
        if (world) {
            await deleteContentlets(request, world.contentletIds);

            // The site is already known — the seeding fetched it — so the teardown does not spend
            // a round trip re-resolving it. Every call here rides on the test's own timeout
            // budget; the leaner this stays, the more of that budget the browser teardown keeps.
            await apiHelpers.deleteFolders(world.siteHostname, [`/${world.folderName}`]);

            await deleteContentType(request, world.contentTypeId);
            world = undefined;
        }
    });

    test('presents the scope control on the all-fields default with a plain placeholder', async ({
        adminPage,
        apiHelpers,
        request,
        testSuffix
    }) => {
        await seed(request, apiHelpers, testSuffix);
        const drive = new ContentDrivePage(adminPage);
        const search = new ContentDriveSearchScope(adminPage);

        await drive.goTo();

        await search.expectActive('ALL_FIELDS');
        // The control names itself for assistive technology (FR-023), and the placeholder stays
        // the shared input's own "Search" — it deliberately does not describe the active scope
        // (FR-003 as amended).
        await expect(search.trigger).toHaveAttribute('aria-label', 'Search in');
        await expect(search.input).toHaveAttribute('placeholder', 'Search');
    });

    test('marks the active option and explains every option in the panel', async ({
        adminPage,
        apiHelpers,
        request,
        testSuffix
    }) => {
        await seed(request, apiHelpers, testSuffix);
        const drive = new ContentDrivePage(adminPage);
        const search = new ContentDriveSearchScope(adminPage);

        await drive.goTo();
        await search.open();

        await search.expectOptionMarked('ALL_FIELDS');
        // FR-022: each option carries the explanation of what it matches. Asserted on a stable
        // phrase of the English copy, so rewording the rest does not break the test.
        await search.expectOptionExplained('TITLE', 'name only');
        await search.expectOptionExplained('ALL_FIELDS', 'any text inside it');
    });

    test('narrows the results to title matches when title scope is chosen @critical', async ({
        adminPage,
        apiHelpers,
        request,
        testSuffix
    }) => {
        const seeded = await seed(request, apiHelpers, testSuffix);
        const drive = new ContentDrivePage(adminPage);
        const search = new ContentDriveSearchScope(adminPage);

        await drive.goTo();

        // The default scope sends no scope field at all — omitted, not null (FR-017) — and finds
        // both rows, the title one and the body-field one.
        const allFields = await drive.captureSearchPayload(
            () => drive.searchField.fill(seeded.term),
            (payload) => payload.filters.text === seeded.term
        );
        expect(allFields.filters).not.toHaveProperty('searchScope');
        expect(allFields.filters.text).toBe(seeded.term);
        await drive.expectListContainsTitle(seeded.titleRowTitle);
        await drive.expectListContainsTitle(seeded.fieldRowTitle);

        // Choosing a scope re-runs the search immediately (FR-004), carrying the scope (FR-017).
        // The prefix match reaches the title row but not the body-field row (FR-008).
        const title = await drive.captureSearchPayload(
            () => search.choose('TITLE'),
            (payload) => payload.filters.searchScope === 'TITLE'
        );
        expect(title.filters.text).toBe(seeded.term);
        expect(title.filters.searchScope).toBe('TITLE');
        await drive.expectListContainsTitle(seeded.titleRowTitle);
        await expect(drive.listTitles.filter({ hasText: seeded.fieldRowTitle })).toHaveCount(0);
    });

    test('restores all-fields results when the scope returns to all fields @critical', async ({
        adminPage,
        apiHelpers,
        request,
        testSuffix
    }) => {
        const seeded = await seed(request, apiHelpers, testSuffix);
        const drive = new ContentDrivePage(adminPage);
        const search = new ContentDriveSearchScope(adminPage);

        await drive.goTo();
        await drive.captureSearchPayload(
            () => drive.searchField.fill(seeded.term),
            (payload) => payload.filters.text === seeded.term
        );
        await search.choose('TITLE');

        // Back to the default: the scope is dropped from the request entirely, never sent as a
        // literal 'ALL_FIELDS' (FR-017, mirrored by the store deleting the key), and the row only
        // All Fields can see comes back (FR-009's no-regression promise).
        const restored = await drive.captureSearchPayload(
            () => search.choose('ALL_FIELDS'),
            (payload) => payload.filters.text === seeded.term && !('searchScope' in payload.filters)
        );
        expect(restored.filters).not.toHaveProperty('searchScope');
        await drive.expectListContainsTitle(seeded.fieldRowTitle);
    });

    test('does not search again when the active scope is re-selected @critical', async ({
        adminPage,
        apiHelpers,
        request,
        testSuffix
    }) => {
        const seeded = await seed(request, apiHelpers, testSuffix);
        const drive = new ContentDrivePage(adminPage);
        const search = new ContentDriveSearchScope(adminPage);

        await drive.goTo();

        // Type the term the guard protects. The capture waits for THIS search's response — a
        // predicate that only a term-carrying request satisfies — so the portlet's own startup
        // searches drain behind it without spending round trips flushing them by hand.
        await drive.captureSearchPayload(
            () => drive.searchField.fill(seeded.term),
            (payload) => payload.filters.text === seeded.term
        );

        // p-listbox is single-select with toggle semantics: re-clicking the active option emits
        // `null`, which the component must ignore. A re-run would reset the user to page 1 for
        // nothing, and only watching the wire can prove it did not happen. The guard reads
        // payloads, not URLs: a re-search the click caused would carry this same term, while a
        // leftover startup search (empty `text`) is not a violation — the predicate keeps the two
        // apart, so the proof stays exact without any settling choreography.
        await drive.expectNoSearchWhile(
            async () => {
                await search.open();
                await search.option('ALL_FIELDS').click();
                // A re-search would be a state change → store effect → request, all synchronous
                // with the click itself — unlike the typing path, which carries the debounce. The
                // panel closing is the deterministic signal that the click's consequences ran.
                await expect(search.panel).toBeHidden();
            },
            (payload) => payload.filters.text === seeded.term
        );
    });

    test('searches drive-wide and leaves the selected folder behind', async ({
        adminPage,
        apiHelpers,
        request,
        testSuffix
    }) => {
        const seeded = await seed(request, apiHelpers, testSuffix);
        const drive = new ContentDrivePage(adminPage);
        const tree = new ContentDriveTree(adminPage);

        await drive.goTo();

        // Narrow the listing to the (empty) seeded folder first, so what appears next cannot be
        // explained by the folder scope: both contentlets live at the site root, outside it.
        await tree.selectFolder(seeded.folderName);

        await drive.captureSearchPayload(
            () => drive.searchField.fill(seeded.term),
            (payload) => payload.filters.text === seeded.term
        );

        await drive.expectListContainsTitle(seeded.titleRowTitle);
        await drive.expectListContainsTitle(seeded.fieldRowTitle);
    });

    test('returns the scope to all fields when all filters are cleared @critical', async ({
        adminPage,
        apiHelpers,
        request,
        testSuffix
    }) => {
        const seeded = await seed(request, apiHelpers, testSuffix);
        const drive = new ContentDrivePage(adminPage);
        const search = new ContentDriveSearchScope(adminPage);

        await drive.goTo();
        await drive.captureSearchPayload(
            () => drive.searchField.fill(seeded.term),
            (payload) => payload.filters.text === seeded.term
        );
        await search.choose('TITLE');

        const clearAll = adminPage.getByTestId('clear-all-filters');
        await expect(clearAll).toBeVisible();
        await clearAll.click();

        // The scope qualifies the term; with the filters gone it must be gone too, or the trigger
        // would claim a scope the listing is not using (FR-020).
        await search.expectActive('ALL_FIELDS');
    });

    test('resolves a deep link to the scope it names and degrades an unknown one @critical', async ({
        adminPage,
        apiHelpers,
        request,
        testSuffix
    }) => {
        const seeded = await seed(request, apiHelpers, testSuffix);
        const drive = new ContentDrivePage(adminPage);
        const search = new ContentDriveSearchScope(adminPage);

        // The address carries the scope beside the other filters (FR-014), and the drive runs the
        // filtered search on load — no typing needed.
        await adminPage.goto(
            `${Portlet.ContentDrive}?filters=${encodeURIComponent(
                `title:${seeded.term};searchScope:TITLE`
            )}`
        );
        await expect(drive.toolbar).toBeVisible({ timeout: 20000 });

        await search.expectActive('TITLE');
        await drive.expectListContainsTitle(seeded.titleRowTitle);
        await expect(drive.listTitles.filter({ hasText: seeded.fieldRowTitle })).toHaveCount(0);

        // A stale or hand-edited scope must degrade to the default, never 400 the listing
        // (FR-015): the unknown value is dropped and the term keeps its All Fields breadth.
        await adminPage.goto(
            `${Portlet.ContentDrive}?filters=${encodeURIComponent(
                `title:${seeded.term};searchScope:WAT`
            )}`
        );
        await expect(drive.toolbar).toBeVisible({ timeout: 20000 });

        await search.expectActive('ALL_FIELDS');
        await drive.expectListContainsTitle(seeded.fieldRowTitle);
    });

    test('starts a clean visit on all fields after a scope was changed', async ({
        adminPage,
        apiHelpers,
        request,
        testSuffix
    }) => {
        const seeded = await seed(request, apiHelpers, testSuffix);
        const drive = new ContentDrivePage(adminPage);
        const search = new ContentDriveSearchScope(adminPage);

        await drive.goTo();
        await drive.captureSearchPayload(
            () => drive.searchField.fill(seeded.term),
            (payload) => payload.filters.text === seeded.term
        );
        await search.choose('TITLE');

        // The scope is filter state in the address, not a per-user preference (FR-016): a clean
        // entry — no query string — starts over on the default. A second goto alone would only
        // change the hash inside the same Angular document: no reload, no startup requests, and
        // goTo's own waits would stall on responses that never fire. Leaving the app first makes
        // the return a real page load — a genuinely clean visit, which is the thing under test.
        await adminPage.goto('about:blank');
        await drive.goTo();

        await search.expectActive('ALL_FIELDS');
    });

    test('searches a term made of query syntax without breaking the listing', async ({
        adminPage,
        apiHelpers,
        request,
        testSuffix
    }) => {
        await seed(request, apiHelpers, testSuffix);
        const drive = new ContentDrivePage(adminPage);

        await drive.goTo();

        // The term is treated as literal text (FR-027): the capture's predicate already proves
        // the request answered 200 — an unescaped term would have failed the query before any
        // results could render (FR-029), surfacing as the search error toast and a stale grid.
        await drive.captureSearchPayload(
            () => drive.searchField.fill('(a+b)'),
            (payload) => payload.filters.text === '(a+b)'
        );

        await expect(adminPage.locator('.p-toast-message')).toHaveCount(0);
        await expect(drive.listTitles).toHaveCount(0);
    });

    test('matches folders by name identically in both scopes', async ({
        adminPage,
        apiHelpers,
        request,
        testSuffix
    }) => {
        const seeded = await seed(request, apiHelpers, testSuffix);
        const drive = new ContentDrivePage(adminPage);
        const search = new ContentDriveSearchScope(adminPage);

        await drive.goTo();

        await drive.captureSearchPayload(
            () => drive.searchField.fill(seeded.term),
            (payload) => payload.filters.text === seeded.term
        );
        await drive.expectListContainsTitle(seeded.folderName);

        // Folder names match the term as a substring no matter the scope (FR-011): the scope
        // narrows contentlets, not the structure the user navigates through.
        await search.choose('TITLE');
        await drive.expectListContainsTitle(seeded.folderName);
    });
});
