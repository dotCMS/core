import { signalStoreFeature, type } from '@ngrx/signals';
import { Events, withEventHandlers } from '@ngrx/signals/events';

import { inject } from '@angular/core';

import { mergeMap, withLatestFrom } from 'rxjs/operators';

import { GlobalStore } from '@dotcms/store';

import { DASHBOARD_TABS, DashboardTab } from '../../constants';
import { TimeRangeInput } from '../../types';
import {
    conversionsApiEvents,
    engagementApiEvents,
    externalEvents,
    filtersApiEvents,
    filtersEvents,
    pageviewApiEvents
} from '../events';
import { ConversionsState } from '../features/with-conversions.feature';
import { EngagementState } from '../features/with-engagement.feature';
import { FiltersState } from '../features/with-filters.feature';
import { PageviewState } from '../features/with-pageview.feature';

/**
 * Builds the per-metric `*Requested` events for the active tab so the HTTP
 * handlers in each feature slice can react. Returned as an array because
 * `mergeMap` flattens it into individual emissions, each of which is
 * auto-dispatched by `withEventHandlers`.
 */
const buildLoadEventsForTab = (
    tab: DashboardTab,
    payload: { timeRange: TimeRangeInput; currentSiteId: string }
) => {
    switch (tab) {
        case DASHBOARD_TABS.pageview:
            return [
                pageviewApiEvents.totalPageViewsRequested(payload),
                pageviewApiEvents.uniqueVisitorsRequested(payload),
                pageviewApiEvents.topPagePerformanceRequested(payload),
                pageviewApiEvents.pageViewTimeLineRequested(payload),
                pageviewApiEvents.pageViewDeviceBrowsersRequested(payload),
                pageviewApiEvents.topPagesTableRequested(payload)
            ];
        case DASHBOARD_TABS.conversions:
            return [
                conversionsApiEvents.totalConversionsRequested(payload),
                conversionsApiEvents.conversionTrendRequested(payload),
                conversionsApiEvents.convertingVisitorsRequested(payload),
                conversionsApiEvents.trafficVsConversionsRequested(payload),
                conversionsApiEvents.contentConversionsRequested(payload),
                conversionsApiEvents.conversionsOverviewRequested(payload)
            ];
        case DASHBOARD_TABS.engagement:
            return [
                engagementApiEvents.engagementKpisRequested(payload),
                engagementApiEvents.engagementBreakdownRequested(payload),
                engagementApiEvents.engagementSparklineRequested(payload),
                engagementApiEvents.engagementPlatformsRequested(payload)
            ];
        default:
            return [];
    }
};

/**
 * SignalStore feature that fans out filter intents and external signals
 * into per-metric `*Requested` events.
 *
 * Triggers:
 * - `filtersEvents.tabSelected` — user picked a different tab
 * - `filtersEvents.timeRangeSelected` — user changed the time range
 * - `filtersEvents.refreshRequested` — user clicked refresh
 * - `filtersApiEvents.filtersHydrated` — initial hydration on store init
 * - `externalEvents.siteChanged` — global site selector changed
 *
 * The reducer is guaranteed to have run by the time `Events` emits (see
 * `ReducerEvents` ordering in the plugin), so reading
 * `store.currentTab()` / `store.timeRange()` always reflects the
 * post-event state. `mergeMap` lets multiple parallel `*Requested` events
 * be emitted from a single trigger; the per-metric HTTP handlers use
 * `switchMap` to cancel stale requests when filters change again.
 *
 * Composed after `withFilters`, `withPageview`, `withConversions`, and
 * `withEngagement` so all required state slices are present.
 */
export function withAutoload() {
    return signalStoreFeature(
        {
            state: type<FiltersState & PageviewState & ConversionsState & EngagementState>()
        },
        withEventHandlers(
            (store, events = inject(Events), globalStore = inject(GlobalStore)) => ({
                fanOutLoadOnFilterChange$: events
                    .on(
                        filtersEvents.tabSelected,
                        filtersEvents.timeRangeSelected,
                        filtersEvents.refreshRequested,
                        filtersApiEvents.filtersHydrated,
                        externalEvents.siteChanged
                    )
                    .pipe(
                        // Read current state lazily inside withLatestFrom by
                        // mapping each emission through a synchronous resolver.
                        // `Events` already runs after `ReducerEvents` so state
                        // reflects the just-handled event.
                        withLatestFrom(),
                        mergeMap(() => {
                            const currentSiteId = globalStore.currentSiteId();
                            if (!currentSiteId) {
                                return [];
                            }

                            return buildLoadEventsForTab(store.currentTab(), {
                                timeRange: store.timeRange(),
                                currentSiteId
                            });
                        })
                    )
            })
        )
    );
}
