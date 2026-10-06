import { patchState, unprotected } from '@ngrx/signals';
import { SpectatorService, createServiceFactory, mockProvider } from '@openng/spectator/vitest';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { DotContentDriveService } from '@dotcms/data-access';
import { LoggerService, SiteService } from '@dotcms/dotcms-js';
import { DotFilterFacade } from '@dotcms/ui';
import { testFilterFacadeConformance } from '@dotcms/ui/testing';

import { createAddRelationshipsFilterFacade } from './add-relationships-filter-facade';
import { AddRelationshipsStore } from './add-relationships.store';

import { AddRelationshipsInput } from '../models/add-relationships.models';

/**
 * The shared conformance suite (obligations O1–O9), run against this dialog's facade.
 *
 * Imported from `@dotcms/ui/testing` rather than `@dotcms/ui`: it calls Jest globals, and
 * re-exporting it from the production barrel drags those into every consumer's typecheck.
 */
describe('AddRelationshipsFilterFacade', () => {
    let spectator: SpectatorService<InstanceType<typeof AddRelationshipsStore>>;

    /** Extracted from the provider so the reload assertions below can read its calls. */
    const searchMock = vi.fn().mockReturnValue(
        of({
            list: [],
            contentCount: 0,
            folderCount: 0,
            hasMoreContent: false,
            hasMoreFolders: false,
            nextContentCursor: 0,
            nextFolderCursor: 0
        })
    );

    const createService = createServiceFactory({
        service: AddRelationshipsStore,
        providers: [
            mockProvider(DotContentDriveService, { search: searchMock }),
            mockProvider(SiteService, { currentSite: { hostname: 'demo.dotcms.com' } }),
            mockProvider(LoggerService)
        ]
    });

    const input: AddRelationshipsInput = {
        contentTypeId: 'target-type',
        selected: [],
        selectionMode: 'multiple',
        contentletContext: { languageId: 1 }
    };

    testFilterFacadeConformance(
        'AddRelationships',
        () => {
            spectator = createService();
            const store = spectator.service;
            store.initialize(input);

            return {
                facade: createAddRelationshipsFilterFacade(store),
                readRawBag: () => store.filters() as Record<string, unknown>,
                readPage: () => store.page().number,
                goToPage2: () => store.setPage(2),
                // What the caller seeded: the contentlet's locale, and nothing else. Clearing
                // returns here rather than to `{}` — an empty set would strand the editor in an
                // unfiltered library.
                expectedDefaults: { languageId: ['1'] },
                // This dialog has no URL, so its bag already holds the vocabulary chips speak and
                // its normalization is the identity. The same assertion passes on Content Drive
                // for the opposite reason — it stores numbers — which is the point of one suite.
                encodedFilter: { key: 'languageId', value: ['2'] },
                writeRaw: (key: string, value: unknown) =>
                    patchState(unprotected(store), {
                        filters: { ...store.filters(), [key]: value as string | string[] }
                    }),
                /**
                 * O8. The relationship's target content type is a caller restriction, not a
                 * filter: it lives on the store's own `contentTypeId` and there is no code path
                 * from the facade to it. That is what makes "the editor cannot widen past the
                 * type this field accepts" structural rather than a convention.
                 */
                restrictedKeys: ['contentTypes', 'contentTypeId']
            };
        },
        { normalizes: false }
    );

    /**
     * A chip that changes the bag has to change the results.
     *
     * Reported in review: the Locale chip wrote through the facade, the store recorded the filter
     * and reset paging — and nothing reloaded. The list kept showing the previous locale until some
     * other control happened to fire a search. `clearFilters` already reloaded (it goes through
     * `reset`), so the seam was answering two of its own methods differently.
     */
    describe('a filter change reloads the results', () => {
        let store: InstanceType<typeof AddRelationshipsStore>;
        let facade: DotFilterFacade;

        beforeEach(() => {
            spectator = createService();
            store = spectator.service;
            store.initialize(input);
            facade = createAddRelationshipsFilterFacade(store);
            store.load();
            searchMock.mockClear();
        });

        it('searches again when a chip sets a filter', () => {
            facade.patchFilters({ languageId: ['2'] });

            expect(searchMock).toHaveBeenCalledTimes(1);
            expect(searchMock.mock.calls[0][0].language).toEqual(['2']);
        });

        it('searches again when a chip clears its filter', () => {
            facade.patchFilters({ languageId: ['2'] });
            searchMock.mockClear();

            facade.removeFilter('languageId');

            expect(searchMock).toHaveBeenCalledTimes(1);
            expect(searchMock.mock.calls[0][0].language).toBeUndefined();
        });

        it('does not search for a patch that changes nothing (O9 still holds)', () => {
            facade.patchFilters({ languageId: ['1'] });

            expect(searchMock).not.toHaveBeenCalled();
        });
    });

    /**
     * Beyond the shared obligations: on this surface the browsed site counts as something to
     * clear.
     *
     * Reported from the running app — changing site or folder left "Clear all" hidden, so an
     * editor who browsed into an empty site was told to try another one with no control to do it
     * with. The site is not in the filter bag (it is scope, the way Content Drive's browsed folder
     * is), which is why the shared computed alone cannot see it.
     */
    describe('scope counts as something to clear', () => {
        let store: InstanceType<typeof AddRelationshipsStore>;
        let facade: DotFilterFacade;

        beforeEach(() => {
            spectator = createService();
            store = spectator.service;
            store.initialize(input);
            facade = createAddRelationshipsFilterFacade(store);
        });

        it('offers nothing to clear on the site the dialog opened on', () => {
            expect(facade.$hasNonDefaultFilters()).toBe(false);
        });

        it('offers a clear once the editor browses to another site', () => {
            store.setScope({ hostname: 'other.dotcms.com' });

            expect(facade.$hasNonDefaultFilters()).toBe(true);
        });

        it('offers a clear once the editor browses into a folder', () => {
            store.setScope({ hostname: 'demo.dotcms.com', path: '/blog/' });

            expect(facade.$hasNonDefaultFilters()).toBe(true);
        });

        it('puts the site back, not only the filters — O5 must still hold', () => {
            store.patchFilters({ title: 'something' });
            store.setScope({ hostname: 'other.dotcms.com', path: '/blog/' });

            facade.clearFilters();

            expect(store.scopeLabel()).toBe('demo.dotcms.com');
            expect(facade.getFilterValue('title')).toBeUndefined();
            expect(facade.$hasNonDefaultFilters()).toBe(false);
        });
    });
});
