import { useEffect } from 'react';

// import { isInsideEditor } from '@dotcms/client';
import { DotContentAnalytics } from '../../dot-content-analytics';

/**
 * Custom hook `useContentAnalytics`.
 *
 * This hook is designed to handle changes in the location of the current DotContentAnalytics
 * instance and track page views when the location changes.
 *
 * @returns {void}
 */
export const useContentAnalytics = (instance: DotContentAnalytics | null): void => {
    /**
     * This `useEffect` hook is responsible for tracking location changes when not inside an editor environment, and invoking the
     * `locationChanged` method from the analytics instance with current location.
     */
    useEffect(() => {
        if (!instance || typeof document === 'undefined') {
            return;
        }

        // const insideEditor = isInsideEditor();
        const insideEditor = false;

        if (!insideEditor) {
            const location = typeof window !== 'undefined' ? window.location : undefined;

            if (instance && location) {
                instance.trackPageView();
            }
        }
    }, [instance]);
};
