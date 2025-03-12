import { signalStoreFeature, type, withMethods } from '@ngrx/signals';

import { inject } from '@angular/core';

import { DotAnalyticsTrackerService } from '@dotcms/data-access';
import { EVENT_TYPES } from '@dotcms/dotcms-models';

import { AnalyticsUVEModeChange, AnalyticsUVECalendarChange } from './models';
import { DEBOUNCE_FOR_TRACKING, TRACKING_DELAY } from './utils';

import { UVEState } from '../../models';

/**
 *
 * @description This feature is used to handle the tracking of events
 * @export
 * @return {*}
 */
export function withTrack() {
    return signalStoreFeature(
        {
            state: type<UVEState>()
        },
        withMethods((_, analyticsTrackerService = inject(DotAnalyticsTrackerService)) => ({
            trackUVEModeChange: DEBOUNCE_FOR_TRACKING((payload: AnalyticsUVEModeChange) => {
                analyticsTrackerService.track<AnalyticsUVEModeChange>(
                    EVENT_TYPES.UVE_MODE_CHANGE,
                    payload
                );
            }, TRACKING_DELAY),
            trackUVECalendarChange: DEBOUNCE_FOR_TRACKING((payload: AnalyticsUVECalendarChange) => {
                analyticsTrackerService.track<AnalyticsUVECalendarChange>(
                    EVENT_TYPES.UVE_CALENDAR_CHANGE,
                    payload
                );
            }, TRACKING_DELAY)
        }))
    );
}
