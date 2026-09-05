import { patchState, signalStoreFeature, type, withComputed, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Signal } from '@angular/core';

import { catchError, exhaustMap, mergeMap, tap } from 'rxjs/operators';

import { DotAiEmbeddingsService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotAiEmbeddingsBuildForm, DotAiIndex, DotAiIndexStatus } from '@dotcms/dotcms-models';

import { DotAiPortletState } from '../../models/dot-ai-portlet.models';

/**
 * Index administration: build, add to, delete from, delete, rebuild.
 *
 * Reads `loadIndexes` and `markIndexBuilding` from `withAiIndexes`, which owns the list —
 * every mutation here refreshes through that one owner, so the Embeddings table and the
 * retrieval picker update together (FR-033).
 *
 * The `rxMethod` operator per action is load-bearing:
 * - `exhaustMap` for build / rebuild, so a double click cannot double-fire (FR-035)
 * - `mergeMap` for delete, because it is per row — deleting A must not cancel B (FR-034)
 */
export function withAiEmbeddings() {
    return signalStoreFeature(
        type<{
            state: DotAiPortletState;
            props: { indexOptions: Signal<{ label: string; value: string }[]> };
            methods: {
                loadIndexes: () => void;
                markIndexBuilding: (indexName: string) => void;
            };
        }>(),
        withComputed((store) => ({
            /**
             * Both filters are pure `computed` work: `indexCount` returns every index in one
             * response with no query parameters, so there is nothing to fetch on a keystroke.
             */
            filteredIndexes: computed<DotAiIndex[]>(() => {
                const needle = store.indexFilter().trim().toLowerCase();
                const status = store.statusFilter();
                const statuses = store.indexStatuses();

                return store.indexes().filter((index) => {
                    const matchesName = !needle || index.name.toLowerCase().includes(needle);
                    const matchesStatus = !status || statuses[index.name] === status;

                    return matchesName && matchesStatus;
                });
            })
        })),
        withMethods((store) => {
            const embeddingsService = inject(DotAiEmbeddingsService);
            const httpErrorManager = inject(DotHttpErrorManagerService);

            const fail = (error: HttpErrorResponse) => {
                httpErrorManager.handle(error);

                return EMPTY;
            };

            return {
                setIndexFilter(indexFilter: string): void {
                    patchState(store, { indexFilter });
                },

                setStatusFilter(statusFilter: DotAiIndexStatus | null): void {
                    patchState(store, { statusFilter });
                },

                buildIndex: rxMethod<DotAiEmbeddingsBuildForm>(
                    pipe(
                        // exhaustMap: a double submit must not build twice.
                        exhaustMap((form) =>
                            embeddingsService.buildIndex(form).pipe(
                                tap((result) => {
                                    // The response names the index authoritatively, so the
                                    // first poll does not have to infer BUILDING from a delta
                                    // that has not appeared yet.
                                    store.markIndexBuilding(result.indexName);
                                    store.loadIndexes();
                                }),
                                catchError(fail)
                            )
                        )
                    )
                ),

                deleteFromIndex: rxMethod<{ indexName: string; query: string }>(
                    pipe(
                        exhaustMap(({ indexName, query }) =>
                            embeddingsService.deleteFromIndex(indexName, query).pipe(
                                tap(() => store.loadIndexes()),
                                catchError(fail)
                            )
                        )
                    )
                ),

                deleteIndex: rxMethod<string>(
                    pipe(
                        // mergeMap: per-row, so deleting one index cannot cancel another.
                        mergeMap((indexName) =>
                            embeddingsService.deleteIndex(indexName).pipe(
                                tap(() => store.loadIndexes()),
                                catchError(fail)
                            )
                        )
                    )
                ),

                rebuildEmbeddingsDb: rxMethod<void>(
                    pipe(
                        exhaustMap(() =>
                            embeddingsService.rebuildEmbeddingsDb().pipe(
                                tap(() => store.loadIndexes()),
                                catchError(fail)
                            )
                        )
                    )
                )
            };
        })
    );
}
