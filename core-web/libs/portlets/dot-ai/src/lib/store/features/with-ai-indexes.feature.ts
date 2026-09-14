import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withHooks,
    withMethods
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, interval, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';

import { catchError, switchMap, tap } from 'rxjs/operators';

import { DotAiEmbeddingsService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { DOT_AI_INDEX_STATUS, DotAiIndex } from '@dotcms/dotcms-models';

import { DotAiPortletState } from '../../models/dot-ai-portlet.models';
import {
    deriveIndexStatuses,
    toIndexOptions,
    withPendingIndexes
} from '../../utils/dot-ai-index.utils';

/** Matches the legacy portlet's cadence; fast enough to feel live, slow enough to be cheap. */
const INDEX_POLL_MS = 5000;

/**
 * How long a requested build may stay unaccounted for before the portlet stops waiting on it.
 *
 * A seed keeps its index BUILDING and keeps the poll running. Embedding normally writes its
 * first rows within a second or two, so anything still missing after two minutes is a build
 * that failed somewhere the client cannot see — and without a stop condition the poll would run
 * for the life of the page.
 */
const BUILD_SEED_TTL_MS = 2 * 60 * 1000;

/**
 * The embeddings index list — one owner, two readers.
 *
 * The Embeddings table and the retrieval index picker both read this, which is the reason the
 * store lives on the shell rather than per tab: one fetch, one signal, and deleting an index
 * refreshes the picker for free (FR-033).
 *
 * `GET /embeddings/indexCount` requires CMS_ADMINISTRATOR_ROLE while portlet access does not,
 * so a 403 here is a **normal state for a non-admin**, not an error. Surfacing it as a dialog
 * would interrupt someone who has done nothing wrong (FR-049, FR-050).
 */
export function withAiIndexes() {
    return signalStoreFeature(
        type<{ state: DotAiPortletState }>(),
        withComputed((store) => ({
            /** Retrieval targets only — the cache pseudo-index is excluded. */
            indexOptions: computed(() => toIndexOptions(store.indexes()))
        })),
        withMethods((store) => {
            const embeddingsService = inject(DotAiEmbeddingsService);
            const httpErrorManager = inject(DotHttpErrorManagerService);

            const applyIndexes = (indexes: DotAiIndex[]) => {
                const now = Date.now();

                // Drop seeds that have outlived their welcome before anything derives from them,
                // so an abandoned build stops both the BUILDING badge and the poll.
                const liveSeeds = Object.fromEntries(
                    Object.entries(store.indexBuildSeeds()).filter(
                        ([, startedAt]) => now - startedAt < BUILD_SEED_TTL_MS
                    )
                );

                const seeds = new Set(Object.keys(liveSeeds));

                // The server's list plus a placeholder for each seeded build it has not caught
                // up with, so a new index is in the table from the moment it is requested.
                const merged = withPendingIndexes(indexes, seeds);
                const offered = toIndexOptions(merged).map((option) => option.value);

                const statuses = deriveIndexStatuses(merged, store.indexFragmentSnapshot(), seeds);

                // An index that has settled is no longer a candidate for the next poll.
                const stillBuilding = Object.fromEntries(
                    Object.entries(liveSeeds).filter(
                        ([name]) => statuses[name] === DOT_AI_INDEX_STATUS.BUILDING
                    )
                );

                patchState(store, {
                    indexes: merged,
                    indexStatuses: statuses,
                    indexBuildSeeds: stillBuilding,
                    // Snapshotted from the server's own response, never from `merged`: a
                    // placeholder recorded at zero fragments would look like a settled index on
                    // the next poll and flip itself to READY before the build had begun.
                    indexFragmentSnapshot: indexes.reduce<Record<string, number>>(
                        (snapshot, index) => {
                            snapshot[index.name] = index.fragments;

                            return snapshot;
                        },
                        {}
                    ),
                    indexesForbidden: false,
                    // Seed the picker only when the current choice is not on offer — FR-018
                    // wants a fallback, not an overwrite. Keying this off a "seeded once" flag
                    // was wrong: the flag is not persisted, so every visit arrived unseeded and
                    // replaced the restored index with the alphabetically-first one, since
                    // `indexCount` returns a TreeMap.
                    ...(offered.length && !offered.includes(store.settingsIndexName())
                        ? { settingsIndexName: offered[0] }
                        : {})
                });
            };

            return {
                loadIndexes: rxMethod<void>(
                    pipe(
                        switchMap(() =>
                            embeddingsService.getIndexes().pipe(
                                tap(applyIndexes),
                                catchError((error: HttpErrorResponse) => {
                                    if (error?.status === 403) {
                                        patchState(store, {
                                            indexesForbidden: true,
                                            indexes: []
                                        });

                                        return EMPTY;
                                    }

                                    httpErrorManager.handle(error);

                                    return EMPTY;
                                })
                            )
                        )
                    )
                ),

                /**
                 * Marks an index as building straight away.
                 *
                 * Called with the `indexName` the build response returns — an authoritative
                 * "a build just started here", so the first poll does not have to infer it
                 * from a delta that has not appeared yet.
                 */
                markIndexBuilding(indexName: string): void {
                    const listed = store.indexes().some((index) => index.name === indexName);

                    patchState(store, {
                        indexBuildSeeds: { ...store.indexBuildSeeds(), [indexName]: Date.now() },
                        // Stand the row up now rather than waiting for the next poll — the build
                        // has been accepted, so the index exists whether or not `indexCount`
                        // knows about it yet.
                        ...(listed
                            ? {}
                            : {
                                  indexes: withPendingIndexes(store.indexes(), new Set([indexName]))
                              }),
                        indexStatuses: {
                            ...store.indexStatuses(),
                            [indexName]: DOT_AI_INDEX_STATUS.BUILDING
                        }
                    });
                }
            };
        }),
        withMethods((store) => ({
            /**
             * Re-fetches while a build is outstanding, and stops when none is.
             *
             * Without a poll the derivation never runs again: statuses come from a
             * fragment-count delta, so a BUILDING index only settles to READY if something
             * re-fetches. An idle screen must not talk to the server (FR-027).
             *
             * `switchMap` over the flag is what makes both halves automatic — going false
             * unsubscribes the interval, and `rxMethod` unsubscribes on the store's own
             * teardown. The earlier `effect` + manual `Subscription` needed a `DestroyRef`
             * alongside it, because destroying an effect does not re-run its body, so the
             * stop branch never fired on teardown and an outstanding poll ticked on for the
             * page's lifetime.
             */
            pollIndexes: rxMethod<boolean>(
                pipe(
                    switchMap((building) =>
                        building
                            ? interval(INDEX_POLL_MS).pipe(tap(() => store.loadIndexes()))
                            : EMPTY
                    )
                )
            )
        })),
        withHooks({
            onInit(store) {
                store.pollIndexes(
                    computed(() =>
                        Object.values(store.indexStatuses()).includes(DOT_AI_INDEX_STATUS.BUILDING)
                    )
                );
            }
        })
    );
}
