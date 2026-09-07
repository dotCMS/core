import { patchState, signalStoreFeature, type, withComputed, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Signal } from '@angular/core';

import { catchError, exhaustMap, mergeMap, tap } from 'rxjs/operators';

import { DotAiEmbeddingsService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DotAiEmbeddingsBuildForm, DotAiIndex } from '@dotcms/dotcms-models';

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
             * Pure `computed` work: `indexCount` returns every index in one response with no
             * query parameters, so there is nothing to fetch on a keystroke.
             */
            filteredIndexes: computed<DotAiIndex[]>(() => {
                const needle = store.indexFilter().trim().toLowerCase();

                if (!needle) {
                    return store.indexes();
                }

                return store.indexes().filter((index) => index.name.toLowerCase().includes(needle));
            })
        })),
        withMethods((store) => {
            const embeddingsService = inject(DotAiEmbeddingsService);
            const httpErrorManager = inject(DotHttpErrorManagerService);

            /** The server's own words when it has any, so the reason is not thrown away. */
            const extractReason = (error: HttpErrorResponse): string | undefined => {
                const body = error?.error;

                if (typeof body === 'string') {
                    return body;
                }

                return body?.message ?? body?.error ?? error?.message;
            };

            const fail = (error: HttpErrorResponse) => {
                httpErrorManager.handle(error);

                return EMPTY;
            };

            return {
                setIndexFilter(indexFilter: string): void {
                    patchState(store, { indexFilter });
                },

                dismissBuildNotice(): void {
                    patchState(store, { indexBuildNotice: null });
                },

                buildIndex: rxMethod<DotAiEmbeddingsBuildForm>(
                    pipe(
                        tap(() => patchState(store, { indexBuildNotice: null })),
                        // exhaustMap: a double submit must not build twice.
                        exhaustMap((form) =>
                            embeddingsService.buildIndex(form).pipe(
                                tap((result) => {
                                    // A query that matches nothing still answers 200, and the
                                    // empty index it makes never appears in indexCount — so
                                    // saying nothing here reads as "the build did nothing".
                                    if (!result.totalToEmbed) {
                                        patchState(store, {
                                            indexBuildNotice: {
                                                kind: 'empty',
                                                indexName: result.indexName
                                            }
                                        });

                                        return;
                                    }

                                    patchState(store, {
                                        indexBuildNotice: {
                                            kind: 'built',
                                            indexName: result.indexName,
                                            detail: `${result.totalToEmbed}`
                                        }
                                    });

                                    // The response names the index authoritatively, so the
                                    // first poll does not have to infer BUILDING from a delta
                                    // that has not appeared yet.
                                    store.markIndexBuilding(result.indexName);
                                    store.loadIndexes();
                                }),
                                // Inline rather than through DotHttpErrorManagerService: a build
                                // failure is nearly always a malformed query, and the shared
                                // handler renders it as "Unknown Error" over a raw Java message
                                // with the index name lost. Same reasoning as the chat stream
                                // (FR-014).
                                catchError((error: HttpErrorResponse) => {
                                    patchState(store, {
                                        indexBuildNotice: {
                                            kind: 'failed',
                                            indexName: form.indexName,
                                            detail: extractReason(error)
                                        }
                                    });

                                    return EMPTY;
                                })
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
