import { useCallback, useMemo } from 'react';

import { getUVEState } from '@dotcms/uve';

import { DotCMSAnalytics, DotCMSAnalyticsConfig } from '../../core/shared/models';
import { initializeAnalytics } from '../internal';

/**
 * Custom hook that handles analytics tracking for anonymous users.
 * Provides methods to track events and page views.
 *
 * **UVE Editor Behavior:**
 * - Automatically disables ALL tracking when inside the Universal Visual Editor (UVE)
 * - This prevents editor interactions and preview activities from polluting analytics data
 * - UVE detection is memoized for performance - checked once per component lifecycle
 *
 * **Performance:**
 * - Uses a singleton pattern - all components with the same config share the same analytics instance
 * - Only re-initializes when server or siteKey changes (not debug flag)
 * - UVE state check is memoized to avoid repeated function calls
 *
 * @example
 * ```tsx
 * function Button({ title, urlTitle }) {
 *   const { track, pageView } = useContentAnalytics({
 *     server: 'https://demo.dotcms.com',
 *     siteAuth: 'my-site-auth',
 *     debug: false
 *   });
 *
 *   // Track button click - automatically skipped in UVE editor
 *   const handleClick = () => {
 *     track('btn-click', { title, urlTitle });
 *   };
 *
 *   // Track page view - also skipped in UVE editor
 *   useEffect(() => {
 *     pageView({ page: title });
 *   }, [title, pageView]);
 *
 *   return <button onClick={handleClick}>See Details →</button>;
 * }
 * ```
 *
 * @param {DotCMSAnalyticsConfig} config - Required configuration object for analytics initialization
 * @returns {DotCMSAnalytics} The analytics instance with tracking capabilities
 * @throws {Error} When analytics initialization fails due to invalid configuration
 */
export const useContentAnalytics = (config: DotCMSAnalyticsConfig): DotCMSAnalytics => {
    // Memoize instance based on server and siteKey (the critical config values)
    // Only re-initialize if these change
    const instance = useMemo(
        () => initializeAnalytics(config),
        [config.server, config.siteKey]
    );

    // Memoize UVE state check to avoid repeated calls
    // UVE state is determined by URL params and window context, so it's stable during component lifecycle
    const isInUVE = useMemo(() => {
        return Boolean(getUVEState());
    }, []);

    if (!instance) {
        throw new Error(
            'Failed to initialize DotContentAnalytics. Please verify the required configuration (server and siteAuth).'
        );
    }

    const track = useCallback(
        (eventName: string, payload: Record<string, unknown> = {}) => {
            // Skip analytics tracking when inside UVE editor to avoid polluting analytics data
            // with editor interactions and preview activities
            if (isInUVE) {
                return;
            }

            instance.track(eventName, payload);
        },
        [instance, isInUVE]
    );

    const pageView = useCallback(
        (payload: Record<string, unknown> = {}) => {
            // Skip analytics tracking when inside UVE editor to avoid polluting analytics data
            // with editor interactions and preview activities
            if (isInUVE) {
                return;
            }

            instance.pageView(payload);
        },
        [instance, isInUVE]
    );

    return {
        track,
        pageView
    };
};
