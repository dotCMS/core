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
import { DOT_AI_INDEX_STATUS, DotAiIndex, DotAiIndexStatus } from '@dotcms/dotcms-models';

import { DotAiPortletState } from '../../models/dot-ai-portlet.models';
import {
    stillBuildingSeeds,
    toIndexOptions,
    toRetrievalIndexes,
    withPendingIndexes
} from '../../utils/dot-ai-index.utils';

/** Matches the legacy portlet's cadence; fast enough to feel live, slow enough to be cheap. */
const INDEX_POLL_MS = 5000;

/**
 * How long a requested build may stay missing from `indexCount` before the portlet gives up on
 * it.
 *
 * Deliberately scoped to the window *before* the index is listed at all. Once it appears, the
 * fragment-count delta owns its lifecycle and no clock is involved — a timer measured from the
 * start of the build would cut a large one off mid-flight, flipping a still-growing index to
 * Ready and stopping the poll, which is the exact failure this feature exists to prevent.
 *
 * Embedding normally writes its first rows within a second or two, so a build still unlisted
 * after two minutes failed somewhere the client cannot see.
 */
const PENDING_SEED_TTL_MS = 2 * 60 * 1000;

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
            indexOptions: computed(() => toIndexOptions(store.indexes())),

            /**
             * Build status per index, projected rather than stored.
             *
             * `applyIndexes` keeps a seed only while its index still derives as BUILDING, so
             * "seeded" and "building" are the same fact by the time anything reads this —
             * holding it as a third state field only created somewhere for the two to drift.
             */
            indexStatuses: computed<Record<string, DotAiIndexStatus>>(() => {
                const seeds = store.indexBuildSeeds();

                return Object.fromEntries(
                    store
                        .indexes()
                        .map((index) => [
                            index.name,
                            index.name in seeds
                                ? DOT_AI_INDEX_STATUS.BUILDING
                                : DOT_AI_INDEX_STATUS.READY
                        ])
                );
            })
        })),
        withMethods((store) => {
            const embeddingsService = inject(DotAiEmbeddingsService);
            const httpErrorManager = inject(DotHttpErrorManagerService);

            /**
             * The seeds still worth waiting on: listed by the server, or still inside the
             * grace period.
             *
             * Called from the failure paths as well as the success one. The poll runs while
             * any seed is held, and a `loadIndexes` that keeps failing never reaches
             * `applyIndexes` — so with the expiry enforced only there, a build started just
             * before the server went away would poll every five seconds for the life of the
             * page, raising an error dialog on each tick. Expiry is exactly the answer to
             * that, and it has to run wherever the request lands.
             */
            const liveSeeds = (listed: Set<string>): Record<string, number> => {
                const now = Date.now();

                return Object.fromEntries(
                    Object.entries(store.indexBuildSeeds()).filter(
                        ([name, requestedAt]) =>
                            listed.has(name) || now - requestedAt < PENDING_SEED_TTL_MS
                    )
                );
            };

            const applyIndexes = (indexes: DotAiIndex[]) => {
                const listed = new Set(indexes.map((index) => index.name));
                const live = liveSeeds(listed);
                const seeds = new Set(Object.keys(live));

                // The server's list plus a placeholder for each seeded build it has not caught
                // up with, so a new index is in the table from the moment it is requested.
                const merged = withPendingIndexes(indexes, seeds);

                // An index that has settled is no longer a candidate for the next poll.
                const building = stillBuildingSeeds(merged, store.indexFragmentSnapshot(), seeds);
                const stillBuilding = Object.fromEntries(
                    Object.entries(live).filter(([name]) => building.has(name))
                );

                // Retrieval targets, for the picker's fallback below. Read off `merged` rather
                // than through `toIndexOptions`, which would build and discard a label per
                // index for what is one membership test.
                const offered = toRetrievalIndexes(merged).map((index) => index.name);

                patchState(store, {
                    indexes: merged,
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
                                    // Nothing was listed, so only the grace period can keep a
                                    // seed now — which is what stops the poll rather than
                                    // letting it retry a dead endpoint forever.
                                    const surviving = liveSeeds(new Set());

                                    if (error?.status === 403) {
                                        patchState(store, {
                                            indexesForbidden: true,
                                            indexes: [],
                                            indexBuildSeeds: surviving
                                        });

                                        return EMPTY;
                                    }

                                    patchState(store, { indexBuildSeeds: surviving });
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
                /**
                 * Drops build seeds, by name or all of them.
                 *
                 * Deleting an index, or rebuilding the store, ends any build outstanding for
                 * it. Without this the seed outlives the index it was for and
                 * `withPendingIndexes` puts the deleted index straight back in the table as a
                 * zeroed BUILDING row — offered in the retrieval picker, and eligible to
                 * become `settingsIndexName`, for the rest of the grace period.
                 */
                forgetIndexBuildSeeds(indexName?: string): void {
                    // `undefined`, not falsy: '' is a real value in this module — `report`
                    // passes it as the index name for a store-wide rebuild — and it must not
                    // be mistaken for "all of them".
                    if (indexName === undefined) {
                        patchState(store, { indexBuildSeeds: {}, indexes: [] });

                        return;
                    }

                    // The row goes with the seed. `markIndexBuilding` writes both, and leaving
                    // the row for the refresh to clear left a deleted index standing — as a
                    // plausible-looking READY row with zeroes — whenever that refresh failed.
                    patchState(store, {
                        indexBuildSeeds: Object.fromEntries(
                            Object.entries(store.indexBuildSeeds()).filter(
                                ([name]) => name !== indexName
                            )
                        ),
                        indexes: store.indexes().filter((index) => index.name !== indexName)
                    });
                },

                markIndexBuilding(indexName: string): void {
                    patchState(store, {
                        indexBuildSeeds: { ...store.indexBuildSeeds(), [indexName]: Date.now() },
                        // Stand the row up now rather than waiting for the next poll — the build
                        // has been accepted, so the index exists whether or not `indexCount`
                        // knows about it yet. `withPendingIndexes` is a no-op, same array
                        // reference included, when the list already has it.
                        indexes: withPendingIndexes(store.indexes(), new Set([indexName]))
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
                store.pollIndexes(computed(() => Object.keys(store.indexBuildSeeds()).length > 0));
            }
        })
    );
}
