import { patchState, signalStoreFeature, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, pipe } from 'rxjs';

import { inject } from '@angular/core';

import { catchError, filter, switchMap, tap } from 'rxjs/operators';

import { DotContentTypeService } from '@dotcms/data-access';
import { DotCMSContentType } from '@dotcms/dotcms-models';

interface WithContentTypeCacheState {
    contentTypeCache: Record<string, DotCMSContentType>;
}

/**
 * Caches content types fetched on demand by variable name.
 * Prevents duplicate requests — already-cached types are skipped entirely.
 * Cache persists for the lifetime of the store (page session).
 */
export function withContentTypeCache() {
    return signalStoreFeature(
        withState<WithContentTypeCacheState>({ contentTypeCache: {} }),
        withMethods((store) => {
            const dotContentTypeService = inject(DotContentTypeService);

            return {
                /**
                 * Fetches a content type by variable name and stores it in the cache.
                 * No-ops silently if the variable is already cached.
                 */
                loadContentType: rxMethod<string>(
                    pipe(
                        filter((variable) => !!variable && !store.contentTypeCache()[variable]),
                        switchMap((variable) =>
                            dotContentTypeService.getContentType(variable).pipe(
                                tap((contentType) => {
                                    patchState(store, (state) => ({
                                        contentTypeCache: {
                                            ...state.contentTypeCache,
                                            [variable]: contentType
                                        }
                                    }));
                                }),
                                catchError((error) => {
                                    console.error(
                                        `[withContentTypeCache] Failed to load content type '${variable}'`,
                                        error
                                    );

                                    return EMPTY;
                                })
                            )
                        )
                    )
                )
            };
        })
    );
}
