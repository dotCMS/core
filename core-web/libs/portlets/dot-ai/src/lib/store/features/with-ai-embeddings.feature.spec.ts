import { signalStore, withState } from '@ngrx/signals';
import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { of, Subject, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';

import { DotAiEmbeddingsService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotAiEmbeddingsBuildResult, DotAiIndex } from '@dotcms/dotcms-models';

import { withAiEmbeddings } from './with-ai-embeddings.feature';
import { withAiIndexes } from './with-ai-indexes.feature';

import { DOT_AI_INITIAL_STATE, DotAiPortletState } from '../../models/dot-ai-portlet.models';

const index = (overrides: Partial<DotAiIndex> = {}): DotAiIndex => ({
    name: 'blogs',
    fragments: 10,
    contents: 4,
    tokenTotal: 1000,
    tokensPerChunk: 100,
    contentTypes: ['Blog'],
    ...overrides
});

const buildResult = (indexName = 'blogs'): DotAiEmbeddingsBuildResult => ({
    timeToEmbeddings: '2s',
    totalToEmbed: 6,
    indexName
});

const TestStore = signalStore(
    { providedIn: 'root' },
    withState<DotAiPortletState>(DOT_AI_INITIAL_STATE),
    withAiIndexes(),
    withAiEmbeddings()
);

describe('withAiEmbeddings', () => {
    let spectator: SpectatorService<InstanceType<typeof TestStore>>;
    let store: InstanceType<typeof TestStore>;
    let service: jest.Mocked<DotAiEmbeddingsService>;

    const createService = createServiceFactory({
        service: TestStore,
        providers: [mockProvider(DotAiEmbeddingsService), mockProvider(DotHttpErrorManagerService)]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
        service = spectator.inject(DotAiEmbeddingsService) as jest.Mocked<DotAiEmbeddingsService>;
        service.getIndexes = jest.fn().mockReturnValue(of([index()]));
    });

    describe('buildIndex', () => {
        it('should post an EmbeddingsForm and refresh the list (FR-033)', () => {
            service.buildIndex = jest.fn().mockReturnValue(of(buildResult()));

            store.buildIndex({ indexName: 'blogs', query: '+contentType:Blog' });

            expect(service.buildIndex).toHaveBeenCalledWith({
                indexName: 'blogs',
                query: '+contentType:Blog'
            });
            expect(service.getIndexes).toHaveBeenCalled();
        });

        it('should seed BUILDING from the index the build response names (FR-027)', () => {
            service.buildIndex = jest.fn().mockReturnValue(of(buildResult('blogs')));

            store.buildIndex({ indexName: 'blogs', query: 'q' });

            expect(store.indexStatuses()['blogs']).toBe('BUILDING');
        });

        it('should not fire twice on a double submit (exhaustMap, FR-035)', () => {
            const pending = new Subject<DotAiEmbeddingsBuildResult>();
            service.buildIndex = jest.fn().mockReturnValue(pending);

            store.buildIndex({ indexName: 'blogs', query: 'q' });
            store.buildIndex({ indexName: 'blogs', query: 'q' });

            expect(service.buildIndex).toHaveBeenCalledTimes(1);
        });

        it('should keep the list usable when a build fails (FR-051)', () => {
            const error = new HttpErrorResponse({ status: 500 });
            service.buildIndex = jest.fn().mockReturnValue(throwError(() => error));

            store.buildIndex({ indexName: 'blogs', query: 'q' });

            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(error);
            expect(store.indexesStatus()).not.toBe('ERROR');
        });
    });

    describe('deleteFromIndex', () => {
        it('should send the query as a deletion criterion, not as content to embed', () => {
            service.deleteFromIndex = jest.fn().mockReturnValue(of(3));

            store.deleteFromIndex({ indexName: 'blogs', query: '+contentType:Blog' });

            expect(service.deleteFromIndex).toHaveBeenCalledWith('blogs', '+contentType:Blog');
            expect(service.getIndexes).toHaveBeenCalled();
        });
    });

    describe('deleteIndex (FR-034)', () => {
        it('should let concurrent deletions of different indexes both complete', () => {
            const first = new Subject<number>();
            const second = new Subject<number>();
            service.deleteIndex = jest.fn().mockReturnValueOnce(first).mockReturnValueOnce(second);

            store.deleteIndex('a');
            store.deleteIndex('b');

            // mergeMap, not switchMap: deleting A must not cancel B.
            expect(service.deleteIndex).toHaveBeenCalledTimes(2);

            first.next(1);
            first.complete();
            second.next(1);
            second.complete();

            expect(service.getIndexes).toHaveBeenCalled();
        });
    });

    describe('rebuildEmbeddingsDb', () => {
        it('should rebuild and refresh', () => {
            service.rebuildEmbeddingsDb = jest.fn().mockReturnValue(of(true));

            store.rebuildEmbeddingsDb();

            expect(service.rebuildEmbeddingsDb).toHaveBeenCalled();
            expect(service.getIndexes).toHaveBeenCalled();
        });

        it('should not fire twice on a double click (exhaustMap)', () => {
            const pending = new Subject<boolean>();
            service.rebuildEmbeddingsDb = jest.fn().mockReturnValue(pending);

            store.rebuildEmbeddingsDb();
            store.rebuildEmbeddingsDb();

            expect(service.rebuildEmbeddingsDb).toHaveBeenCalledTimes(1);
        });
    });

    describe('client-side filtering (FR-028)', () => {
        beforeEach(() => {
            service.getIndexes = jest
                .fn()
                .mockReturnValue(of([index({ name: 'blogs' }), index({ name: 'news' })]));
            store.loadIndexes();
        });

        it('should filter by name without a server round trip', () => {
            store.setIndexFilter('blo');

            expect(store.filteredIndexes().map((i) => i.name)).toEqual(['blogs']);
            // Only the initial load — filtering must not re-fetch.
            expect(service.getIndexes).toHaveBeenCalledTimes(1);
        });

        it('should be case insensitive', () => {
            store.setIndexFilter('NEWS');

            expect(store.filteredIndexes().map((i) => i.name)).toEqual(['news']);
        });

        it('should filter by status', () => {
            store.markIndexBuilding('blogs');
            store.setStatusFilter('BUILDING');

            expect(store.filteredIndexes().map((i) => i.name)).toEqual(['blogs']);
        });

        it('should show everything when no filter is set', () => {
            expect(store.filteredIndexes()).toHaveLength(2);
        });
    });
});
