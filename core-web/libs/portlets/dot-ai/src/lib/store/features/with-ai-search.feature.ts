import { patchState, signalStoreFeature, type, withComputed, withMethods } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Signal } from '@angular/core';

import { catchError, switchMap, tap } from 'rxjs/operators';

import {
    DotAiIndexNotFoundError,
    DotAiSearchService,
    DotHttpErrorManagerService
} from '@dotcms/data-access';
import { ComponentStatus, DotAiRetrievalPayload } from '@dotcms/dotcms-models';

import { DotAiPortletState } from '../../models/dot-ai-portlet.models';

const isIndexNotFound = (error: unknown): error is DotAiIndexNotFoundError =>
    !!error && typeof error === 'object' && 'indexNotFound' in error;

/**
 * Semantic search.
 *
 * Consumes `retrievalPayload` from `withRetrievalSettings` rather than assembling its own
 * body, which is what keeps Search and Chat honestly identical in what they retrieve.
 */
export function withAiSearch() {
    return signalStoreFeature(
        // `props`, not `computed`: @ngrx/signals 21 renamed that key on the feature-input
        // type, and getting it wrong fails at the consumer with "Property does not exist",
        // not here — so it reads like a composition-order bug when it is not one.
        type<{
            state: DotAiPortletState;
            props: { retrievalPayload: Signal<DotAiRetrievalPayload> };
        }>(),
        withComputed((store) => ({
            searchResults: computed(() => store.searchResponse()?.results ?? []),
            isSearching: computed(() => store.searchStatus() === ComponentStatus.LOADING)
        })),
        withMethods((store) => {
            const searchService = inject(DotAiSearchService);
            const httpErrorManager = inject(DotHttpErrorManagerService);

            return {
                runSearch: rxMethod<void>(
                    pipe(
                        // switchMap: re-running cancels the in-flight search, so a slow earlier
                        // response can never overwrite a newer one.
                        switchMap(() => {
                            const prompt = store.searchPrompt().trim();

                            if (!prompt) {
                                return EMPTY;
                            }

                            patchState(store, {
                                searchStatus: ComponentStatus.LOADING,
                                searchMissingIndex: null
                            });

                            return searchService
                                .semanticSearch({ ...store.retrievalPayload(), prompt })
                                .pipe(
                                    tap((searchResponse) =>
                                        patchState(store, {
                                            searchResponse,
                                            hasSearched: true,
                                            searchStatus: ComponentStatus.LOADED
                                        })
                                    ),
                                    catchError((error: unknown) => {
                                        // A missing index is actionable and names itself, so it
                                        // is shown in place rather than as a generic failure.
                                        if (isIndexNotFound(error)) {
                                            patchState(store, {
                                                searchMissingIndex: error.indexName,
                                                searchResponse: null,
                                                hasSearched: true,
                                                searchStatus: ComponentStatus.LOADED
                                            });

                                            return EMPTY;
                                        }

                                        // Narrowed rather than cast: `as never` is
                                        // assignable to anything, so it switched off checking
                                        // on this argument entirely. Anything that is not
                                        // already an HttpErrorResponse — the index-not-found
                                        // sentinel has been handled above — is wrapped, so
                                        // the handler still has a shape to dispatch on.
                                        httpErrorManager.handle(
                                            error instanceof HttpErrorResponse
                                                ? error
                                                : new HttpErrorResponse({ error })
                                        );
                                        // LOADED, not ERROR: the screen stays usable (FR-051).
                                        patchState(store, {
                                            searchStatus: ComponentStatus.LOADED,
                                            hasSearched: true
                                        });

                                        return EMPTY;
                                    })
                                );
                        })
                    )
                )
            };
        })
    );
}
