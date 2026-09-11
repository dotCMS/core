import { patchState, signalStore, withState } from '@ngrx/signals';
import { createServiceFactory, mockProvider, SpectatorService } from '@openng/spectator/vitest';
import { of, throwError } from 'rxjs';
import { Mock, vi } from 'vitest';

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
        spectator.inject(DotAiEmbeddingsService).getIndexes = vi.fn().mockReturnValue(of(indexes));
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
            spectator.inject(DotAiEmbeddingsService).getIndexes = vi
                .fn()
                .mockReturnValue(throwError(() => new HttpErrorResponse({ status: 403 })));

            store.loadIndexes();

            expect(store.indexesForbidden()).toBe(true);
            expect(spectator.inject(DotHttpErrorManagerService).handle).not.toHaveBeenCalled();
        });

        it('should still route other failures through the error manager', () => {
            const error = new HttpErrorResponse({ status: 500 });
            spectator.inject(DotAiEmbeddingsService).getIndexes = vi
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

    describe('index seeding (FR-018)', () => {
        it('should keep a restored index that is still offered', () => {
            // The old "seed once" flag was never persisted, so every visit arrived unseeded and
            // replaced the restored choice with the alphabetically-first index.
            patchState(store, { settingsIndexName: 'news' });

            stubIndexes([index({ name: 'blogs' }), index({ name: 'news' })]);
            store.loadIndexes();

            expect(store.settingsIndexName()).toBe('news');
        });

        it('should fall back to the first offered index when the choice is gone', () => {
            patchState(store, { settingsIndexName: 'retired' });

            stubIndexes([index({ name: 'blogs' }), index({ name: 'news' })]);
            store.loadIndexes();

            expect(store.settingsIndexName()).toBe('blogs');
        });
    });

    describe('polling while a build is outstanding (FR-027)', () => {
        beforeEach(() => vi.useFakeTimers());
        afterEach(() => vi.useRealTimers());

        it('should not talk to the server while nothing is building', () => {
            stubIndexes([index({ name: 'blogs' })]);
            store.loadIndexes();
            spectator.flushEffects();

            vi.advanceTimersByTime(20000);

            // Only the explicit load — an idle screen must stay quiet.
            expect(spectator.inject(DotAiEmbeddingsService).getIndexes).toHaveBeenCalledTimes(1);
        });

        it('should re-fetch until the build settles, then stop', () => {
            stubIndexes([index({ name: 'blogs', fragments: 10 })]);
            store.loadIndexes();
            store.markIndexBuilding('blogs');
            spectator.flushEffects();

            // Count still moving, so still building.
            stubIndexes([index({ name: 'blogs', fragments: 20 })]);
            vi.advanceTimersByTime(5000);
            expect(store.indexStatuses()['blogs']).toBe(DOT_AI_INDEX_STATUS.BUILDING);

            // Unchanged: the build has finished and the poll must unsubscribe itself.
            stubIndexes([index({ name: 'blogs', fragments: 20 })]);
            vi.advanceTimersByTime(5000);
            expect(store.indexStatuses()['blogs']).toBe(DOT_AI_INDEX_STATUS.READY);
            spectator.flushEffects();

            const calls = (spectator.inject(DotAiEmbeddingsService).getIndexes as Mock).mock.calls
                .length;
            vi.advanceTimersByTime(20000);

            expect(
                (spectator.inject(DotAiEmbeddingsService).getIndexes as Mock).mock.calls.length
            ).toBe(calls);
        });
    });
});
