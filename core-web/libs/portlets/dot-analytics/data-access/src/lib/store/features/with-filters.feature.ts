import { patchState, signalStoreFeature, withMethods, withState } from '@ngrx/signals';
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
    /** Current active tab ('pageview' | 'conversions') */
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
 * State transitions are driven by events from the `@ngrx/signals/events`
 * plugin (see `filtersEvents` and `filtersApiEvents` in `../events`). The
 * legacy imperative methods (`setTimeRange`, `setCurrentTab`) are kept
 * during the migration; they will be removed in Step 10 once all callers
 * dispatch events instead.
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
                // Bare 'custom' string (dropdown picked, no dates yet) must NOT
                // update state — the URL side-effect runs regardless via the
                // navigation handler. State only changes for predefined ranges
                // and complete custom DateRange tuples.
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
        ),
        // Legacy methods kept only during the incremental migration. Removed
        // in Step 10 once `dot-analytics-dashboard.store.ts` and the dashboard
        // component dispatch events instead of calling these mutators.
        withMethods((store) => ({
            /**
             * @deprecated Dispatch `filtersEvents.timeRangeSelected({ timeRange })` instead.
             */
            setTimeRange(timeRange: TimeRangeInput): void {
                patchState(store, { timeRange });
            },

            /**
             * @deprecated Dispatch `filtersEvents.tabSelected({ tab })` instead.
             */
            setCurrentTab(tab: DashboardTab): void {
                patchState(store, { currentTab: tab });
            }
        }))
    );
}
