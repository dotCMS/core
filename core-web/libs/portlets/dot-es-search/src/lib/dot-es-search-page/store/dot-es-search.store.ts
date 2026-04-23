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
import { ComponentStatus, ESSearchParams, ESSearchResponse } from '@dotcms/dotcms-models';
import { PrincipalConfiguration } from '@dotcms/ui';

const DEFAULT_QUERY = '{\n  "query": {\n    "match_all": {}\n  }\n}';

export type EsSearchActiveTab = 'results' | 'raw' | 'aggregations' | 'suggestions';

export interface EsSearchState {
    query: string;
    params: Required<Omit<ESSearchParams, 'userid'>> & { userid: string; wrapCode: boolean };
    status: ComponentStatus;
    response: ESSearchResponse | null;
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
    queryTimeMs: null,
    activeTab: 'results',
    emptyStateConfig: null
};

export const DotEsSearchStore = signalStore(
    withState<EsSearchState>(initialState),
    withComputed((store) => ({
        contentlets: computed(() => store.response()?.contentlets ?? []),
        hits: computed(() => store.response()?.esresponse[0]?.hits?.hits ?? []),
        hitCount: computed(() => {
            const total = store.response()?.esresponse[0]?.hits?.total;
            if (total == null) return 0;
            return typeof total === 'object' ? total.value : total;
        }),
        rawJson: computed(() =>
            store.response() ? JSON.stringify(store.response(), null, 2) : ''
        ),
        isLoading: computed(() => store.status() === ComponentStatus.LOADING),
        hasResults: computed(
            () => store.status() === ComponentStatus.LOADED && store.response() !== null
        ),
        aggregations: computed(() => store.response()?.esresponse[0]?.aggregations ?? null),
        hasAggregations: computed(() => {
            const aggs = store.response()?.esresponse[0]?.aggregations;
            return !!aggs && Object.keys(aggs).length > 0;
        }),
        suggestions: computed(() => store.response()?.esresponse[0]?.suggest ?? null),
        hasSuggestions: computed(() => {
            const suggest = store.response()?.esresponse[0]?.suggest;
            return !!suggest && Object.keys(suggest).length > 0;
        })
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
                    tap(() =>
                        patchState(store, {
                            status: ComponentStatus.LOADING,
                            activeTab: 'results'
                        })
                    ),
                    switchMap(() => {
                        const start = Date.now();
                        // eslint-disable-next-line @typescript-eslint/no-unused-vars
                        const { userid, wrapCode: _wrapCode, ...apiParams } = store.params();

                        return searchService
                            .search(store.query(), { ...apiParams, ...(userid ? { userid } : {}) })
                            .pipe(
                                tapResponse({
                                    next: (response) => {
                                        const hits = response.esresponse[0]?.hits?.hits ?? [];
                                        const aggs = response.esresponse[0]?.aggregations;
                                        const hasAggs = !!aggs && Object.keys(aggs).length > 0;
                                        const activeTab: EsSearchActiveTab =
                                            hasAggs && hits.length === 0
                                                ? 'aggregations'
                                                : 'results';

                                        patchState(store, {
                                            status: ComponentStatus.LOADED,
                                            response,
                                            queryTimeMs: Date.now() - start,
                                            activeTab
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
