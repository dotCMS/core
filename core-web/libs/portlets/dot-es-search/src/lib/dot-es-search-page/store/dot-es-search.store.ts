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
import { pipe } from 'rxjs';

import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject } from '@angular/core';

import { switchMap, tap } from 'rxjs/operators';

import {
    DotEsSearchService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    ESSearchParams,
    ESSearchResponse,
    RawESSearchResponse
} from '@dotcms/dotcms-models';
import { PrincipalConfiguration } from '@dotcms/ui';

const DEFAULT_QUERY = '{\n  "query": {\n    "match_all": {}\n  }\n}';

export type EsSearchActiveTab = 'results' | 'raw' | 'aggregations' | 'suggestions';

export interface EsSearchState {
    query: string;
    params: Required<Omit<ESSearchParams, 'userid'>> & { userid: string; wrapCode: boolean };
    status: ComponentStatus;
    response: ESSearchResponse | null;
    rawResponse: RawESSearchResponse | null;
    queryTimeMs: number | null;
    activeTab: EsSearchActiveTab;
    emptyStateConfig: PrincipalConfiguration | null;
}

const initialState: EsSearchState = {
    query: DEFAULT_QUERY,
    params: {
        live: true,
        depth: 1,
        allCategoriesInfo: false,
        userid: '',
        wrapCode: false
    },
    status: ComponentStatus.INIT,
    response: null,
    rawResponse: null,
    queryTimeMs: null,
    activeTab: 'results',
    emptyStateConfig: null
};

export const DotEsSearchStore = signalStore(
    withState<EsSearchState>(initialState),
    withComputed((store) => ({
        hits: computed(() => store.response()?.esresponse[0]?.hits?.hits ?? []),
        hitCount: computed(() => store.response()?.esresponse[0]?.hits?.total ?? 0),
        aggregations: computed(() => store.response()?.esresponse[0]?.aggregations ?? null),
        suggestions: computed(() => store.response()?.esresponse[0]?.suggest ?? null),
        rawJson: computed(() =>
            store.rawResponse() ? JSON.stringify(store.rawResponse(), null, 2) : ''
        ),
        isLoading: computed(() => store.status() === ComponentStatus.LOADING),
        hasResults: computed(
            () => store.status() === ComponentStatus.LOADED && store.response() !== null
        )
    })),
    withMethods(
        (
            store,
            searchService = inject(DotEsSearchService),
            httpErrorManager = inject(DotHttpErrorManagerService)
        ) => ({
            setQuery(query: string): void {
                patchState(store, { query });
            },

            setParam<K extends keyof EsSearchState['params']>(
                key: K,
                value: EsSearchState['params'][K]
            ): void {
                patchState(store, { params: { ...store.params(), [key]: value } });
            },

            setActiveTab(tab: EsSearchActiveTab): void {
                patchState(store, { activeTab: tab });
            },

            runSearch: rxMethod<void>(
                pipe(
                    tap(() => patchState(store, { status: ComponentStatus.LOADING })),
                    switchMap(() => {
                        const start = Date.now();
                        // eslint-disable-next-line @typescript-eslint/no-unused-vars
                        const { userid, wrapCode: _wrapCode, ...apiParams } = store.params();

                        return searchService
                            .search(store.query(), { ...apiParams, ...(userid ? { userid } : {}) })
                            .pipe(
                                tapResponse({
                                    next: (response) => {
                                        patchState(store, {
                                            status: ComponentStatus.LOADED,
                                            response,
                                            queryTimeMs: Date.now() - start
                                        });
                                    },
                                    error: (error: HttpErrorResponse) => {
                                        httpErrorManager.handle(error);
                                        patchState(store, { status: ComponentStatus.ERROR });
                                    }
                                })
                            );
                    })
                )
            ),

            loadRaw: rxMethod<void>(
                pipe(
                    switchMap(() =>
                        searchService.searchRaw(store.query()).pipe(
                            tapResponse({
                                next: (rawResponse) => patchState(store, { rawResponse }),
                                error: (error: HttpErrorResponse) => httpErrorManager.handle(error)
                            })
                        )
                    )
                )
            )
        })
    ),
    withHooks({
        onInit(store) {
            const dotMessageService = inject(DotMessageService);

            patchState(store, {
                emptyStateConfig: {
                    title: dotMessageService.get('esSearch.results.empty'),
                    icon: 'pi-search',
                    subtitle: dotMessageService.get('esSearch.results.empty.hint')
                }
            });
        }
    })
);
