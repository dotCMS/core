import { describe, expect } from '@jest/globals';
import { SpectatorService, createServiceFactory, mockProvider } from '@openng/spectator/jest';
import { of } from 'rxjs';

import { DotContentDriveService, DotLanguagesService } from '@dotcms/data-access';
import { SiteService } from '@dotcms/dotcms-js';
import { DotContentDriveSearchResponse } from '@dotcms/dotcms-models';
import { createFakeContentlet } from '@dotcms/utils-testing';

import { ADD_RELATIONSHIPS_PAGE_SIZE, AddRelationshipsStore } from './add-relationships.store';
import { ConstrainedIdentifiersService } from './constrained-identifiers.service';

import { AddRelationshipsInput } from '../models/add-relationships.models';

const item = (n: number) =>
    createFakeContentlet({
        inode: `inode-${n}`,
        identifier: `id-${n}`,
        title: `Content ${n}`,
        languageId: 1
    });

const response = (
    list: ReturnType<typeof item>[],
    overrides: Partial<DotContentDriveSearchResponse> = {}
): DotContentDriveSearchResponse => ({
    list,
    contentCount: list.length,
    folderCount: 0,
    hasMoreContent: false,
    hasMoreFolders: false,
    nextContentCursor: 0,
    nextFolderCursor: 0,
    ...overrides
});

