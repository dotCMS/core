import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, forkJoin, of, pipe } from 'rxjs';

import { inject } from '@angular/core';

import { catchError, switchMap, tap } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';

import { DotUserDetail, DotUsersService } from '../../services/dot-users.service';

export type DotUsersCreateStatus = 'idle' | 'loading' | 'loaded' | 'error';

export interface DotUsersCreateState {
    status: DotUsersCreateStatus;
    /** Full profile returned by getUser — additionalInfo/birthday/etc. */
    detail: DotUserDetail | null;
    /**
     * Every role KEY currently on the user. Feeds Access-toggle
     * hydration and the "preserve unrelated roles" merge on save.
     */
    roleKeys: string[];
    /**
     * Full additionalInfo map returned by getUser. Persisted here so
     * the shell can spread unmanaged keys through on save — the
     * backend replaces this map wholesale (see UserResource#save).
     */
    additionalInfo: Record<string, unknown>;
    /** Snapshot of the toolgroup toggle at load time. */
    gettingStarted: boolean;
}

const initialState: DotUsersCreateState = {
    status: 'idle',
    detail: null,
    roleKeys: [],
    additionalInfo: {},
    gettingStarted: false
};

/**
 * Dialog-scoped signal store for the Create/Edit User dialog. Owns
 * the three-call hydration (`getUser` + `getUserRoles` +
 * `getGettingStartedState`) so the shell stays about form + UX and
 * the HTTP lives in one place — mirroring the pattern the list
 * component already uses.
 *
 * NOT provided at root: the shell provides this on itself so every
 * dialog instance gets its own store, mirroring the modal lifecycle.
 */
export const DotUsersCreateStore = signalStore(
    withState<DotUsersCreateState>(initialState),
    withMethods((store) => {
        const usersService = inject(DotUsersService);
        const httpErrorManager = inject(DotHttpErrorManagerService);

        return {
            /**
             * Runs the three parallel calls, transitions status to
             * `loaded` on success or `error` on failure. `switchMap`
             * gives cancellation on rapid re-dispatch and per-call
             * teardown scoped to the store's lifecycle.
             *
             * `getGettingStartedState` is caught inline because it's
             * a non-critical side surface — a failure defaults to
             * `false` and doesn't fail the whole hydration.
             */
            loadUserDetail: rxMethod<string>(
                pipe(
                    tap(() => patchState(store, { status: 'loading' })),
                    switchMap((userId) =>
                        forkJoin({
                            user: usersService.getUser(userId),
                            userRoles: usersService.getUserRoles(userId),
                            gettingStarted: usersService
                                .getGettingStartedState(userId)
                                .pipe(catchError(() => of(false)))
                        }).pipe(
                            tap(({ user, userRoles, gettingStarted }) => {
                                const roleKeys = userRoles
                                    .map((role) => role.roleKey)
                                    .filter((key): key is string => !!key);
                                patchState(store, {
                                    detail: user,
                                    roleKeys,
                                    additionalInfo: user.additionalInfo ?? {},
                                    gettingStarted,
                                    status: 'loaded'
                                });
                            }),
                            catchError((error) => {
                                httpErrorManager.handle(error);
                                patchState(store, { status: 'error' });

                                return EMPTY;
                            })
                        )
                    )
                )
            )
        };
    })
);
