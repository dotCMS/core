import { useEffect } from 'react';

import { DotContentAnalytics } from '../../dot-content-analytics';
import { isInsideEditor } from '../../shared/dot-content-analytics.utils';

/**
 * Custom hook that handles analytics page view tracking.
 *
 * @param {DotContentAnalytics | null} instance - The analytics instance used to track page views
 * @returns {void}
 *
 */
export const useContentAnalytics = (instance: DotContentAnalytics | null): void => {
    /**
     * Tracks page view when component mounts, but only if:
     * - We have a valid analytics instance
     * - We're in a browser environment
     * - We're not inside the editor
     */
    useEffect(() => {
        if (!instance || typeof window === 'undefined') {
            return;
        }

        const insideEditor = isInsideEditor();

        if (!insideEditor) {
            instance.pageView({ source: 'headless' });
        }
    }, [instance]);
};
