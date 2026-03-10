import { patchState, signalStore, withHooks, withMethods } from '@ngrx/signals';

import { Location } from '@angular/common';
import { effect, inject } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';

import { GlobalStore } from '@dotcms/store';

import { withConversions } from './features/with-conversions.feature';
import { withEngagement } from './features/with-engagement.feature';
import { withFilters } from './features/with-filters.feature';
import { withPageview } from './features/with-pageview.feature';

import { DASHBOARD_TABS, DashboardTab, TIME_RANGE_OPTIONS } from '../constants';
import { TimeRangeInput } from '../types';
import { isValidTab, paramsToTimeRange } from '../utils/filters.utils';
import { silentNavigate } from '../utils/router.utils';

/**
 * Analytics Dashboard Store
 *
 * Composed signal store for the analytics dashboard.
 * Uses signal store features for modular state management:
 *
 * - withFilters: Shared filter state (timeRange, currentTab)
 * - withPageview: Pageview report data
 * - withConversions: Conversions report data
 *
 * Data loading strategy:
 * - Automatically loads data for the active tab when tab, timeRange, or siteId changes
 * - Pageview data is loaded when the pageview tab is active
 * - Conversions data is loaded lazily when the conversions tab is first activated
 */
export const DotAnalyticsDashboardStore = signalStore(
    withFilters(),
    withPageview(),
    withConversions(),
    withEngagement(),
    // Coordinator methods that work across features
    withMethods(
        (
            store,
            route = inject(ActivatedRoute),
            router = inject(Router),
            location = inject(Location)
        ) => ({
            /**
             * Sets current tab and syncs URL without triggering Angular router navigation.
             */
            setCurrentTabAndNavigate(tab: DashboardTab): void {
                store.setCurrentTab(tab);
                silentNavigate(router, location, route, { tab });
            },

            /**
             * Refreshes all currently loaded data based on the current tab.
             */
            refreshAllData(): void {
                const currentTab = store.currentTab();

                switch (currentTab) {
                    case DASHBOARD_TABS.pageview:
                        store.loadAllPageviewData();
                        break;
                    case DASHBOARD_TABS.engagement:
                        store.loadEngagementData();
                        break;
                    case DASHBOARD_TABS.conversions:
                        store.loadConversionsData();
                        break;
                }
            },

            /**
             * Updates time range and syncs URL with query params.
             *
             * Uses `location.replaceState` (same as `setCurrentTabAndNavigate`) to avoid
             * triggering router events that would reset component state.
             *
             * When `timeRange` is the bare `'custom'` string (dropdown selected but no
             * dates chosen yet), only the URL is updated — store state is left unchanged
             * so no data reload is triggered until a full date range is confirmed.
             */
            updateTimeRange(timeRange: TimeRangeInput): void {
                const queryParams: Params = {};

                if (Array.isArray(timeRange)) {
                    // Complete custom date range — update state and URL
                    store.setTimeRange(timeRange);
                    queryParams['time_range'] = TIME_RANGE_OPTIONS.custom;
                    queryParams['from'] = timeRange[0];
                    queryParams['to'] = timeRange[1];
                } else {
                    // Predefined range OR bare 'custom' (no dates yet)
                    // Only update state for predefined ranges, not for bare 'custom'
                    if (timeRange !== TIME_RANGE_OPTIONS.custom) {
                        store.setTimeRange(timeRange);
                    }

                    queryParams['time_range'] = timeRange;
                    // Null out from/to to remove them from the URL when switching away from custom
                    queryParams['from'] = null;
                    queryParams['to'] = null;
                }

                silentNavigate(router, location, route, queryParams);
            }
        })
    ),
    withHooks({
        onInit(store) {
            const route = inject(ActivatedRoute);
            const globalStore = inject(GlobalStore);
            const params = route.snapshot.queryParams;

            // Set initial state from query params
            patchState(store, setTabFromQueryParams(params), setTimeRangeFromQueryParams(params));

            // Auto-load data when currentTab, timeRange, or currentSiteId changes
            effect(() => {
                const currentTab = store.currentTab();
                store.timeRange(); // Read to establish reactivity
                const currentSiteId = globalStore.currentSiteId();

                // Only load if we have a site ID
                if (!currentSiteId) {
                    return;
                }

                // Load data based on active tab
                switch (currentTab) {
                    case DASHBOARD_TABS.pageview:
                        store.loadAllPageviewData();
                        break;
                    case DASHBOARD_TABS.conversions:
                        store.loadConversionsData();
                        break;
                    case DASHBOARD_TABS.engagement:
                        store.loadEngagementData();
                        break;
                }
            });
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
