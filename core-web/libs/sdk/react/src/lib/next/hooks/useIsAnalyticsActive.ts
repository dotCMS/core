import { useSyncExternalStore } from 'react';

import { isAnalyticsActive } from '@dotcms/uve';

// Subscriber store and state cache
const subscribers = new Set<() => void>();
let currentValue: boolean | null = null;
let isInitialized = false;

// Event handlers for analytics lifecycle
function handleAnalyticsReady() {
    currentValue = true;
    subscribers.forEach((callback) => callback());
}

function handleAnalyticsCleanup() {
    currentValue = null;
    subscribers.forEach((callback) => callback());
}

// Lazy initialization: only set up event listeners when first hook is used
function initializeAnalyticsListeners() {
    if (isInitialized || typeof window === 'undefined') {
        return;
    }

    window.addEventListener('dotcms:analytics:ready', handleAnalyticsReady);
    window.addEventListener('dotcms:analytics:cleanup', handleAnalyticsCleanup);
    isInitialized = true;
}

/**
 * @internal
 * React hook that checks whether DotCMS Analytics is active.
 *
 * Uses useSyncExternalStore to subscribe to analytics state changes via custom events:
 * - `dotcms:analytics:ready`: Fired when Analytics initializes
 * - `dotcms:analytics:cleanup`: Fired on page unload
 *
 * Components automatically re-render when analytics state changes. Works regardless
 * of initialization order and returns false during SSR.
 *
 * @returns {boolean} True if analytics is active, false otherwise
 *
 * @example
 * ```tsx
 * function Contentlet({ item }) {
 *   const isAnalyticsActive = useIsAnalyticsActive()
 *
 *   const attrs = isAnalyticsActive
 *     ? { 'data-dot-analytics-id': item.id }
 *     : {}
 *
 *   return <div {...attrs}>{item.title}</div>
 * }
 * ```
 */
export const useIsAnalyticsActive = (): boolean => {
    return useSyncExternalStore(
        // Subscribe: register callback for state changes
        (callback) => {
            initializeAnalyticsListeners(); // Lazy init on first subscription
            subscribers.add(callback);
            return () => subscribers.delete(callback);
        },
        // Get snapshot (client): return current analytics state
        () => {
            if (currentValue === null && typeof window !== 'undefined') {
                currentValue = isAnalyticsActive();
            }
            return currentValue ?? false;
        },
        // Get server snapshot (SSR): always false
        () => false
    );
};
