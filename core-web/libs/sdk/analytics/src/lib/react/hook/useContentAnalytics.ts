import { useContext } from 'react';

import { DotContentAnalyticsCustomHook } from '../../dotAnalytics/shared/dot-content-analytics.model';
import { isInsideEditor } from '../../dotAnalytics/shared/dot-content-analytics.utils';
import DotContentAnalyticsContext from '../contexts/DotContentAnalyticsContext';

/**
 * Custom hook that handles analytics page view tracking.
 *
 * @returns {DotContentAnalyticsCustomHook} - The analytics instance used to track page views
 *
 */
export const useContentAnalytics = (): DotContentAnalyticsCustomHook => {
    const instance = useContext(DotContentAnalyticsContext);

    return {
        /**
         * Track an event with the analytics instance.
         *
         * @param {string} eventName - The name of the event to track
         * @param {object} payload - Additional data to include with the event
         */
        track: (eventName: string, payload: Record<string, unknown> = {}) => {
            if (instance?.track && !isInsideEditor()) {
                instance.track(eventName, {
                    ...payload,
                    timestamp: new Date().toISOString()
                });
            }
        }
    };
};
