import { patchState, signalStore, withHooks, withMethods } from '@ngrx/signals';

import { inject } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';

import { withConversions } from './features/with-conversions.feature';
import { isValidTab, paramsToTimeRange, withFilters } from './features/with-filters.feature';
import { withPageview } from './features/with-pageview.feature';

import { DASHBOARD_TABS, DashboardTab, TIME_RANGE_OPTIONS } from '../constants';
import { TimeRangeInput } from '../types';

/**
 * Analytics Dashboard Store
 *
 * Composed signal store for the analytics dashboard.
 * Uses signal store features for modular state management:
 *
 * - withFilters: Shared filter state (timeRange, currentTab)
 * - withPageview: Pageview report data with auto-loading
 * - withConversions: Conversions report data with lazy loading
 *
 * @example
 * ```typescript
 * // In component
 * readonly store = inject(DotAnalyticsDashboardStore);
 *
 * // Access filter state
 * const timeRange = this.store.timeRange();
 * this.store.setTimeRange('last30days');
 *
 * // Access pageview data (auto-loaded)
 * const totalPageViews = this.store.totalPageViews();
 *
 * // Load conversions data (lazy)
 * this.store.loadConversionsData();
 * ```
 */
export const DotAnalyticsDashboardStore = signalStore(
    withFilters(),
    withPageview(),
    withConversions(),
    // Coordinator methods that work across features
    withMethods((store, route = inject(ActivatedRoute), router = inject(Router)) => ({
        /**
         * Updates time range from query params and syncs URL.
         * Converts query params to time range, updates store, and navigates.
         * Preserves all query params (including tab).
         */
        refreshQueryParams(queryParams: Params): void {
            const timeRange = paramsToTimeRange(queryParams);
            store.setTimeRange(timeRange);

            // Update URL (merge to keep tab param and any other params)
            router.navigate([], {
                relativeTo: route,
                queryParams: queryParams,
                queryParamsHandling: 'merge',
                replaceUrl: true
            });
        },

        /**
         * Sets current tab and syncs URL.
         */
        setCurrentTabAndNavigate(tab: DashboardTab): void {
            store.setCurrentTab(tab);

            // Update URL with tab query param
            // TODO: Find a better way to update the URL with the tab query param.
            router.navigate([], {
                relativeTo: route,
                queryParams: { tab: tab },
                queryParamsHandling: 'merge',
                replaceUrl: true
            });
        },

        /**
         * Refreshes all currently loaded data based on the current tab.
         */
        refreshAllData(): void {
            const currentTab = store.currentTab();

            if (currentTab === DASHBOARD_TABS.pageview) {
                store.loadAllPageviewData();
            } else {
                store.loadConversionsData();
            }
        },

        /**
         * Sets time range and syncs URL.
         */
        setTimeRangeAndNavigate(timeRange: TimeRangeInput): void {
            store.setTimeRange(timeRange);

            // Build query params from time range
            const queryParams: Params = {};

            if (Array.isArray(timeRange)) {
                // Custom date range
                queryParams['time_range'] = TIME_RANGE_OPTIONS.custom;
                queryParams['from'] = timeRange[0];
                queryParams['to'] = timeRange[1];
            } else {
                // Predefined range
                queryParams['time_range'] = timeRange;
            }

            // Update URL
            // TODO: Find a better way to update the URL with the time range query params.
            router.navigate([], {
                relativeTo: route,
                queryParams: queryParams,
                queryParamsHandling: 'merge',
                replaceUrl: true
            });
        }
    })),
    withHooks({
        onInit(store) {
            const route = inject(ActivatedRoute);
            // Initialize state from URL query params (only once on mount)
            const params = route.snapshot.queryParams;

            // Use custom state updaters
            patchState(store, setTabFromQueryParams(params), setTimeRangeFromQueryParams(params));
        }
    })
);

/**
 * Sets the time range from the query params.
 * @param params - The query params.
 * @returns The time range.
 */
const setTimeRangeFromQueryParams = (params: Params) => {
    const timeRange = paramsToTimeRange(params || {});

    return { timeRange };
};

/**
 * Sets the tab from the query params.
 * @param params - The query params.
 * @returns The tab.
 */
const setTabFromQueryParams = (params: Params) => {
    const currentTab = params?.['tab'];

    if (currentTab && isValidTab(currentTab)) {
        return { currentTab };
    }

    return {};
};
