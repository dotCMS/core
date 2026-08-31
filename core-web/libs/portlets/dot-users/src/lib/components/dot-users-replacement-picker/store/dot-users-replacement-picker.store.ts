import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, pipe } from 'rxjs';

import { inject } from '@angular/core';

import { catchError, switchMap, tap } from 'rxjs/operators';

import { DotUserListItem, DotUsersService } from '../../../services/dot-users.service';

export interface DotUsersReplacementPickerState {
    suggestions: DotUserListItem[];
    isLoading: boolean;
    /**
     * Distinct from "no matches" so a 500 doesn't look like an empty
     * result set. Consumed by the picker's empty-template to swap the
     * copy.
     */
    hasError: boolean;
}

const initialState: DotUsersReplacementPickerState = {
    suggestions: [],
    isLoading: false,
    hasError: false
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
             * A failed fetch flips `hasError` and clears the
             * suggestion list — the empty-template then swaps to the
             * error copy. We don't route through `httpErrorManager`
             * because the picker embeds inline (delete confirm,
             * bulk-delete confirm) and a global toast on every
             * keystroke would be noisy; the field-level error is the
             * right surface here.
             */
            search: rxMethod<string>(
                pipe(
                    tap(() => patchState(store, { isLoading: true, hasError: false })),
                    switchMap((query) =>
                        usersService
                            .getUsersPaginated({ filter: query, page: 1, perPage: 10 })
                            .pipe(
                                tap((response) =>
                                    patchState(store, {
                                        suggestions: response.entity ?? [],
                                        isLoading: false
                                    })
                                ),
                                catchError(() => {
                                    patchState(store, {
                                        isLoading: false,
                                        hasError: true,
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
