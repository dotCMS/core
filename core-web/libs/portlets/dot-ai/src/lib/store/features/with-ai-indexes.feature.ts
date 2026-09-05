import {
    patchState,
    signalStoreFeature,
    type,
    withComputed,
    withHooks,
    withMethods
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, interval, pipe, Subscription } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, effect, inject, untracked } from '@angular/core';

import { catchError, switchMap, tap } from 'rxjs/operators';

import { DotAiEmbeddingsService, DotHttpErrorManagerService } from '@dotcms/data-access';
import { ComponentStatus, DOT_AI_INDEX_STATUS, DotAiIndex } from '@dotcms/dotcms-models';

import { DotAiPortletState } from '../../models/dot-ai-portlet.models';
import { deriveIndexStatuses, toIndexOptions } from '../../utils/dot-ai-index.utils';

/** Matches the legacy portlet's cadence; fast enough to feel live, slow enough to be cheap. */
const INDEX_POLL_MS = 5000;

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
                const seeds = new Set(store.indexBuildSeeds());
                const statuses = deriveIndexStatuses(indexes, store.indexFragmentSnapshot(), seeds);

                // An index that has settled is no longer a candidate for the next poll.
                const stillBuilding = store
                    .indexBuildSeeds()
                    .filter((name) => statuses[name] === 'BUILDING');

                patchState(store, {
                    indexes,
                    indexStatuses: statuses,
                    indexBuildSeeds: stillBuilding,
                    indexFragmentSnapshot: indexes.reduce<Record<string, number>>(
                        (snapshot, index) => {
                            snapshot[index.name] = index.fragments;

                            return snapshot;
                        },
                        {}
                    ),
                    indexesForbidden: false,
                    indexesStatus: ComponentStatus.LOADED,
                    // Seed the picker once, so a later poll cannot yank the user's choice.
                    ...(store.settingsIndexSeeded() || !indexes.length
                        ? {}
                        : {
                              settingsIndexName:
                                  toIndexOptions(indexes)[0]?.value ?? store.settingsIndexName(),
                              settingsIndexSeeded: true
                          })
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
                                            indexes: [],
                                            indexesStatus: ComponentStatus.LOADED
                                        });

                                        return EMPTY;
                                    }

                                    httpErrorManager.handle(error);
                                    patchState(store, { indexesStatus: ComponentStatus.LOADED });

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
                    patchState(store, {
                        indexBuildSeeds: [...new Set([...store.indexBuildSeeds(), indexName])],
                        indexStatuses: { ...store.indexStatuses(), [indexName]: 'BUILDING' }
                    });
                }
            };
        }),
        withHooks({
            onInit(store) {
                // Without this the derivation never runs again: statuses are computed from a
                // fragment-count delta, so a BUILDING index only settles to READY if something
                // re-fetches. Poll only while a build is outstanding, and stop as soon as it
                // settles — an idle screen should not talk to the server (FR-027).
                let poll: Subscription | null = null;

                effect(() => {
                    const building = Object.values(store.indexStatuses()).includes(
                        DOT_AI_INDEX_STATUS.BUILDING
                    );

                    untracked(() => {
                        if (building && !poll) {
                            poll = interval(INDEX_POLL_MS).subscribe(() => store.loadIndexes());

                            return;
                        }

                        if (!building && poll) {
                            poll.unsubscribe();
                            poll = null;
                        }
                    });
                });
            },
            onDestroy() {
                // The effect's teardown owns the subscription; nothing to do beyond letting
                // the injection context dispose it.
            }
        })
    );
}
