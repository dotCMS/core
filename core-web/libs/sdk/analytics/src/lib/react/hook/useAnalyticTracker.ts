import { useContext } from 'react';

import { AnalyticsTracker } from '../../shared/dot-content-analytics.model';
import { isInsideEditor } from '../../shared/dot-content-analytics.utils';
import DotContentAnalyticsContext from '../contexts/DotContentAnalyticsContext';

/**
 * Hook to track analytics events in the application
 *
 * @returns {AnalyticsTracker} Object with track method to send events
 *
 * @example
 * ```tsx
 * const { track } = useAnalyticsTracker();
 *
 * const handleClick = () => {
 *   track("btn-click", {
 *     title: "My Title",
 *     buttonText: "Link to detail",
 *     urlTitle: "my-page"
 *   });
 * };
 *
 * return (
 *   <button onClick={handleClick}>
 *     Click me
 *   </button>
 * );
 * ```
 */
export const useAnalyticsTracker = (): AnalyticsTracker => {
    const instance = useContext(DotContentAnalyticsContext);
    const insideEditor = isInsideEditor();

    return {
        track: (eventName: string, payload = {}) => {
            if (instance?.track && !insideEditor) {
                instance.track(eventName, {
                    ...payload,
                    timestamp: new Date().toISOString()
                });
            }
        }
    };
};
