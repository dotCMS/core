import { useCallback, useRef } from 'react';

import { getUVEState } from '@dotcms/uve';

import { DotCMSAnalytics, DotCMSAnalyticsConfig } from '../../dotAnalytics/shared/dot-content-analytics.model';
import { initializeAnalytics } from '../internal';

/**
 * Custom hook that handles analytics tracking for anonymous users.
 *
 * @example
 * ```tsx
 * function Button({ title, urlTitle }) {
 *   const { track } = useContentAnalytics();
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
 * @example
 * ```tsx
 * // Session debugging example
 * function AnalyticsDebugComponent() {
 *   const { getAnonymousUserId, getSessionInfo, updateSessionActivity } = useContentAnalytics();
 *
 *   const handleManualActivity = () => {
 *     updateSessionActivity();
 *     // Manual activity updated
 *   };
 *
 *   // Debug session info in development
 *   const debugInfo = () => {
 *     if (process.env.NODE_ENV === 'development') {
 *       console.log('Anonymous ID:', getAnonymousUserId());
 *       console.log('Session info:', getSessionInfo());
 *     }
 *   };
 *
 *   return (
 *     <div>
 *       <button onClick={handleManualActivity}>Update Activity</button>
 *       <button onClick={debugInfo}>Debug Session</button>
 *       <p>User ID: {getAnonymousUserId()}</p>
 *     </div>
 *   );
 * }
 * ```
 *
 * @returns {DotCMSAnalytics} - The analytics instance with tracking capabilities for anonymous users
 * @throws {Error} - Throws error if used outside of DotContentAnalyticsProvider or if analytics failed to initialize
 */
/**
 * Hook to access analytics tracking APIs without a Provider.
 * - Relies on env-driven singleton initialization.
 * - Adds timestamp to track payloads and de-duplicates pageView per path.
 *
 * @throws Error when env config is missing and analytics is not initialized.
 */
export const useContentAnalytics = (config: DotCMSAnalyticsConfig): DotCMSAnalytics => {
    const instance = initializeAnalytics(config);
    const lastPathRef = useRef<string | null>(null);

    if (!instance) {
        throw new Error(
            'useContentAnalytics: analytics not initialized. Ensure NEXT_PUBLIC_DOTCMS_HOST and NEXT_PUBLIC_DOTCMS_ANALYTICS_SITE_KEY are set.'
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
