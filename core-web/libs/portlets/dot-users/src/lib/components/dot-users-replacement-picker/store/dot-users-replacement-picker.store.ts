import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, pipe } from 'rxjs';

import { inject } from '@angular/core';

import { catchError, switchMap, tap } from 'rxjs/operators';

import { ComponentStatus } from '@dotcms/dotcms-models';

import { DotUserListItem, DotUsersService } from '../../../services/dot-users.service';

export interface DotUsersReplacementPickerState {
    suggestions: DotUserListItem[];
    /**
     * Single status field instead of separate `isLoading` / `hasError`
     * booleans — the standard `ComponentStatus` shape used across the
     * dotCMS portlets. Avoids the loading anti-pattern where mutually
     * exclusive states can drift out of sync.
     *
     * States we hit:
     *   IDLE    → picker just mounted, or a search resolved to no
     *             results (no error, nothing loading).
     *   LOADING → a request is in flight.
     *   LOADED  → the last request returned successfully; suggestions
     *             may be a non-empty list or an empty one (no matches).
     *   ERROR   → the last request failed; the empty-template swaps
     *             to the error copy.
     */
    status: ComponentStatus;
}

const initialState: DotUsersReplacementPickerState = {
    suggestions: [],
    status: ComponentStatus.IDLE
};

/**
 * Component-scoped signal store for the replacement-user picker.
 * Owns the debounced search HTTP so the component stays about
 * inputs / outputs / rendering — mirroring the "component MUST NOT
 * call HTTP directly" rule the users portlet review surfaced.
 *
 * NOT provided at root: the picker declares it in its own
 * `providers[]`, so every picker instance gets its own store and the
 * `switchMap` cancellation is scoped to the picker's lifecycle.
 */
export const DotUsersReplacementPickerStore = signalStore(
    withState<DotUsersReplacementPickerState>(initialState),
    withMethods((store) => {
        const usersService = inject(DotUsersService);

        return {
            /**
             * `switchMap` cancels an in-flight request the moment a
             * newer query arrives. PrimeNG's 300ms delay narrows the
             * race but a slow early response can still overwrite a
             * newer one without cancellation. `rxMethod` manages the
             * subscription lifetime automatically (no `take(1)` here,
             * per the portlet guide).
             *
             * A failed fetch flips `status` to `ERROR` and clears the
             * suggestion list — the empty-template then swaps to the
             * error copy. We don't route through `httpErrorManager`
             * because the picker embeds inline (delete confirm,
             * bulk-delete confirm) and a global toast on every
             * keystroke would be noisy; the field-level error is the
             * right surface here.
             */
            search: rxMethod<string>(
                pipe(
                    tap(() => patchState(store, { status: ComponentStatus.LOADING })),
                    switchMap((query) =>
                        usersService
                            .getUsersPaginated({ filter: query, page: 1, perPage: 10 })
                            .pipe(
                                tap((response) =>
                                    patchState(store, {
                                        suggestions: response.entity ?? [],
                                        status: ComponentStatus.LOADED
                                    })
                                ),
                                catchError(() => {
                                    patchState(store, {
                                        status: ComponentStatus.ERROR,
                                        suggestions: []
                                    });

                                    return EMPTY;
                                })
                            )
                    )
                )
            )
        };
    })
);
