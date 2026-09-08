import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, forkJoin, Observable, of, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';

import { catchError, map, switchMap, take, tap } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService
} from '@dotcms/data-access';
import { DotCMSAPIResponse, DotMessageSeverity, DotMessageType } from '@dotcms/dotcms-models';

import {
    DotUserFormPayload,
    DotUserListItem,
    DotUsersService
} from '../../services/dot-users.service';

export type DotUsersListStatus = 'init' | 'loading' | 'loaded' | 'error';

export type DotUsersListSortDirection = 'ASC' | 'DESC';

export interface DotUsersListState {
    users: DotUserListItem[];
    selectedUsers: DotUserListItem[];
    totalRecords: number;
    page: number;
    rows: number;
    filter: string;
    /**
     * System role key that constrains the list to users holding that role.
     * Empty string means "no role filter" (the `All access` UI option).
     */
    roleFilter: string;
    sortField: string;
    sortOrder: DotUsersListSortDirection;
    status: DotUsersListStatus;
    /**
     * Role NAMES per userId for the currently displayed page. Filled
     * in one go from the inline `roles` array the list endpoint
     * returns when we ask for `includeRoles=true` (#37236 merged).
     * No per-row fan-out — a viewer whose 403-fallback strips the
     * flag just sees empty Roles cells rather than N extra requests.
     */
    userRoles: Record<string, string[]>;
}

const initialState: DotUsersListState = {
    users: [],
    selectedUsers: [],
    totalRecords: 0,
    page: 1,
    rows: 20,
    filter: '',
    roleFilter: '',
    sortField: 'lastLoginDate',
    sortOrder: 'DESC',
    status: 'init',
    userRoles: {}
};

/**
 * Snapshot of the store fields that shape the outbound list query.
 * Passed through `buildFilterParams` so the store call stays flat and
 * the transitional `includeRoles` fallback path can rebuild the same
 * params without the flag on 403.
 */
type ListQueryStoreSnapshot = {
    filter: () => string;
    roleFilter: () => string;
    page: () => number;
    rows: () => number;
    sortField: () => string;
    sortOrder: () => DotUsersListSortDirection;
};

function buildFilterParams(store: ListQueryStoreSnapshot, includeRoles = true) {
    return {
        filter: store.filter() || undefined,
        roleKey: store.roleFilter() || undefined,
        page: store.page(),
        perPage: store.rows(),
        orderBy: store.sortField(),
        direction: store.sortOrder(),
        includeRoles
    };
}

/**
 * Runs the list request with `includeRoles=true` and, on the specific
 * 403 the #37236 gate raises (viewer lacks CMS Administrator or the
 * Roles + Users portlets), retries once without the flag. Everything
 * else — 4xx on filter/paging inputs, 5xx, network errors — is
 * re-thrown for the outer `catchError` to surface via
 * `httpErrorManager`.
 */
function fetchUsersPage(
    usersService: DotUsersService,
    params: ReturnType<typeof buildFilterParams>
): Observable<DotCMSAPIResponse<DotUserListItem[]>> {
    return usersService.getUsersPaginated(params).pipe(
        catchError((error: unknown) => {
            if (params.includeRoles && error instanceof HttpErrorResponse && error.status === 403) {
                return usersService.getUsersPaginated({ ...params, includeRoles: false });
            }

            throw error;
        })
    );
}

