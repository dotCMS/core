import { signalStoreFeature, type, withMethods } from '@ngrx/signals';

import { inject } from '@angular/core';

import { DotAnalyticsTrackerService } from '@dotcms/data-access';
import { EVENT_TYPES } from '@dotcms/dotcms-models';

import { AnalyticsUVEModeChange, AnalyticsUVECalendarChange } from './models';

import { UVEState } from '../../models';

const debounce = <T>(func: (...args: T[]) => void, delay: number) => {
    let timeout: number;

    return function (...args: T[]) {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), delay);
    };
};

const TRACKING_DELAY = 5000;

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
        withMethods((_, analyticsTracker = inject(DotAnalyticsTrackerService)) => ({
            trackUVEModeChange: debounce((payload: AnalyticsUVEModeChange) => {
                analyticsTracker.track<AnalyticsUVEModeChange>(
                    EVENT_TYPES.UVE_MODE_CHANGE,
                    payload
                );
            }, TRACKING_DELAY),
            trackUVECalendarChange: debounce((payload: AnalyticsUVECalendarChange) => {
                analyticsTracker.track<AnalyticsUVECalendarChange>(
                    EVENT_TYPES.UVE_CALENDAR_CHANGE,
                    payload
                );
            }, TRACKING_DELAY)
        }))
    );
}
