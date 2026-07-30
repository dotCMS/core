import { patchState, signalStore, withHooks, withMethods, withState } from '@ngrx/signals';
import { EMPTY } from 'rxjs';

import { effect, inject, untracked } from '@angular/core';

import { catchError, take } from 'rxjs/operators';

import { DotHttpErrorManagerService } from '@dotcms/data-access';

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
    sortField: 'lastLoginDate',
    sortOrder: 'DESC',
    status: 'init'
};

export const DotUsersListStore = signalStore(
    withState<DotUsersListState>(initialState),
    withMethods((store) => {
        const usersService = inject(DotUsersService);
        const httpErrorManager = inject(DotHttpErrorManagerService);

        function loadUsers() {
            patchState(store, { status: 'loading' });
            usersService
                .getUsersPaginated({
                    filter: store.filter() || undefined,
                    page: store.page(),
                    perPage: store.rows(),
                    orderBy: store.sortField(),
                    direction: store.sortOrder()
                })
                .pipe(
                    take(1),
                    catchError((error) => {
                        httpErrorManager.handle(error);
                        patchState(store, { status: 'error' });

                        return EMPTY;
                    })
                )
                .subscribe((response) => {
                    patchState(store, {
                        users: response.entity,
                        totalRecords: response.pagination?.totalEntries ?? 0,
                        status: 'loaded'
                    });
                });
        }

        return {
            loadUsers,

            setFilter(filter: string) {
                patchState(store, { filter, page: 1 });
            },

            setPagination(page: number, rows: number) {
                patchState(store, { page, rows });
            },

            setSort(field: string, order: DotUsersListSortDirection) {
                patchState(store, { sortField: field, sortOrder: order });
            },

            setSelectedUsers(users: DotUserListItem[]) {
                patchState(store, { selectedUsers: users });
            },

            deleteSelectedUsers() {
                const selected = store.selectedUsers();
                if (selected.length === 0) {
                    return;
                }
                patchState(store, { status: 'loading' });

                const deletions = selected.map((user) =>
                    usersService.deleteUser(user.userId).pipe(
                        take(1),
                        catchError((error) => {
                            httpErrorManager.handle(error);

                            return EMPTY;
                        })
                    )
                );

                // Fire and reload once all resolve (or error out individually)
                let remaining = deletions.length;
                deletions.forEach((deletion$) => {
                    deletion$.subscribe({
                        complete: () => {
                            remaining -= 1;
                            if (remaining === 0) {
                                patchState(store, { selectedUsers: [] });
                                loadUsers();
                            }
                        }
                    });
                });
            }
        };
    }),
    withHooks((store) => ({
        onInit() {
            effect(() => {
                store.filter();
                store.page();
                store.rows();
                store.sortField();
                store.sortOrder();

                untracked(() => store.loadUsers());
            });
        }
    }))
);
