import { signalStore, withHooks } from '@ngrx/signals';
import { Dispatcher } from '@ngrx/signals/events';

import { effect, inject, untracked } from '@angular/core';
import { ActivatedRoute, Params } from '@angular/router';

import { GlobalStore } from '@dotcms/store';

import { externalEvents, filtersApiEvents } from './events';
import { withConversions } from './features/with-conversions.feature';
import { withEngagement } from './features/with-engagement.feature';
import { withFilters } from './features/with-filters.feature';
import { withPageview } from './features/with-pageview.feature';
import { withAutoload } from './handlers/with-autoload.handlers';
import { withNavigation } from './handlers/with-navigation.handlers';

import { DashboardTab } from '../constants';
import { isValidTab, paramsToTimeRange } from '../utils/filters.utils';

/**
 * Analytics Dashboard Store
 *
 * Composed signal store for the analytics dashboard. State and side effects
 * are split across feature slices and handler features:
 *
 * - withFilters: shared filter state (timeRange, currentTab) via withReducer
 * - withPageview / withConversions / withEngagement: per-tab metric state
 *   plus HTTP handlers that dispatch *Loaded / *Failed events
 * - withNavigation: URL sync, breadcrumb, banner localStorage
 * - withAutoload: fan-out from filter intents and global site changes to
 *   per-metric *Requested events
 *
 * Components interact with the store by dispatching events from
 * `./events` via `injectDispatch(...)`. The store exposes only state
 * signals and computeds — no public mutator methods.
 *
 * @see https://arcadioquintero.com/en/blog/ngrx-signalstore-events-plugin
 */
export const DotAnalyticsDashboardStore = signalStore(
    withFilters(),
    withPageview(),
    withConversions(),
    withEngagement(),
    withNavigation(),
    withAutoload(),
    withHooks({
        onInit(store) {
            const route = inject(ActivatedRoute);
            const globalStore = inject(GlobalStore);
            const dispatcher = inject(Dispatcher);
            const params = route.snapshot.queryParams;

            // Hydrate initial filter state from URL query params via a single
            // dispatched event. The reducer patches state for the keys that
            // are present; navigation handler refreshes the breadcrumb;
            // autoload handler fans out per-metric *Requested events.
            const tabFromUrl = readTabFromQueryParams(params);
            const timeRangeFromUrl = paramsToTimeRange(params || {});
            dispatcher.dispatch(
                filtersApiEvents.filtersHydrated({
                    tab: tabFromUrl ?? store.currentTab(),
                    ...(timeRangeFromUrl !== undefined ? { timeRange: timeRangeFromUrl } : {})
                })
            );

            // Bridge the global site signal into a `siteChanged` event so the
            // autoload handler can react uniformly to filter and site
            // changes. `untracked()` prevents the inner dispatch from
            // creating a circular signal subscription.
            effect(() => {
                const siteId = globalStore.currentSiteId();
                if (siteId) {
                    untracked(() => dispatcher.dispatch(externalEvents.siteChanged({ siteId })));
                }
            });
        }
    })
);

/**
 * Reads and validates the `tab` query param. Returns `undefined` when the
 * param is absent or doesn't match a known dashboard tab.
 */
const readTabFromQueryParams = (params: Params): DashboardTab | undefined => {
    const candidate = params?.['tab'];

    if (candidate && isValidTab(candidate)) {
        return candidate as DashboardTab;
    }

    return undefined;
};
