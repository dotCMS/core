import { useEffect, useRef } from 'react';

import { DotCMSAnalytics } from '../../dotAnalytics/shared/dot-content-analytics.model';
import { isInsideEditor } from '../../dotAnalytics/shared/dot-content-analytics.utils';

/**
 * Internal custom hook that handles analytics page view tracking.
 *
 * @param {DotCMSAnalytics | null} instance - The analytics instance used to track page views
 * @returns {void}
 *
 */
export function useRouterTracker(analytics: DotCMSAnalytics | null) {
    const lastPathRef = useRef<string | null>(null);

    useEffect(() => {
        if (!analytics) return;

        function handleRouteChange() {
            const currentPath = window.location.pathname;
            if (currentPath !== lastPathRef.current && !isInsideEditor() && analytics) {
                lastPathRef.current = currentPath;
                analytics.pageView();
            }
        }

        // Track initial page view
        handleRouteChange();

        // Listen for navigation events
        window.addEventListener('popstate', handleRouteChange);
        window.addEventListener('beforeunload', handleRouteChange);

        return () => {
            window.removeEventListener('popstate', handleRouteChange);
            window.removeEventListener('beforeunload', handleRouteChange);
        };
    }, [analytics]);
}
