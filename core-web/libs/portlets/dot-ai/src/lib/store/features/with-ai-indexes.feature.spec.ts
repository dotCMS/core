import { signalStore, withState } from '@ngrx/signals';
import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/jest';
import { of, throwError } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';

import { DotAiEmbeddingsService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DOT_AI_INDEX_STATUS, DotAiIndex } from '@dotcms/dotcms-models';

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

const TestStore = signalStore(
    { providedIn: 'root' },
    withState<DotAiPortletState>(DOT_AI_INITIAL_STATE),
    withAiIndexes()
);

describe('withAiIndexes', () => {
    let spectator: SpectatorService<InstanceType<typeof TestStore>>;
    let store: InstanceType<typeof TestStore>;

    const createService = createServiceFactory({
        service: TestStore,
        providers: [mockProvider(DotAiEmbeddingsService), mockProvider(DotHttpErrorManagerService)]
    });

    beforeEach(() => {
        spectator = createService();
        store = spectator.service;
    });

    const stubIndexes = (indexes: DotAiIndex[]) => {
        spectator.inject(DotAiEmbeddingsService).getIndexes = jest
            .fn()
            .mockReturnValue(of(indexes));
    };

    it('should load the indexes', () => {
        stubIndexes([index()]);

        store.loadIndexes();

        expect(store.indexes()).toHaveLength(1);
    });

    it('should exclude the cache pseudo-index from the retrieval picker', () => {
        stubIndexes([index({ name: 'cache' }), index({ name: 'blogs', contents: 7 })]);

        store.loadIndexes();

        // It stays in `indexes` for the Embeddings table, but is not a retrieval target.
        expect(store.indexes().map((i) => i.name)).toEqual(['cache', 'blogs']);
        expect(store.indexOptions()).toEqual([{ label: 'blogs - (contents:7)', value: 'blogs' }]);
    });

    it('should seed the settings index once, from the loaded list', () => {
        stubIndexes([index({ name: 'blogs' })]);

        store.loadIndexes();

        expect(store.settingsIndexName()).toBe('blogs');
    });

    describe('403 (FR-049, FR-050)', () => {
        it('should enter a forbidden state rather than surfacing an error dialog', () => {
            spectator.inject(DotAiEmbeddingsService).getIndexes = jest
                .fn()
                .mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 })));

            store.loadIndexes();

            expect(store.indexesForbidden()).toBe(true);
            expect(spectator.inject(DotHttpErrorManagerService).handle).not.toHaveBeenCalled();
        });

        it('should still route other failures through the error manager', () => {
            const error = new HttpErrorResponse({ status: 500 });
            spectator.inject(DotAiEmbeddingsService).getIndexes = jest
                .fn()
                .mockReturnValue(throwError(() => error));

            store.loadIndexes();

            expect(store.indexesForbidden()).toBe(false);
            expect(spectator.inject(DotHttpErrorManagerService).handle).toHaveBeenCalledWith(error);
        });
    });

    describe('per-index build status (FR-027)', () => {
        it('should mark an index BUILDING as soon as a build is seeded for it', () => {
            stubIndexes([index({ name: 'blogs' }), index({ name: 'other' })]);
            store.loadIndexes();

            store.markIndexBuilding('blogs');

            expect(store.indexStatuses()['blogs']).toBe(DOT_AI_INDEX_STATUS.BUILDING);
            expect(store.indexStatuses()['other']).toBe(DOT_AI_INDEX_STATUS.READY);
        });

        it('should settle to READY once the fragment count stops moving', () => {
            stubIndexes([index({ name: 'blogs', fragments: 10 })]);
            store.loadIndexes();
            store.markIndexBuilding('blogs');

            // Next poll: count moved, so still building.
            stubIndexes([index({ name: 'blogs', fragments: 20 })]);
            store.loadIndexes();
            expect(store.indexStatuses()['blogs']).toBe(DOT_AI_INDEX_STATUS.BUILDING);

            // Poll again: unchanged, so the build has finished.
            stubIndexes([index({ name: 'blogs', fragments: 20 })]);
            store.loadIndexes();
            expect(store.indexStatuses()['blogs']).toBe(DOT_AI_INDEX_STATUS.READY);
        });
    });
});
