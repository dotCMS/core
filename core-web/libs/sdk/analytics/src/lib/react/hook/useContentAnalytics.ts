import { useCallback, useRef } from 'react';

import { getUVEState } from '@dotcms/uve';

import {
    DotCMSAnalytics,
    DotCMSAnalyticsConfig
} from '../../core/shared/dot-content-analytics.model';
import { initializeAnalytics } from '../internal';

/**
 * Custom hook that handles analytics tracking for anonymous users.
 * Provides methods to track events and page views with automatic timestamp injection.
 * Automatically disables tracking when inside the UVE editor.
 *
 * @example
 * ```tsx
 * function Button({ title, urlTitle }) {
 *   const { track } = useContentAnalytics({
 *     server: 'https://demo.dotcms.com',
 *     siteAuth: 'my-site-auth',
 *     debug: false
 *   });
 *
 *   // Track button click with custom properties
 *   return (
 *     <button onClick={() => track('btn-click', { title, urlTitle })}>
 *       See Details →
 *     </button>
 *   );
 * }
 * ```
 *
 * @param {DotCMSAnalyticsConfig} config - Required configuration object for analytics initialization
 * @returns {DotCMSAnalytics} The analytics instance with tracking capabilities
 * @throws {Error} When analytics initialization fails due to invalid configuration
 */
export const useContentAnalytics = (config: DotCMSAnalyticsConfig): DotCMSAnalytics => {
    const instance = initializeAnalytics(config);
    const lastPathRef = useRef<string | null>(null);

    if (!instance) {
        throw new Error(
            'Failed to initialize DotContentAnalytics. Please verify the required configuration (server and siteAuth).'
        );
    }

    const track = useCallback(
        (eventName: string, payload: Record<string, unknown> = {}) => {
            if (!getUVEState()) {
                instance.track(eventName, {
                    ...payload,
                    timestamp: new Date().toISOString()
                });
            }
        },
        [instance]
    );

    const pageView = useCallback(() => {
        if (!getUVEState()) {
            const currentPath = window.location.pathname;
            if (currentPath !== lastPathRef.current) {
                lastPathRef.current = currentPath;
                instance.pageView();
            }
        }
    }, [instance]);

    return {
        track,
        pageView
    };
};
