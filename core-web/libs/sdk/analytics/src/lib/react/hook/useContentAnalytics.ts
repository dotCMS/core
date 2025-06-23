import { useCallback, useContext, useRef } from 'react';

import { DotAnalytics } from '../../dotAnalytics/shared/dot-content-analytics.model';
import { isInsideEditor } from '../../dotAnalytics/shared/dot-content-analytics.utils';
import DotContentAnalyticsContext from '../contexts/DotContentAnalyticsContext';

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
 * @returns {DotAnalytics} - The analytics instance with tracking capabilities for anonymous users
 */
export const useContentAnalytics = (): DotAnalytics => {
    const instance = useContext(DotContentAnalyticsContext);
    const lastPathRef = useRef<string | null>(null);

    if (!instance) {
        throw new Error('useContentAnalytics must be used within a DotContentAnalyticsProvider');
    }

    const track = useCallback(
        (eventName: string, payload: Record<string, unknown> = {}) => {
            if (!isInsideEditor()) {
                instance?.track(eventName, {
                    ...payload,
                    timestamp: new Date().toISOString()
                });
            }
        },
        [instance]
    );

    const pageView = useCallback(
        (payload: Record<string, unknown> = {}) => {
            if (!isInsideEditor()) {
                const currentPath = window.location.pathname;
                if (currentPath !== lastPathRef.current) {
                    lastPathRef.current = currentPath;
                    instance.pageView(payload);
                }
            }
        },
        [instance]
    );

    return {
        track,
        pageView
    };
};
