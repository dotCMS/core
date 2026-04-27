import { signalStoreFeature, type } from '@ngrx/signals';
import { Events, withEventHandlers } from '@ngrx/signals/events';

import { Location } from '@angular/common';
import { inject } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';

import { tap } from 'rxjs/operators';

import { DotLocalstorageService, DotMessageService } from '@dotcms/data-access';
import { GlobalStore } from '@dotcms/store';

import { DASHBOARD_TAB_LIST, DashboardTab, TIME_RANGE_OPTIONS } from '../../constants';
import { TimeRangeInput } from '../../types';
import { silentNavigate } from '../../utils/router.utils';
import { filtersApiEvents, filtersEvents, uiEvents } from '../events';
import { FiltersState } from '../features/with-filters.feature';

const TAB_CONFIG_MAP = new Map(DASHBOARD_TAB_LIST.map((tab) => [tab.id, tab]));

const HIDE_ANALYTICS_MESSAGE_BANNER_KEY = 'analytics-dashboard-hide-message-banner';

/**
 * Builds the query params patch for a time range change.
 * - `DateRange` array → `time_range=custom` plus `from`/`to` ISO dates
 * - bare string → `time_range=<value>` and null out `from`/`to`
 */
const buildTimeRangeQueryParams = (timeRange: TimeRangeInput): Params => {
    if (Array.isArray(timeRange)) {
        return {
            time_range: TIME_RANGE_OPTIONS.custom,
            from: timeRange[0],
            to: timeRange[1]
        };
    }

    return {
        time_range: timeRange,
        from: null,
        to: null
    };
};

/**
 * Refreshes the global breadcrumb to match the given dashboard tab.
 */
const refreshBreadcrumb = (
    tab: DashboardTab,
    globalStore: InstanceType<typeof GlobalStore>,
    messageService: DotMessageService
): void => {
    const tabConfig = TAB_CONFIG_MAP.get(tab);
    if (tabConfig) {
        globalStore.addNewBreadcrumb({
            id: `analytics-${tab}`,
            label: messageService.get(tabConfig.label)
        });
    }
};

/**
 * SignalStore feature that wires URL synchronization, breadcrumb updates,
 * and localStorage persistence as side-effect handlers over events from
 * the `@ngrx/signals/events` plugin.
 *
 * - `filtersEvents.tabSelected` → silent URL update + breadcrumb refresh
 * - `filtersApiEvents.filtersHydrated` → breadcrumb refresh on init
 * - `filtersEvents.timeRangeSelected` → silent URL update for time_range,
 *   from, to query params
 * - `uiEvents.messageBannerDismissed` → persist dismissal to localStorage
 *
 * "Silent" navigation uses `location.replaceState` (no router event) so
 * the dashboard doesn't unmount/remount on filter changes.
 *
 * Reads `FiltersState` only as a typing hint; compose after `withFilters()`.
 */
export function withNavigation() {
    return signalStoreFeature(
        { state: type<FiltersState>() },
        withEventHandlers(
            (
                _store,
                events = inject(Events),
                router = inject(Router),
                location = inject(Location),
                route = inject(ActivatedRoute),
                globalStore = inject(GlobalStore),
                messageService = inject(DotMessageService),
                localStorageService = inject(DotLocalstorageService)
            ) => ({
                /**
                 * Sync the `tab` query param and refresh the breadcrumb
                 * when the user picks a different dashboard tab.
                 */
                syncTabUrl$: events.on(filtersEvents.tabSelected).pipe(
                    tap(({ payload }) => {
                        silentNavigate(router, location, route, { tab: payload.tab });
                        refreshBreadcrumb(payload.tab, globalStore, messageService);
                    })
                ),

                /**
                 * Refresh the breadcrumb after URL-driven hydration on init
                 * so deep-links like `?tab=engagement` show the right label.
                 */
                refreshBreadcrumbOnHydrate$: events.on(filtersApiEvents.filtersHydrated).pipe(
                    tap(({ payload }) => {
                        if (payload.tab) {
                            refreshBreadcrumb(payload.tab, globalStore, messageService);
                        }
                    })
                ),

                /**
                 * Sync the `time_range`, `from`, `to` query params when the
                 * user picks a new time range. Bare 'custom' (no dates yet)
                 * still updates the URL — the reducer is the one that skips
                 * the state mutation for that case.
                 */
                syncTimeRangeUrl$: events.on(filtersEvents.timeRangeSelected).pipe(
                    tap(({ payload }) => {
                        silentNavigate(
                            router,
                            location,
                            route,
                            buildTimeRangeQueryParams(payload.timeRange)
                        );
                    })
                ),

                /**
                 * Persist the message banner dismissal preference so it
                 * stays hidden across reloads.
                 */
                persistBannerDismissal$: events.on(uiEvents.messageBannerDismissed).pipe(
                    tap(() => {
                        localStorageService.setItem(HIDE_ANALYTICS_MESSAGE_BANNER_KEY, true);
                    })
                )
            })
        )
    );
}
