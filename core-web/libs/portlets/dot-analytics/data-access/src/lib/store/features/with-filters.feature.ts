import { signalStoreFeature, withState } from '@ngrx/signals';
import { on, withReducer } from '@ngrx/signals/events';

import { DASHBOARD_TABS, DashboardTab, TIME_RANGE_OPTIONS } from '../../constants';
import { TimeRangeInput } from '../../types';
import { filtersApiEvents, filtersEvents } from '../events';

/**
 * State interface for the Filters feature.
 * Contains shared filter state for the analytics dashboard.
 */
export interface FiltersState {
    /** Current time range selection */
    timeRange: TimeRangeInput;
    /** Current active tab ('pageview' | 'conversions' | 'engagement') */
    currentTab: DashboardTab;
}

/**
 * Initial state for the Filters feature.
 */
const initialFiltersState: FiltersState = {
    timeRange: TIME_RANGE_OPTIONS.last7days,
    currentTab: DASHBOARD_TABS.engagement
};

/**
 * Signal Store Feature for managing shared filters in the analytics dashboard.
 *
 * State transitions are driven entirely by events from the
 * `@ngrx/signals/events` plugin (see `filtersEvents` and `filtersApiEvents`
 * in `../events`):
 *
 * - `tabSelected({ tab })` → patch `currentTab`
 * - `timeRangeSelected({ timeRange })` → patch `timeRange`, except for the
 *   bare `'custom'` string which leaves state untouched (only the URL
 *   syncs in that case)
 * - `filtersHydrated({ tab?, timeRange? })` → patch the keys that arrive
 *   from URL query params on store init
 *
 * @returns Signal store feature wiring filter state to its reducer
 */
export function withFilters() {
    return signalStoreFeature(
        withState(initialFiltersState),
        withReducer(
            on<FiltersState>(filtersEvents.tabSelected, ({ payload }) => ({
                currentTab: payload.tab
            })),
            on<FiltersState>(filtersEvents.timeRangeSelected, ({ payload }) => {
                if (payload.timeRange === TIME_RANGE_OPTIONS.custom) {
                    return {};
                }

                return { timeRange: payload.timeRange };
            }),
            on<FiltersState>(filtersApiEvents.filtersHydrated, ({ payload }) => {
                const next: Partial<FiltersState> = {};

                if (payload.tab) {
                    next.currentTab = payload.tab;
                }

                if (payload.timeRange) {
                    next.timeRange = payload.timeRange;
                }

                return next;
            })
        )
    );
}