export const DotUsersListStore = signalStore(
    withState<DotUsersListState>(initialState),
    withMethods((store) => {
        const usersService = inject(DotUsersService);
        const httpErrorManager = inject(DotHttpErrorManagerService);
        const messageDisplayService = inject(DotMessageDisplayService);
        const messageService = inject(DotMessageService);

        /**
         * `rxMethod` + `switchMap` cancels the previous in-flight request
         * when a new one is dispatched, so rapid sort/filter/page changes
         * cannot let a stale response overwrite a fresh page.
         */
        const loadUsers = rxMethod<void>(
            pipe(
                tap(() => patchState(store, { status: 'loading' })),
                switchMap(() => fetchUsersPage(usersService, buildFilterParams(store))),
                tap((response) => {
                    // Backend returns `roles: [{id, name, roleKey}]` per
                    // row when `includeRoles=true` (#37236). Build the
                    // userRoles map synchronously from the response —
                    // personal role and inherited grants are already
                    // filtered out server-side. Rows without a `roles`
                    // field (a viewer whose 403-fallback stripped the
                    // flag) leave that user's cell empty; we no longer
                    // fan out N `/v1/roles/users/{id}` requests.
                    const rolesMap: Record<string, string[]> = {};
                    for (const user of response.entity) {
                        rolesMap[user.userId] = (user.roles ?? [])
                            .map((role) => role.name)
                            .filter((name): name is string => !!name);
                    }
                    patchState(store, {
                        users: response.entity,
                        totalRecords: response.pagination?.totalEntries ?? 0,
                        status: 'loaded',
                        userRoles: rolesMap
                    });
                }),
                catchError((error) => {
                    httpErrorManager.handle(error);
                    patchState(store, { status: 'error' });

                    return EMPTY;
                })
            )
        );

        return {
            loadUsers,

            setFilter(filter: string) {
                patchState(store, { filter, page: 1 });
                loadUsers();
            },

            setRoleFilter(roleFilter: string) {
                patchState(store, { roleFilter, page: 1 });
                loadUsers();
            },

            /**
             * Atomically applies a PrimeNG `onLazyLoad` payload (page + rows +
             * sort). Batching in a single patchState + one `loadUsers()` call
             * prevents the redundant HTTP round-trip that would occur if page
             * and sort were dispatched through separate setters.
             */
            applyLazyLoad({
                page,
                rows,
                sortField,
                sortOrder
            }: {
                page: number;
                rows: number;
                sortField: string;
                sortOrder: DotUsersListSortDirection;
            }) {
                patchState(store, { page, rows, sortField, sortOrder });
                loadUsers();
            },

            setSelectedUsers(users: DotUserListItem[]) {
                patchState(store, { selectedUsers: users });
            },

            /**
             * Creates a new user and reloads the list on success. Errors
             * are surfaced through the shared HTTP error manager; the
             * list stays in `loaded` state so the user can retry from
             * the same dialog.
             *
             * `gettingStartedChange` optionally chains a toolgroup PUT
             * after user creation succeeds. A "getting started" failure
             * is soft — logged via the error manager but the user
             * creation is still considered successful (the toolgroup
             * can be toggled again from the same dialog).
             *
             * `rxMethod` + `switchMap` gives per-call teardown (so the
             * subscription can't outlive the component) and cancels a
             * previous in-flight save if the user double-clicks — the
             * new request supersedes the pending one instead of firing
             * two PUTs.
             */
            createUser: rxMethod<{
                payload: DotUserFormPayload;
                gettingStartedChange?: 'add' | 'remove';
            }>(
                pipe(
                    tap(() => patchState(store, { status: 'loading' })),
                    switchMap(({ payload, gettingStartedChange }) =>
                        usersService.createUser(payload).pipe(
                            switchMap((created) => {
                                // Create defaults to "not present", so
                                // only an explicit "add" is meaningful.
                                if (gettingStartedChange !== 'add' || !created.userId) {
                                    return of(created);
                                }

                                return usersService.setGettingStarted(created.userId, true).pipe(
                                    map(() => created),
                                    catchError((error) => {
                                        httpErrorManager.handle(error);

                                        return of(created);
                                    })
                                );
                            }),
                            tap(() => {
                                messageDisplayService.push({
                                    life: 5000,
                                    severity: DotMessageSeverity.SUCCESS,
                                    message: messageService.get('users.create.success'),
                                    type: DotMessageType.SIMPLE_MESSAGE
                                });
                                loadUsers();
                            }),
                            catchError((error) => {
                                httpErrorManager.handle(error);
                                patchState(store, { status: 'loaded' });

                                return EMPTY;
                            })
                        )
                    )
                )
            ),

            /**
             * Updates a user and reloads the list on success. Same
             * error / teardown contract as {@link createUser}.
             * `gettingStartedChange` optionally chains the toolgroup
             * PUT (add or remove).
             */
            updateUser: rxMethod<{
                payload: DotUserFormPayload;
                gettingStartedChange?: 'add' | 'remove';
            }>(
                pipe(
                    tap(() => patchState(store, { status: 'loading' })),
                    switchMap(({ payload, gettingStartedChange }) =>
                        usersService.updateUser(payload).pipe(
                            switchMap((updated) => {
                                if (!gettingStartedChange || !payload.userId) {
                                    return of(updated);
                                }

                                return usersService
                                    .setGettingStarted(
                                        payload.userId,
                                        gettingStartedChange === 'add'
                                    )
                                    .pipe(
                                        map(() => updated),
                                        catchError((error) => {
                                            httpErrorManager.handle(error);

                                            return of(updated);
                                        })
                                    );
                            }),
                            tap(() => {
                                messageDisplayService.push({
                                    life: 5000,
                                    severity: DotMessageSeverity.SUCCESS,
                                    message: messageService.get('users.update.success'),
                                    type: DotMessageType.SIMPLE_MESSAGE
                                });
                                loadUsers();
                            }),
                            catchError((error) => {
                                httpErrorManager.handle(error);
                                patchState(store, { status: 'loaded' });

                                return EMPTY;
                            })
                        )
                    )
                )
            ),

            /**
             * Single-user delete dispatched from the dialog footer.
             * Separate from `deleteSelectedUsers`, which acts on the
             * bulk-selection.
             */
            deleteSingleUser: rxMethod<{
                userId: string;
                replacementUserId?: string;
            }>(
                pipe(
                    tap(() => patchState(store, { status: 'loading' })),
                    switchMap(({ userId, replacementUserId }) =>
                        usersService.deleteUser(userId, replacementUserId).pipe(
                            tap(() => {
                                messageDisplayService.push({
                                    life: 5000,
                                    severity: DotMessageSeverity.SUCCESS,
                                    message: messageService.get('users.delete.success.one'),
                                    type: DotMessageType.SIMPLE_MESSAGE
                                });
                                loadUsers();
                            }),
                            catchError((error) => {
                                httpErrorManager.handle(error);
                                patchState(store, { status: 'loaded' });

                                return EMPTY;
                            })
                        )
                    )
                )
            ),

            deleteSelectedUsers(replacementUserId?: string) {
                const selected = store.selectedUsers();
                if (selected.length === 0 || store.status() === 'loading') {
                    return;
                }
                patchState(store, { status: 'loading' });

                const deletions = selected.map((user) =>
                    usersService.deleteUser(user.userId, replacementUserId).pipe(
                        take(1),
                        map(() => true),
                        catchError((error) => {
                            httpErrorManager.handle(error);

                            return of(false);
                        })
                    )
                );

                forkJoin(deletions)
                    .pipe(take(1))
                    .subscribe((results) => {
                        const total = results.length;
                        const successCount = results.filter(Boolean).length;
                        const failureCount = total - successCount;

                        if (successCount > 0) {
                            const successKey =
                                successCount === 1
                                    ? 'users.delete.success.one'
                                    : 'users.delete.success.many';
                            const message =
                                failureCount === 0
                                    ? messageService.get(successKey, `${successCount}`)
                                    : messageService.get(
                                          'users.delete.partial-success',
                                          `${successCount}`,
                                          `${total}`,
                                          `${failureCount}`
                                      );

                            messageDisplayService.push({
                                life: 5000,
                                severity:
                                    failureCount === 0
                                        ? DotMessageSeverity.SUCCESS
                                        : DotMessageSeverity.WARNING,
                                message,
                                type: DotMessageType.SIMPLE_MESSAGE
                            });
                        }

                        patchState(store, { selectedUsers: [] });
                        loadUsers();
                    });
            }
        };
    })
);
