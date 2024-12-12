import { useEffect, useRef } from 'react';

import { DotContentAnalytics } from '../../dotAnalytics/dot-content-analytics';

/**
 * Internal custom hook that handles analytics page view tracking.
 *
 * @param {DotContentAnalytics | null} instance - The analytics instance used to track page views
 * @returns {void}
 *
 */
export function useRouteTracker(analytics: DotContentAnalytics | null) {
    const lastPathRef = useRef<string | null>(null);

    useEffect(() => {
        if (!analytics) return;

        const currentPath = window.location.pathname;

        if (currentPath !== lastPathRef.current) {
            lastPathRef.current = currentPath;
            analytics.pageView();
        }
    }, [analytics]);
}
