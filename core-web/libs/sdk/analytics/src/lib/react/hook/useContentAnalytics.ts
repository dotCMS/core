import { useContext, useRef } from 'react';

import { DotAnalytics } from '../../dotAnalytics/shared/dot-content-analytics.model';
import { isInsideEditor } from '../../dotAnalytics/shared/dot-content-analytics.utils';
import DotContentAnalyticsContext from '../contexts/DotContentAnalyticsContext';

/**
 * Custom hook that handles analytics page view tracking.
 *
 * @example
 * ```tsx
 * function Button({ title, urlTitle }) {
 *   const { track } = useContentAnalytics();
 *
 *   // First parameter: custom event name to identify the action
 *   // Second parameter: object with properties you want to track
 *   return (
 *     <button onClick={() => track('btn-click', { title, urlTitle })}>
 *       See Details →
 *     </button>
 *   );
 * }
 * ```
 * @returns {DotContentAnalyticsCustomHook} - The analytics instance used to track page views
 */
export const useContentAnalytics = (): DotAnalytics => {
    const instance = useContext(DotContentAnalyticsContext);
    const lastPathRef = useRef<string | null>(null);

    if (!instance) {
        throw new Error('useContentAnalytics must be used within a DotContentAnalyticsProvider');
    }

    return {
        /**
         * Track a custom event.
         * @param {string} eventName - The name of the event to track.
         * @param {Record<string, unknown>} payload - The payload to track.
         */
        track: (eventName: string, payload: Record<string, unknown> = {}) => {
            if (!isInsideEditor()) {
                instance?.track(eventName, {
                    ...payload,
                    timestamp: new Date().toISOString()
                });
            }
        },

        /**
         * Track a page view.
         * @param {Record<string, unknown>} payload - The payload to track.
         */
        pageView: (payload: Record<string, unknown> = {}) => {
            if (!isInsideEditor()) {
                const currentPath = window.location.pathname;
                if (currentPath !== lastPathRef.current) {
                    lastPathRef.current = currentPath;
                    instance.pageView(payload);
                }
            }
        }
    };
};
