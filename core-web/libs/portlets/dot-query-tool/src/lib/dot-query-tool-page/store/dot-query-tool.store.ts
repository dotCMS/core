import { tapResponse } from '@ngrx/operators';
import {
    patchState,
    signalStore,
    withComputed,
    withHooks,
    withMethods,
    withState
} from '@ngrx/signals';
import { rxMethod } from '@ngrx/signals/rxjs-interop';
import { EMPTY, pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';

import { catchError, switchMap, take, tap } from 'rxjs/operators';

import {
    DotCurrentUserService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { ComponentStatus, DotCMSContentlet } from '@dotcms/dotcms-models';
import { PrincipalConfiguration } from '@dotcms/ui';

import {
    QueryToolActiveTab,
    QueryToolSearchForm,
    QueryToolSearchResponse
} from '../../models/dot-query-tool.models';
import { DotQueryToolService } from '../../services/dot-query-tool.service';

export const DEFAULT_LIMIT = 20;
export const DEFAULT_OFFSET = 0;
export const MAX_RESULTS = 1000;

export interface QueryToolState {
    query: string;
    sort: string;
    offset: number;
    limit: number;
    userId: string;
    isAdmin: boolean;
    status: ComponentStatus;
    response: QueryToolSearchResponse | null;
    queryTimeMs: number | null;
    activeTab: QueryToolActiveTab;
    emptyStateConfig: PrincipalConfiguration | null;
}

const initialState: QueryToolState = {
    query: '',
    sort: '',
    offset: DEFAULT_OFFSET,
    limit: DEFAULT_LIMIT,
    userId: '',
    isAdmin: false,
    status: ComponentStatus.INIT,
    response: null,
    queryTimeMs: null,
    activeTab: 'results',
    emptyStateConfig: null
};

export const DotQueryToolStore = signalStore(
    withState<QueryToolState>(initialState),
    withComputed((store) => ({
        contentlets: computed<DotCMSContentlet[]>(
            () => store.response()?.jsonObjectView.contentlets ?? []
        ),
        resultsSize: computed(() => store.response()?.resultsSize ?? 0),
        queryTook: computed(() => store.response()?.queryTook ?? 0),
        contentTook: computed(() => store.response()?.contentTook ?? 0),
        isLoading: computed(() => store.status() === ComponentStatus.LOADING),
        hasLoadedResults: computed(
            () =>
                store.status() === ComponentStatus.LOADED &&
                (store.response()?.jsonObjectView.contentlets.length ?? 0) > 0
        ),
        rawJson: computed(() => {
            const response = store.response();
            return response ? JSON.stringify(response, null, 2) : '';
        }),
        showingFrom: computed(() => {
            const total = store.response()?.resultsSize ?? 0;
            const returned = store.response()?.jsonObjectView.contentlets.length ?? 0;
            return returned === 0 ? 0 : Math.min(store.offset() + 1, total);
        }),
        showingTo: computed(() => {
            const returned = store.response()?.jsonObjectView.contentlets.length ?? 0;
            return store.offset() + returned;
        }),
        limitWasCapped: computed(() => store.limit() > MAX_RESULTS),
        apiRequestBody: computed<QueryToolSearchForm>(() => {
            const limit = Math.min(store.limit(), MAX_RESULTS);
            const userId = store.userId();
            return {
                query: store.query(),
                sort: store.sort(),
                offset: store.offset(),
                limit,
                ...(userId ? { userId } : {})
            };
        })
    })),
    withMethods(
        (
            store,
            queryToolService = inject(DotQueryToolService),
            httpErrorManager = inject(DotHttpErrorManagerService)
        ) => ({
            setQuery(query: string): void {
                patchState(store, { query });
            },
            setSort(sort: string): void {
                patchState(store, { sort });
            },
            setOffset(offset: number): void {
                patchState(store, { offset: Math.max(0, offset) });
            },
            setLimit(limit: number): void {
                patchState(store, { limit: Math.max(1, limit) });
            },
            setUserId(userId: string): void {
                patchState(store, { userId });
            },
            setActiveTab(tab: QueryToolActiveTab): void {
                patchState(store, { activeTab: tab });
            },
            resetOffset(): void {
                patchState(store, { offset: 0 });
            },
            runSearch: rxMethod<void>(
                pipe(
                    tap(() =>
                        patchState(store, {
                            status: ComponentStatus.LOADING,
                            activeTab: 'results'
                        })
                    ),
                    switchMap(() => {
                        const start = Date.now();
                        return queryToolService.search(store.apiRequestBody()).pipe(
                            tapResponse({
                                next: (response) => {
                                    patchState(store, {
                                        status: ComponentStatus.LOADED,
                                        response,
                                        queryTimeMs: Date.now() - start
                                    });
                                },
                                error: (error: HttpErrorResponse) => {
                                    patchState(store, { status: ComponentStatus.ERROR });
                                    httpErrorManager.handle(error);
                                }
                            })
                        );
                    })
                )
            )
        })
    ),
    withHooks({
        onInit(store) {
            const messageService = inject(DotMessageService);
            const currentUserService = inject(DotCurrentUserService);
            const httpErrorManager = inject(DotHttpErrorManagerService);

            patchState(store, {
                emptyStateConfig: {
                    title: messageService.get('queryTool.results.empty'),
                    icon: 'pi-search',
                    subtitle: messageService.get('queryTool.results.empty.hint')
                }
            });

            currentUserService
                .getCurrentUser()
                .pipe(
                    take(1),
                    catchError((error: HttpErrorResponse) => {
                        httpErrorManager.handle(error);
                        return EMPTY;
                    })
                )
                .subscribe(({ admin }) => patchState(store, { isAdmin: admin }));
        }
    })
);
