import { signalStore, withState } from '@ngrx/signals';
import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { of, Subject, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';

import { DotAiSearchService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { ComponentStatus, DotAiSearchResponse } from '@dotcms/dotcms-models';

import { withAiSearch } from './with-ai-search.feature';
import { withRetrievalSettings } from './with-retrieval-settings.feature';

import { DOT_AI_INITIAL_STATE, DotAiPortletState } from '../../models/dot-ai-portlet.models';

const response = (overrides: Partial<DotAiSearchResponse> = {}): DotAiSearchResponse => ({
    timeToEmbeddings: '120ms',
    total: 1,
    count: 1,
    query: 'q',
    threshold: 0.25,
    operator: '<=>',
    offset: 0,
    limit: 50,
    results: [
        {
            identifier: 'id-1',
            inode: 'inode-1',
            title: 'A result',
            contentType: 'Blog',
            modDate: '2026-01-01',
            matches: [{ distance: 0.2, extractedText: 'text' }]
        }
    ],
    ...overrides
});

const TestStore = signalStore(
    { providedIn: 'root' },
    withState<DotAiPortletState>(DOT_AI_INITIAL_STATE),
    withRetrievalSettings(),
    withAiSearch()
);

describe('withAiSearch', () => {
    let spectator: SpectatorService<InstanceType<typeof TestStore>>;
    let store: InstanceType<typeof TestStore>;

    const createService = createServiceFactory({
        service: TestStore,
        providers: [mockProvider(DotAiSearchService), mockProvider(DotHttpErrorManagerService)]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    it('should store results and mark the search loaded', () => {
        spectator.inject(DotAiSearchService).semanticSearch = jest
            .fn()
            .mockReturnValue(of(response()));

        store.setSearchPrompt('q');
        store.runSearch();

        expect(store.searchResponse()?.count).toBe(1);
        expect(store.searchStatus()).toBe(ComponentStatus.LOADED);
    });

    it('should send the shared retrieval payload plus the prompt', () => {
        const service = spectator.inject(DotAiSearchService);
        service.semanticSearch = jest.fn().mockReturnValue(of(response()));

        store.setSettings({ settingsIndexName: 'blogs' });
        store.setSearchPrompt('what is dotCMS');
        store.runSearch();

        expect(service.semanticSearch).toHaveBeenCalledWith(
            expect.objectContaining({ prompt: 'what is dotCMS', indexName: 'blogs' })
        );
    });

    it('should not search on an empty prompt', () => {
        const service = spectator.inject(DotAiSearchService);
        service.semanticSearch = jest.fn().mockReturnValue(of(response()));

        store.setSearchPrompt('   ');
        store.runSearch();

        expect(service.semanticSearch).not.toHaveBeenCalled();
    });

    it('should cancel an in-flight search when a new one starts (switchMap)', () => {
        const first = new Subject<DotAiSearchResponse>();
        const second = new Subject<DotAiSearchResponse>();
        const service = spectator.inject(DotAiSearchService);
        service.semanticSearch = jest.fn().mockReturnValueOnce(first).mockReturnValueOnce(second);

        store.setSearchPrompt('one');
        store.runSearch();
        store.setSearchPrompt('two');
        store.runSearch();

        // The abandoned first response must not win a race against the second.
        first.next(response({ query: 'one' }));
        second.next(response({ query: 'two' }));

        expect(store.searchResponse()?.query).toBe('two');
    });

    it('should keep the screen usable when a search fails (FR-051)', () => {
        const error = new HttpErrorResponse({ status: 500 });
        spectator.inject(DotAiSearchService).semanticSearch = jest
            .fn()
            .mockReturnValue(throwError(() => error));

        store.setSearchPrompt('q');
        store.runSearch();

        expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(error);
        expect(store.searchStatus()).toBe(ComponentStatus.LOADED);
    });

    it('should report a missing index by name rather than generically', () => {
        spectator.inject(DotAiSearchService).semanticSearch = jest
            .fn()
            .mockReturnValue(
                throwError(() => ({ indexNotFound: true, indexName: 'gone', original: null }))
            );

        store.setSearchPrompt('q');
        store.runSearch();

        expect(store.searchMissingIndex()).toBe('gone');
        expect(spectator.inject(DotHttpErrorManagerService).handle).not.toHaveBeenCalled();
    });

    it('should expose a distinct empty state when nothing passed the threshold', () => {
        spectator.inject(DotAiSearchService).semanticSearch = jest
            .fn()
            .mockReturnValue(of(response({ count: 0, total: 0, results: [] })));

        store.setSearchPrompt('q');
        store.runSearch();

        expect(store.hasSearched()).toBe(true);
        expect(store.searchResults()).toEqual([]);
    });
});
