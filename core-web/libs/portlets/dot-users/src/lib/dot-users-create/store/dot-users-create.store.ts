import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, forkJoin, of, pipe } from 'rxjs';

import { inject } from '@angular/core';

import { catchError, switchMap, tap } from 'rxjs/operators';

import { DotHttpErrorManagerService, DotRolesService } from '@dotcms/data-access';

import { DotUserDetail, DotUsersService } from '../../services/dot-users.service';

export type DotUsersCreateStatus = 'idle' | 'loading' | 'loaded' | 'error';

/**
 * Compact projection of a role membership we care about on save — both
 * the stable `id` (what the roles tab emits after #37218) and the
 * `roleKey` (what the Access toggles compare against, and what the
 * shell still sends for those three well-known roles). Empty `roleKey`
 * means "keyless custom role" — kept, not dropped, per #37218.
 */
export interface DotUsersCreateRole {
    id: string;
    roleKey: string;
}

export interface DotUsersCreateState {
    status: DotUsersCreateStatus;
    /** Full profile returned by getUser — additionalInfo/birthday/etc. */
    detail: DotUserDetail | null;
    /**
     * Every role currently on the user. Feeds Access-toggle hydration
     * (by `roleKey`) and the "preserve unrelated roles" merge on save
     * (by `id`). Both fields are always populated; `roleKey` may be
     * empty for user-created keyless roles — those are preserved,
     * since #37218 lets the backend resolve them by id.
     */
    roles: DotUsersCreateRole[];
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
    roles: [],
    additionalInfo: {},
    gettingStarted: false
};

/**
 * Dialog-scoped signal store for the Create/Edit User dialog. Owns
 * the three-call hydration (`getUser` + `getForUser` +
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
        const rolesService = inject(DotRolesService);
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
                            userRoles: rolesService.getForUser(userId),
                            gettingStarted: usersService
                                .getGettingStartedState(userId)
                                .pipe(catchError(() => of(false)))
                        }).pipe(
                            tap(({ user, userRoles, gettingStarted }) => {
                                // Preserve every role, keyless included —
                                // #37218 lets the backend resolve either
                                // id or key per entry, so we no longer
                                // silently strip roles whose `roleKey`
                                // is empty.
                                const roles: DotUsersCreateRole[] = userRoles.map((role) => ({
                                    id: role.id,
                                    roleKey: role.roleKey ?? ''
                                }));
                                patchState(store, {
                                    detail: user,
                                    roles,
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
