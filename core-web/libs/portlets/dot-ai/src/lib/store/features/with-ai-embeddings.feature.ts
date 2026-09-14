import { patchState, signalStoreFeature, type, withComputed, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Signal } from '@angular/core';

import { catchError, concatMap, mergeMap, tap } from 'rxjs/operators';

import { DotAiEmbeddingsService } from '@dotcms/data-access';
import { DotAiEmbeddingsBuildForm, DotAiIndex } from '@dotcms/dotcms-models';

import {
    DOT_AI_INDEX_OPERATION,
    DotAiIndexNotice,
    DotAiPortletState
} from '../../models/dot-ai-portlet.models';

/**
 * Index administration: build, add to, delete from, delete, rebuild.
 *
 * Reads `loadIndexes` and `markIndexBuilding` from `withAiIndexes`, which owns the list —
 * every mutation here refreshes through that one owner, so the Embeddings table and the
 * retrieval picker update together (FR-033).
 *
 * Every operation records what it did in `indexNotice`. None of them used to: three of the
 * four returned a count the store dropped on the floor, so a destructive action confirmed and
 * then said nothing at all.
 *
 * The `rxMethod` operator per action is load-bearing:
 * - `concatMap` for build and remove — these are submitted from a dialog that can be abandoned
 *   mid-flight and reopened, so a second submit must neither be dropped (`exhaustMap`, which
 *   left the new dialog spinning on a request that was never sent) nor cancel the first
 *   (`switchMap`, which would abandon a build the server has already started). Double-submit
 *   is prevented in the dialog, which disables its own button while a request is outstanding
 *   (FR-035)
 * - `mergeMap` for `deleteIndex`, because that one is per row — deleting A must not cancel the
 *   delete of B (FR-034)
 */
export function withAiEmbeddings() {
    return signalStoreFeature(
        type<{
            state: DotAiPortletState;
            props: { indexOptions: Signal<{ label: string; value: string }[]> };
            methods: {
                loadIndexes: () => void;
                markIndexBuilding: (indexName: string) => void;
                forgetIndexBuildSeeds: (indexName?: string) => void;
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

            /** The server's own words when it has any, so the reason is not thrown away. */
            const extractReason = (error: HttpErrorResponse): string | undefined => {
                const body = error?.error;

                if (typeof body === 'string') {
                    return body;
                }

                return body?.message ?? body?.error ?? error?.message;
            };

            const notify = (notice: DotAiIndexNotice) => patchState(store, { indexNotice: notice });

            /**
             * Records a failed operation and swallows it.
             *
             * A 403 is the same normal non-admin state `loadIndexes` handles — index operations
             * require CMS_ADMINISTRATOR_ROLE while portlet access does not — so it puts the tab
             * into its forbidden state rather than throwing a dialog over someone who has done
             * nothing wrong (FR-050). Everything else is reported in the portlet's own words
             * rather than through the shared handler, which renders a malformed query as
             * "Unknown Error" with the index name lost (FR-014).
             */
            const report = (
                operation: DotAiIndexNotice['operation'],
                indexName: string,
                error: HttpErrorResponse
            ) => {
                if (error?.status === 403) {
                    patchState(store, { indexesForbidden: true });

                    return EMPTY;
                }

                notify({ operation, outcome: 'failed', indexName, detail: extractReason(error) });

                return EMPTY;
            };

            return {
                setIndexFilter(indexFilter: string): void {
                    patchState(store, { indexFilter });
                },

                buildIndex: rxMethod<DotAiEmbeddingsBuildForm>(
                    pipe(
                        tap(() => patchState(store, { indexNotice: null })),
                        concatMap((form) =>
                            embeddingsService.buildIndex(form).pipe(
                                tap((result) => {
                                    // A query that matches nothing still answers 200, and the
                                    // empty index it makes never appears in indexCount — so
                                    // saying nothing here reads as "the build did nothing".
                                    if (!result.totalToEmbed) {
                                        patchState(store, {
                                            indexNotice: {
                                                operation: DOT_AI_INDEX_OPERATION.BUILD,
                                                outcome: 'empty',
                                                indexName: result.indexName
                                            }
                                        });

                                        return;
                                    }

                                    patchState(store, {
                                        indexNotice: {
                                            operation: DOT_AI_INDEX_OPERATION.BUILD,
                                            outcome: 'ok',
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
                                catchError((error: HttpErrorResponse) =>
                                    report(DOT_AI_INDEX_OPERATION.BUILD, form.indexName, error)
                                )
                            )
                        )
                    )
                ),

                removeFromIndex: rxMethod<{ indexName: string; query: string }>(
                    pipe(
                        tap(() => patchState(store, { indexNotice: null })),
                        concatMap(({ indexName, query }) =>
                            embeddingsService.deleteFromIndex(indexName, query).pipe(
                                tap((deleted) => {
                                    // The server answers 200 with `deleted: 0` when the query
                                    // matches nothing, so silence here would read as success.
                                    notify({
                                        operation: DOT_AI_INDEX_OPERATION.REMOVE_CONTENT,
                                        outcome: deleted ? 'ok' : 'empty',
                                        indexName,
                                        detail: `${deleted}`
                                    });
                                    store.loadIndexes();
                                }),
                                catchError((error: HttpErrorResponse) =>
                                    report(DOT_AI_INDEX_OPERATION.REMOVE_CONTENT, indexName, error)
                                )
                            )
                        )
                    )
                ),

                deleteIndex: rxMethod<string>(
                    pipe(
                        // mergeMap: per-row, so deleting one index cannot cancel another.
                        mergeMap((indexName) =>
                            embeddingsService.deleteIndex(indexName).pipe(
                                tap((deleted) => {
                                    notify({
                                        operation: DOT_AI_INDEX_OPERATION.DELETE_INDEX,
                                        outcome: 'ok',
                                        indexName,
                                        detail: `${deleted}`
                                    });
                                    // The index is gone, so any build outstanding for it is
                                    // too — a surviving seed would put it straight back in
                                    // the table as a zeroed BUILDING row.
                                    store.forgetIndexBuildSeeds(indexName);
                                    store.loadIndexes();
                                }),
                                catchError((error: HttpErrorResponse) =>
                                    report(DOT_AI_INDEX_OPERATION.DELETE_INDEX, indexName, error)
                                )
                            )
                        )
                    )
                ),

                rebuildEmbeddingsDb: rxMethod<void>(
                    pipe(
                        concatMap(() =>
                            embeddingsService.rebuildEmbeddingsDb().pipe(
                                tap(() => {
                                    notify({
                                        operation: DOT_AI_INDEX_OPERATION.REBUILD_DB,
                                        outcome: 'ok',
                                        indexName: ''
                                    });
                                    // Every index went with the store, and so does every seed.
                                    store.forgetIndexBuildSeeds();
                                    store.loadIndexes();
                                }),
                                catchError((error: HttpErrorResponse) =>
                                    report(DOT_AI_INDEX_OPERATION.REBUILD_DB, '', error)
                                )
                            )
                        )
                    )
                )
            };
        })
    );
}
