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
                            catchError((error) => {
                                httpErrorManager.handle(error);
                                patchState(store, { status: 'error' });

                                return EMPTY;
                            })
                        )
                ),
                tap((response) => {
                    patchState(store, {
                        users: response.entity,
                        totalRecords: response.pagination?.totalEntries ?? 0,
                        status: 'loaded'
                    });
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

            setPagination(page: number, rows: number) {
                patchState(store, { page, rows });
                loadUsers();
            },

            setSort(field: string, order: DotUsersListSortDirection) {
                patchState(store, { sortField: field, sortOrder: order });
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
