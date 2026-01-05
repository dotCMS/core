import { patchState, signalStoreFeature, withMethods, withState } from '@ngrx/signals';

import { DASHBOARD_TABS, DashboardTab, TIME_RANGE_OPTIONS } from '../../constants';
import { TimeRangeInput } from '../../types';

/**
 * State interface for the Filters feature.
 * Contains shared filter state for the analytics dashboard.
 */
export interface FiltersState {
    /** Current time range selection */
    timeRange: TimeRangeInput;
    /** Current active tab ('pageview' | 'conversions') */
    currentTab: DashboardTab;
}

/**
 * Initial state for the Filters feature.
 */
const initialFiltersState: FiltersState = {
    timeRange: TIME_RANGE_OPTIONS.last7days,
    currentTab: DASHBOARD_TABS.pageview
};

/**
 * Signal Store Feature for managing shared filters in the analytics dashboard.
 *
 * This feature provides state management for:
 * - Time range selection (shared across all reports)
 * - Current tab selection (pageview vs conversions)
 *
 * @returns Signal store feature with filters state and methods
 */
export function withFilters() {
    return signalStoreFeature(
        withState(initialFiltersState),
        withMethods((store) => ({
            /**
             * Sets the time range filter.
             * This will trigger data reload in features that observe timeRange.
             *
             * @param timeRange - The new time range to set
             */
            setTimeRange(timeRange: TimeRangeInput): void {
                patchState(store, { timeRange });
            },

            /**
             * Sets the current active tab.
             *
             * @param tab - The tab to set ('pageview' | 'conversions')
             */
            setCurrentTab(tab: DashboardTab): void {
                patchState(store, { currentTab: tab });
            }
        }))
    );
}
