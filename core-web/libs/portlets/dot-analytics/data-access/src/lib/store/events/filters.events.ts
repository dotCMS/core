import { type } from '@ngrx/signals';
import { eventGroup } from '@ngrx/signals/events';

import { DashboardTab } from '../../constants';
import { TimeRangeInput } from '../../types';

/**
 * User intent events dispatched from the analytics filters toolbar.
 *
 * Components dispatch these via `injectDispatch(filtersEvents)` to express
 * filter changes; the store reacts via `withReducer` (state) and
 * `withEventHandlers` (URL sync, autoload fan-out).
 */
export const filtersEvents = eventGroup({
    source: 'Analytics Filters',
    events: {
        /** User selected a different dashboard tab. */
        tabSelected: type<{ tab: DashboardTab }>(),
        /** User picked a new time range (predefined, custom array, or bare 'custom'). */
        timeRangeSelected: type<{ timeRange: TimeRangeInput }>(),
        /** User clicked the refresh button. */
        refreshRequested: type<void>()
    }
});
