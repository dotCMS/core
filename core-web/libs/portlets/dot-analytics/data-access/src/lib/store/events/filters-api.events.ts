import { type } from '@ngrx/signals';
import { eventGroup } from '@ngrx/signals/events';

import { DashboardTab } from '../../constants';
import { TimeRangeInput } from '../../types';

/**
 * Downstream filter facts dispatched after hydration or async sources settle.
 *
 * Not intended to be dispatched from user-facing components — these signal
 * that a filter-related state change has happened (URL hydration on init,
 * cross-store reactions, etc.).
 */
export const filtersApiEvents = eventGroup({
    source: 'Analytics Filters API',
    events: {
        /**
         * Initial filter state derived from URL query params on store init.
         * Either `tab`, `timeRange`, or both may be set; missing keys mean
         * "leave the current state value untouched".
         */
        filtersHydrated: type<{ tab?: DashboardTab; timeRange?: TimeRangeInput }>()
    }
});
