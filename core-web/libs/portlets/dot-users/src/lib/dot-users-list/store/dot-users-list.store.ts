import { patchState, signalStore, withMethods, withState } from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, forkJoin, of, pipe } from 'rxjs';

import { inject } from '@angular/core';

import { catchError, map, switchMap, take, tap } from 'rxjs/operators';

import {
    DotHttpErrorManagerService,
    DotMessageDisplayService,
    DotMessageService
} from '@dotcms/data-access';
import { DotMessageSeverity, DotMessageType } from '@dotcms/dotcms-models';

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
    status: 'init'
};

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
                switchMap(() =>
                    usersService
                        .getUsersPaginated({
                            filter: store.filter() || undefined,
                            roleKey: store.roleFilter() || undefined,
                            page: store.page(),
                            perPage: store.rows(),
                            orderBy: store.sortField(),
                            direction: store.sortOrder()
                        })
                        .pipe(
                            // Consume the response inside the inner `.pipe()` where the
                            // Observable is strongly typed. The standalone `pipe(...)`
                            // outside can't propagate the response type through the
                            // switchMap chain under Angular's strict production build.
                            tap((response) => {
                                patchState(store, {
                                    users: response.entity,
                                    totalRecords: response.pagination?.totalEntries ?? 0,
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
             * Creates a new user and reloads the list on success. Errors are
             * surfaced through the shared HTTP error manager; the list stays
             * in `loaded` state so the user can retry from the same dialog.
             *
             * `gettingStartedChange` optionally chains a toolgroup PUT after
             * user creation succeeds. A "getting started" failure is soft —
             * we log it via the shared error manager but the user creation
             * is still considered successful (the toolgroup can be toggled
             * again from the same dialog).
             */
            createUser(payload: DotUserFormPayload, gettingStartedChange?: 'add' | 'remove') {
                patchState(store, { status: 'loading' });
                usersService
                    .createUser(payload)
                    .pipe(
                        take(1),
                        switchMap((created) => {
                            // create defaults to `not present`, so only an
                            // explicit "add" is meaningful here.
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
                        })
                    )
                    .subscribe({
                        next: () => {
                            messageDisplayService.push({
                                life: 5000,
                                severity: DotMessageSeverity.SUCCESS,
                                message: messageService.get('users.create.success'),
                                type: DotMessageType.SIMPLE_MESSAGE
                            });
                            loadUsers();
                        },
                        error: (error) => {
                            httpErrorManager.handle(error);
                            patchState(store, { status: 'loaded' });
                        }
                    });
            },

            /**
             * Updates a user and reloads the list on success. Same error
             * contract as `createUser`. `gettingStartedChange` optionally
             * chains the toolgroup PUT (add or remove).
             */
            updateUser(payload: DotUserFormPayload, gettingStartedChange?: 'add' | 'remove') {
                patchState(store, { status: 'loading' });
                usersService
                    .updateUser(payload)
                    .pipe(
                        take(1),
                        switchMap((updated) => {
                            if (!gettingStartedChange || !payload.userId) {
                                return of(updated);
                            }

                            return usersService
                                .setGettingStarted(payload.userId, gettingStartedChange === 'add')
                                .pipe(
                                    map(() => updated),
                                    catchError((error) => {
                                        httpErrorManager.handle(error);

                                        return of(updated);
                                    })
                                );
                        })
                    )
                    .subscribe({
                        next: () => {
                            messageDisplayService.push({
                                life: 5000,
                                severity: DotMessageSeverity.SUCCESS,
                                message: messageService.get('users.update.success'),
                                type: DotMessageType.SIMPLE_MESSAGE
                            });
                            loadUsers();
                        },
                        error: (error) => {
                            httpErrorManager.handle(error);
                            patchState(store, { status: 'loaded' });
                        }
                    });
            },

            /**
             * Single-user delete dispatched from the dialog footer. Separate
             * from `deleteSelectedUsers`, which acts on the bulk-selection.
             */
            deleteSingleUser(userId: string, replacementUserId?: string) {
                patchState(store, { status: 'loading' });
                usersService
                    .deleteUser(userId, replacementUserId)
                    .pipe(take(1))
                    .subscribe({
                        next: () => {
                            messageDisplayService.push({
                                life: 5000,
                                severity: DotMessageSeverity.SUCCESS,
                                message: messageService.get('users.delete.success.one'),
                                type: DotMessageType.SIMPLE_MESSAGE
                            });
                            loadUsers();
                        },
                        error: (error) => {
                            httpErrorManager.handle(error);
                            patchState(store, { status: 'loaded' });
                        }
                    });
            },

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
