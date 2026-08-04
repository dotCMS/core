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

import { DotUserListItem, DotUsersService } from '../../services/dot-users.service';

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

            deleteSelectedUsers() {
                const selected = store.selectedUsers();
                if (selected.length === 0 || store.status() === 'loading') {
                    return;
                }
                patchState(store, { status: 'loading' });

                const deletions = selected.map((user) =>
                    usersService.deleteUser(user.userId).pipe(
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
                            const message =
                                failureCount === 0
                                    ? messageService.get('users.delete.success', `${successCount}`)
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
