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

export const MAX_HITS = 1000;

export type EsSearchActiveTab = 'results' | 'raw' | 'aggregations' | 'suggestions';

export interface EsSearchState {
    query: string;
    params: Required<Omit<ESSearchParams, 'userid'>> & { userid: string };
    wrapCode: boolean;
    status: ComponentStatus;
    response: ESSearchResponse | null;
    queryTimeMs: number | null;
    activeTab: EsSearchActiveTab;
    emptyStateConfig: PrincipalConfiguration | null;
    queryWasCapped: boolean;
}

const initialState: EsSearchState = {
    query: '',
    params: {
        live: true,
        userid: ''
    },
    wrapCode: false,
    status: ComponentStatus.INIT,
    response: null,
    queryTimeMs: null,
    activeTab: 'results',
    emptyStateConfig: null,
    queryWasCapped: false
};

export const DotEsSearchStore = signalStore(
    withState<EsSearchState>(initialState),
    withComputed((store) => ({
        contentlets: computed(() => store.response()?.contentlets ?? []),
        hits: computed(() =>
            (store.response()?.esresponse[0]?.hits?.hits ?? []).slice(0, MAX_HITS)
        ),
        hitCount: computed(() => {
            const total = store.response()?.esresponse[0]?.hits?.total;
            if (total == null) return 0;
            return typeof total === 'object' ? total.value : total;
        }),
        isCapped: computed(
            () => (store.response()?.esresponse[0]?.hits?.hits?.length ?? 0) > MAX_HITS
        ),
        rawJson: computed(() => {
            const response = store.response();
            if (!response) return '';
            const hitsArray = response.esresponse[0]?.hits?.hits;
            if (!hitsArray || hitsArray.length <= MAX_HITS) {
                return JSON.stringify(response, null, 2);
            }
            const capped = {
                ...response,
                esresponse: [
                    {
                        ...response.esresponse[0],
                        hits: {
                            ...response.esresponse[0].hits,
                            hits: hitsArray.slice(0, MAX_HITS)
                        }
                    }
                ] as ESSearchResponse['esresponse']
            };
            return JSON.stringify(capped, null, 2);
        }),
        isLoading: computed(() => store.status() === ComponentStatus.LOADING),
        hasResults: computed(
            () =>
                store.status() === ComponentStatus.LOADED &&
                (store.response()?.contentlets.length ?? 0) > 0
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

            setWrapCode(value: boolean): void {
                patchState(store, { wrapCode: value });
            },

            setActiveTab(tab: EsSearchActiveTab): void {
                patchState(store, { activeTab: tab });
            },

            runSearch: rxMethod<void>(
                pipe(
                    tap(() =>
                        patchState(store, {
                            status: ComponentStatus.LOADING,
                            activeTab: 'results',
                            queryWasCapped: false
                        })
                    ),
                    switchMap(() => {
                        const start = Date.now();
                        const { userid, ...apiParams } = store.params();

                        let query = store.query();
                        try {
                            const parsed = JSON.parse(query);
                            if (typeof parsed['size'] === 'number' && parsed['size'] > MAX_HITS) {
                                parsed['size'] = MAX_HITS;
                                query = JSON.stringify(parsed, null, 2);
                                patchState(store, { query, queryWasCapped: true });
                            }
                        } catch {
                            // invalid JSON — let the server handle it
                        }

                        return searchService
                            .search(query, { ...apiParams, ...(userid ? { userid } : {}) })
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