describe('AddRelationshipsStore (US2 — selection)', () => {
    let spectator: SpectatorService<InstanceType<typeof AddRelationshipsStore>>;
    let store: InstanceType<typeof AddRelationshipsStore>;
    const searchMock = jest.fn();
    const constrainedMock = jest.fn();

    const createService = createServiceFactory({
        service: AddRelationshipsStore,
        providers: [
            mockProvider(DotContentDriveService, { search: searchMock }),
            mockProvider(SiteService, { currentSite: { hostname: 'demo.dotcms.com' } }),
            mockProvider(ConstrainedIdentifiersService, { get: constrainedMock }),
            mockProvider(DotLanguagesService, {
                get: jest
                    .fn()
                    .mockReturnValue(
                        of([{ id: 1, language: 'English', languageCode: 'en', isoCode: 'en-us' }])
                    )
            })
        ]
    });

    const baseInput: AddRelationshipsInput = {
        contentTypeId: 'target-type',
        selected: [],
        selectionMode: 'multiple'
    };

    /** The body of the last request the store issued. */
    const lastRequest = () => searchMock.mock.calls.at(-1)?.[0];

    beforeEach(() => {
        searchMock.mockReset().mockReturnValue(of(response([item(1), item(2)])));
        constrainedMock.mockReset().mockReturnValue(of(new Set<string>()));
        spectator = createService();
        store = spectator.service;
    });

    describe('seeding the selection (FR-009)', () => {
        it('seeds from the caller, so an already-related item is selected before any search runs', () => {
            store.initialize({ ...baseInput, selected: [item(7)] });

            expect(store.$isSelected()('id-7')).toBe(true);
            expect(searchMock).not.toHaveBeenCalled();
        });

        /**
         * The regression this whole design exists to prevent. The dialog this replaces rebuilt the
         * pre-selection by filtering its *first search response*, so an already-related item that
         * response did not contain was never checked — and confirming replaced the relationship
         * with the rows it could see, silently unrelating the rest.
         */
        it('does NOT rebuild the selection from the search response', () => {
            store.initialize({ ...baseInput, selected: [item(7)] });
            // The search returns a completely different page, containing nothing selected.
            searchMock.mockReturnValue(of(response([item(1), item(2)])));
            store.load();

            expect(store.$isSelected()('id-7')).toBe(true);
            expect(store.$selectedItems()).toHaveLength(1);
        });

        it('starts empty when the caller relates nothing', () => {
            store.initialize(baseInput);

            expect(store.$selectedItems()).toEqual([]);
        });
    });

    describe('the selection survives browsing (FR-011)', () => {
        beforeEach(() => {
            store.initialize(baseInput);
            store.load();
            store.toggleSelection(item(1));
        });

        it('survives a page change', () => {
            searchMock.mockReturnValue(of(response([item(3), item(4)])));
            store.nextPage();

            expect(store.$isSelected()('id-1')).toBe(true);
        });

        it('survives a new search', () => {
            store.patchFilters({ title: 'something else' });
            store.load();

            expect(store.$isSelected()('id-1')).toBe(true);
        });

        it('survives a filter change', () => {
            store.patchFilters({ languageId: ['2'] });
            store.load();

            expect(store.$isSelected()('id-1')).toBe(true);
        });

        it('survives a sort change', () => {
            store.setSort({ field: 'title', order: 'asc' });
            store.load();

            expect(store.$isSelected()('id-1')).toBe(true);
        });

        it('leaves the selection byte-identical across all of them', () => {
            const before = store.$selectedItems();

            store.patchFilters({ title: 'x' });
            store.load();
            store.setSort({ field: 'title', order: 'asc' });
            store.load();

            expect(store.$selectedItems()).toEqual(before);
        });
    });

    describe('confirmation reconciles against the whole selection, not the page', () => {
        /**
         * The data-loss case, stated as a test. An item related in another locale, or simply beyond
         * the first page, is one the picker's own search may never return — and per ADR-0018 the
         * free-text filter goes through the search index, which lags behind writes, so "search for
         * it again" is not a reliable way to find content that demonstrably exists.
         */
        it('still emits an already-related item that no search response ever returned', () => {
            store.initialize({ ...baseInput, selected: [item(9)] });
            searchMock.mockReturnValue(of(response([item(1), item(2)])));
            store.load();

            expect(store.$selectedItems().map((c) => c.identifier)).toContain('id-9');
        });

        it('emits the selection in insertion order', () => {
            store.initialize(baseInput);
            store.toggleSelection(item(2));
            store.toggleSelection(item(1));

            expect(store.$selectedItems().map((c) => c.identifier)).toEqual(['id-2', 'id-1']);
        });

        it('does not duplicate an item already related when it is checked again', () => {
            store.initialize({ ...baseInput, selected: [item(1)] });
            store.toggleSelection(item(1));
            store.toggleSelection(item(1));

            expect(store.$selectedItems().filter((c) => c.identifier === 'id-1')).toHaveLength(1);
        });
    });

    describe('unchecking removes (FR-010)', () => {
        it('removes an already-related item from the selection', () => {
            store.initialize({ ...baseInput, selected: [item(1), item(2)] });
            store.toggleSelection(item(1));

            expect(store.$isSelected()('id-1')).toBe(false);
            expect(store.$selectedItems().map((c) => c.identifier)).toEqual(['id-2']);
        });

        it('can empty the selection entirely — a real, reachable state', () => {
            store.initialize({ ...baseInput, selected: [item(1)] });
            store.toggleSelection(item(1));

            expect(store.$selectedItems()).toEqual([]);
        });
    });

    describe('cardinality (FR-007)', () => {
        it('accumulates in multiple mode', () => {
            store.initialize({ ...baseInput, selectionMode: 'multiple' });
            store.toggleSelection(item(1));
            store.toggleSelection(item(2));

            expect(store.$selectedItems()).toHaveLength(2);
        });

        it('replaces the previous pick in single mode', () => {
            store.initialize({ ...baseInput, selectionMode: 'single' });
            store.toggleSelection(item(1));
            store.toggleSelection(item(2));

            expect(store.$selectedItems().map((c) => c.identifier)).toEqual(['id-2']);
        });
    });

    /**
     * FR-008 end to end — **the dialog has to ask**, not merely be able to store an answer.
     *
     * The first version of this store exposed `setConstrainedIdentifiers` and nothing ever called
     * it: the lookup had been deleted along with the old dialog's service, so every already-claimed
     * row was selectable and confirming took the child from its other parent, silently. The tests
     * missed it because they set the constrained set by hand instead of asserting that opening the
     * dialog fetches it.
     */
    describe('fetching what is already claimed (FR-008)', () => {
        const parentContext = {
            cardinality: 0, // ONE_TO_MANY — a child may have exactly one parent
            isParentField: true,
            parentContentTypeId: 'parent-type',
            fieldVariable: 'relation',
            currentContentIdentifier: 'current-id'
        };

        it('asks for the claimed identifiers when the field is the parent of a one-parent relationship', () => {
            store.initialize({ ...baseInput, ...parentContext });

            expect(constrainedMock).toHaveBeenCalledWith({
                parentContentTypeId: 'parent-type',
                fieldVariable: 'relation',
                currentContentIdentifier: 'current-id'
            });
        });

        it('marks what came back as unselectable', () => {
            constrainedMock.mockReturnValue(of(new Set(['id-1'])));
            store.initialize({ ...baseInput, ...parentContext });

            expect(store.$isConstrained()('id-1')).toBe(true);
            expect(store.$isConstrained()('id-2')).toBe(false);
        });

        it('does not ask on a many-to-many relationship, where nothing is claimed', () => {
            store.initialize({ ...baseInput, ...parentContext, cardinality: 3 });

            expect(constrainedMock).not.toHaveBeenCalled();
        });

        it('does not ask from the child side', () => {
            store.initialize({ ...baseInput, ...parentContext, isParentField: false });

            expect(constrainedMock).not.toHaveBeenCalled();
        });

        /** Contract C3: the filter-time caller supplies no parent context at all. */
        it('does not ask when the caller supplied no parent context', () => {
            store.initialize(baseInput);

            expect(constrainedMock).not.toHaveBeenCalled();
        });
    });

    describe('content claimed by another parent (FR-008)', () => {
        it('reports a constrained identifier as constrained', () => {
            store.initialize(baseInput);
            store.setConstrainedIdentifiers(new Set(['id-1']));

            expect(store.$isConstrained()('id-1')).toBe(true);
            expect(store.$isConstrained()('id-2')).toBe(false);
        });

        it('refuses to select a constrained item', () => {
            store.initialize(baseInput);
            store.setConstrainedIdentifiers(new Set(['id-1']));
            store.toggleSelection(item(1));

            expect(store.$isSelected()('id-1')).toBe(false);
        });

        /**
         * Contract C3. Filter-time callers supply no parent context, and a lookup computed from a
         * partial set would be worse than no lookup — it would disable rows for the wrong reason.
         */
        it('has no constrained identifiers when the caller supplied no parent context', () => {
            store.initialize(baseInput);

            expect(store.$isConstrained()('id-1')).toBe(false);
        });
    });

    describe('select-all is page-scoped but never discards off-page picks', () => {
        it('selects every selectable row on the current page', () => {
            store.initialize(baseInput);
            store.load();
            store.toggleSelectAllVisible(true);

            expect(store.$selectedItems().map((c) => c.identifier)).toEqual(['id-1', 'id-2']);
        });

        it('preserves picks made on another page', () => {
            store.initialize({ ...baseInput, selected: [item(9)] });
            store.load();
            store.toggleSelectAllVisible(true);

            expect(store.$selectedItems().map((c) => c.identifier)).toContain('id-9');
        });

        it('unselecting all clears only the current page, leaving off-page picks alone', () => {
            store.initialize({ ...baseInput, selected: [item(9)] });
            store.load();
            store.toggleSelectAllVisible(true);
            store.toggleSelectAllVisible(false);

            expect(store.$selectedItems().map((c) => c.identifier)).toEqual(['id-9']);
        });

        it('never selects a constrained row', () => {
            store.initialize(baseInput);
            store.load();
            store.setConstrainedIdentifiers(new Set(['id-1']));
            store.toggleSelectAllVisible(true);

            expect(store.$isSelected()('id-1')).toBe(false);
        });
    });

    describe('the selected-items review (FR-014)', () => {
        /**
         * It reads from the selection, never from the current page — otherwise it could only ever
         * show the picks that happen to be on screen, which is the same bug as everywhere else in
         * this file wearing a different hat.
         */
        it('lists the whole selection, including items not on the current page', () => {
            store.initialize({ ...baseInput, selected: [item(9)] });
            store.load();
            store.setViewMode('selected');

            expect(store.$visibleItems().map((c) => c.identifier)).toContain('id-9');
        });

        it('returns to the page of results when switched back', () => {
            store.initialize(baseInput);
            store.load();
            store.setViewMode('selected');
            store.setViewMode('all');

            expect(store.$visibleItems().map((c) => c.identifier)).toEqual(['id-1', 'id-2']);
        });
    });

    /**
     * US3 — the request that leaves the dialog.
     *
     * These assert the **body**, not merely that a call happened: "reuses the Content Drive search"
     * is only true if what goes over the wire is a Content Drive search request pinned to the
     * relationship's type, and a spy on the method name would pass either way.
     */
    describe('the search request (US3)', () => {
        /**
         * `assetPath` is mandatory and is resolved **before** any filter: `AssetPathResolver`
         * parses it as a URI, and an empty one fails the whole request with
         * "can not resolve a valid hostName [null]".
         *
         * This case exists because it was missing. The pin assertions below use
         * `objectContaining`, which says nothing about the rest of the body — so the dialog shipped
         * an empty `assetPath` through a green suite and only failed against a real backend.
         */
        it('carries a resolvable assetPath, not an empty one', () => {
            store.initialize(baseInput);
            store.load();

            expect(lastRequest().assetPath).toBe('//demo.dotcms.com/');
        });

        it('scopes to the contentlet own site when one is supplied', () => {
            store.initialize({
                ...baseInput,
                contentletContext: { hostName: 'other.dotcms.com' }
            });
            store.load();

            expect(lastRequest().assetPath).toBe('//other.dotcms.com/');
        });

        /**
         * A relationship relates content, never a folder. The endpoint returns folders unless told
         * not to, and without this the list opened full of them.
         */
        it('asks for content only, never folders', () => {
            store.initialize(baseInput);
            store.load();

            expect(lastRequest().showFolders).toBe(false);
        });

        /**
         * The site chip has to change the **request**, not just a value in the filter bag.
         *
         * It originally wrote `site` through the filter facade, which `buildRequest` never reads —
         * so the chip moved a value nobody consumed and the results never changed. The scope lives
         * on the store for the same reason Content Drive keeps its browsed folder out of the
         * filters: "Clear all" resets filters and deliberately leaves the browsing scope alone.
         */
        it('re-scopes the search when the site changes', () => {
            store.initialize(baseInput);
            store.load();
            store.setScope({ hostname: 'other.dotcms.com' });

            expect(lastRequest().assetPath).toBe('//other.dotcms.com/');
        });

        it('scopes to a folder within a site', () => {
            store.initialize(baseInput);
            store.setScope({ hostname: 'demo.dotcms.com', path: '/blog/' });

            expect(lastRequest().assetPath).toBe('//demo.dotcms.com/blog/');
        });

        it('returns to the first page when the scope changes', () => {
            store.initialize(baseInput);
            store.setPage(3);
            store.setScope({ hostname: 'other.dotcms.com' });

            expect(store.page().number).toBe(1);
        });

        it('keeps the selection when the scope changes', () => {
            store.initialize(baseInput);
            store.load();
            store.toggleSelection(item(1));
            store.setScope({ hostname: 'other.dotcms.com' });

            expect(store.$isSelected()('id-1')).toBe(true);
        });

        it('labels the scope with the site actually being searched, never "all sites"', () => {
            store.initialize(baseInput);

            expect(store.scopeLabel()).toBe('demo.dotcms.com');

            store.setScope({ hostname: 'other.dotcms.com', path: '/blog/' });
            expect(store.scopeLabel()).toBe('other.dotcms.com/blog/');
        });

        /**
         * T101 — the Locale column rendered empty because rows reached the field with only a
         * `languageId`. `LanguagePipe` needs the whole `DotLanguage`, and the dialog this replaced
         * resolved it for exactly this reason.
         */
        it('attaches the full language object to every loaded row', () => {
            searchMock.mockReturnValue(of(response([item(1)])));
            store.initialize(baseInput);
            store.load();

            expect(store.items()[0].language).toEqual(
                expect.objectContaining({ language: 'English' })
            );
        });

        it('keeps the language object on a confirmed selection', () => {
            searchMock.mockReturnValue(of(response([item(1)])));
            store.initialize(baseInput);
            store.load();
            store.toggleSelection(store.items()[0] as never);

            expect(store.$selectedItems()[0].language).toEqual(
                expect.objectContaining({ language: 'English' })
            );
        });

        /**
         * "Shared Assets" is a synthetic site whose hostname carries a space, so
         * `new URI("//System Host/")` throws `Illegal character in authority` — it reached the
         * editor as a 500 rendered as "The search could not be completed". Content Drive reaches
         * the same conclusion its own way, by skipping the fetch for SYSTEM_HOST outright.
         */
        it('asks the backend nothing for a host it cannot parse', () => {
            store.initialize(baseInput);
            store.load();
            searchMock.mockClear();

            store.setScope({ hostname: 'System Host' });

            expect(searchMock).not.toHaveBeenCalled();
            expect(store.items()).toEqual([]);
            expect(store.errorMessage()).toBeNull();
        });

        /**
         * `reset()` is what the shared bar's "Clear all" calls. Changing where you are looking is
         * not un-picking what you already picked.
         */
        it('keeps the selection through a reset', () => {
            store.initialize(baseInput);
            store.load();
            store.toggleSelection(item(1));
            store.reset();

            expect(store.$isSelected()('id-1')).toBe(true);
        });

        it('pins contentTypes to the relationship target type', () => {
            store.initialize(baseInput);
            store.load();

            expect(lastRequest()).toEqual(
                expect.objectContaining({ contentTypes: ['target-type'] })
            );
        });

        it('keeps the pin on every subsequent search, filter and sort', () => {
            store.initialize(baseInput);
            store.load();
            store.patchFilters({ title: 'anything' });
            store.load();
            store.setSort({ field: 'title', order: 'asc' });

            expect(lastRequest()).toEqual(
                expect.objectContaining({ contentTypes: ['target-type'] })
            );
        });

        /**
         * O8, from the request's side. No filter the editor can reach may widen the result set past
         * the type the field accepts — writing the key straight into the bag must not reach the
         * request.
         */
        it('cannot be widened past the target type through the filter bag', () => {
            store.initialize(baseInput);
            store.patchFilters({ contentTypes: ['some-other-type'] });
            store.load();

            expect(lastRequest().contentTypes).toEqual(['target-type']);
        });

        it('carries the free-text term as the drive search filter', () => {
            store.initialize(baseInput);
            store.patchFilters({ title: 'quarterly' });
            store.load();

            expect(lastRequest()).toEqual(
                expect.objectContaining({ filters: { text: 'quarterly' } })
            );
        });

        it('carries the selected locales as language', () => {
            store.initialize(baseInput);
            store.patchFilters({ languageId: ['1', '2'] });
            store.load();

            expect(lastRequest()).toEqual(expect.objectContaining({ language: ['1', '2'] }));
        });

        it('omits the text filter entirely when the term is removed', () => {
            store.initialize(baseInput);
            store.patchFilters({ title: 'quarterly' });
            store.load();
            store.removeFilter('title');
            store.load();

            expect(lastRequest().filters).toBeUndefined();
        });
    });

    describe('cursor paging (US3)', () => {
        const page = (n: number, hasMore: boolean, cursor: number) =>
            of(response([item(n)], { nextContentCursor: cursor, hasMoreContent: hasMore }));

        it('starts on page 1 and asks for a zero cursor', () => {
            store.initialize(baseInput);
            store.load();

            expect(store.page().number).toBe(1);
            expect(lastRequest().contentCursor).toBe(0);
        });

        it('reaches page 2 with the bookmark page 1 handed back', () => {
            searchMock.mockReturnValue(page(1, true, 10));
            store.initialize(baseInput);
            store.load();
            store.nextPage();

            expect(store.page().number).toBe(2);
            expect(lastRequest().contentCursor).toBe(10);
        });

        /**
         * The bug this store had: it kept a single "current cursor" — the one the last response
         * handed back — so stepping back to page 2 re-sent page 3's bookmark and served the page
         * the editor had just left. Cursors are recorded per page for that reason.
         */
        it('returns to page 2 with page 1s bookmark, not page 3s', () => {
            searchMock.mockReturnValue(page(1, true, 10));
            store.initialize(baseInput);
            store.load();

            searchMock.mockReturnValue(page(2, true, 20));
            store.nextPage();

            searchMock.mockReturnValue(page(3, false, 30));
            store.nextPage();
            expect(lastRequest().contentCursor).toBe(20);

            store.setPage(2);
            store.load();
            expect(lastRequest().contentCursor).toBe(10);
        });

        /**
         * `contentCount` is the size of the page just returned, never a grand total — the drive API
         * is cursor-based. Reading it as the total makes every full page look like the last one and
         * disables "next", which is what this store did.
         */
        it('claims a page beyond the current one while more results remain', () => {
            searchMock.mockReturnValue(page(1, true, 10));
            store.initialize(baseInput);
            store.load();

            expect(store.totalItems()).toBe(ADD_RELATIONSHIPS_PAGE_SIZE * 2);
        });

        it('reports the exact total once the last page is on screen', () => {
            searchMock.mockReturnValue(of(response([item(1), item(2)], { hasMoreContent: false })));
            store.initialize(baseInput);
            store.load();

            expect(store.totalItems()).toBe(2);
        });

        /**
         * A cursor taken against a wider result set must never be applied to a narrower one, so a
         * filter change discards every bookmark rather than carrying them.
         */
        it('returns to the first page and forgets its bookmarks when a filter changes', () => {
            searchMock.mockReturnValue(page(1, true, 10));
            store.initialize(baseInput);
            store.load();
            store.nextPage();

            store.patchFilters({ title: 'narrower' });
            store.load();

            expect(store.page().number).toBe(1);
            expect(lastRequest().contentCursor).toBe(0);
        });

        it('does the same when a filter is removed, when filters are cleared, and on a re-scope', () => {
            searchMock.mockReturnValue(page(1, true, 10));
            store.initialize(baseInput);
            store.patchFilters({ title: 'something' });
            store.load();
            store.nextPage();
            store.removeFilter('title');
            expect(store.page().number).toBe(1);

            store.nextPage();
            store.clearFilters();
            expect(store.page().number).toBe(1);

            store.nextPage();
            store.setScope({ hostname: 'other.dotcms.com' });
            expect(store.page().number).toBe(1);
            expect(lastRequest().contentCursor).toBe(0);
        });
    });
});
