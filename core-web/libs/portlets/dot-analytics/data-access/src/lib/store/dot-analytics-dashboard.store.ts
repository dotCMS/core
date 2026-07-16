import { patchState, signalStore, withHooks, withMethods } from '@ngrx/signals';

import { effect, inject } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';

import { DotMessageService } from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';

import { withConversions } from './features/with-conversions.feature';
import { withEngagement } from './features/with-engagement.feature';
import { withFilters } from './features/with-filters.feature';
import { withPageview } from './features/with-pageview.feature';

import { DASHBOARD_TAB_LIST, DASHBOARD_TABS, TIME_RANGE_OPTIONS } from '../constants';
import { TimeRangeInput } from '../types';
import { paramsToTimeRange } from '../utils/filters.utils';

const TAB_CONFIG_MAP = new Map(DASHBOARD_TAB_LIST.map((tab) => [tab.id, tab]));

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
    withMethods((store, route = inject(ActivatedRoute), router = inject(Router)) => ({
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
         * Updates time range and syncs URL with query params using the Angular Router
         * so `routerLink` + `queryParamsHandling="preserve"` keeps the current filter
         * in the URL when switching report tabs.
         *
         * Uses `replaceUrl: true` to avoid stacking history entries like the previous
         * replaceState-only update.
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

            void router.navigate([], {
                relativeTo: route,
                queryParams,
                queryParamsHandling: 'merge',
                replaceUrl: true
            });
        }
    })),
    withHooks({
        onInit(store) {
            const route = inject(ActivatedRoute);
            const globalStore = inject(GlobalStore);
            const messageService = inject(DotMessageService);
            const params = route.snapshot.queryParams;

            // Set initial time range from query params (tab is synced from the route in the shell component)
            patchState(store, setTimeRangeFromQueryParams(params));

            // Update breadcrumb when currentTab changes
            effect(() => {
                const currentTab = store.currentTab();
                const tabConfig = TAB_CONFIG_MAP.get(currentTab);

                if (tabConfig) {
                    globalStore.addNewBreadcrumb({
                        id: `analytics-${currentTab}`,
                        label: messageService.get(tabConfig.label)
                    });
                }
            });

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
