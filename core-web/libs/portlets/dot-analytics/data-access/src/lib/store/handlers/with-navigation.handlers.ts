import { signalStoreFeature, type } from '@ngrx/signals';
import { Events, withEventHandlers } from '@ngrx/signals/events';

import { Location } from '@angular/common';
import { inject } from '@angular/core';
import { ActivatedRoute, Params, Router } from '@angular/router';

import { tap } from 'rxjs/operators';

import { DotLocalstorageService } from '@dotcms/data-access';

import { TIME_RANGE_OPTIONS } from '../../constants';
import { TimeRangeInput } from '../../types';
import { silentNavigate } from '../../utils/router.utils';
import { filtersEvents, uiEvents } from '../events';
import { FiltersState } from '../features/with-filters.feature';

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
 * SignalStore feature that wires URL synchronization and localStorage
 * persistence as side-effect handlers over events from the
 * `@ngrx/signals/events` plugin.
 *
 * - `filtersEvents.tabSelected` → silent URL update for `tab` query param
 * - `filtersEvents.timeRangeSelected` → silent URL update for `time_range`,
 *   `from`, `to` query params
 * - `uiEvents.messageBannerDismissed` → persist dismissal to localStorage
 *
 * "Silent" navigation uses `location.replaceState` (no router event) so the
 * dashboard doesn't unmount/remount on filter changes — same behavior as
 * the legacy `setCurrentTabAndNavigate` / `updateTimeRange` methods this
 * feature replaces.
 *
 * Breadcrumb refresh on `currentTab` changes still lives in the store's
 * `withHooks.onInit` `effect` during the migration, so legacy method calls
 * (`store.setCurrentTab`, `store.setCurrentTabAndNavigate`) keep updating
 * the breadcrumb. Step 10 migrates that effect to an event handler once
 * all callers dispatch events instead.
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
                localStorageService = inject(DotLocalstorageService)
            ) => ({
                /**
                 * Sync the `tab` query param when the user picks a tab.
                 */
                syncTabUrl$: events.on(filtersEvents.tabSelected).pipe(
                    tap(({ payload }) => {
                        silentNavigate(router, location, route, { tab: payload.tab });
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
