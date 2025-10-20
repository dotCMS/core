import { useSyncExternalStore } from 'react';

import { isAnalyticsActive } from '@dotcms/uve';

// Store para manejar suscriptores
const subscribers = new Set<() => void>();
let currentValue: boolean | null = null; // null = no inicializado

// Event listener para el evento de Analytics ready
function handleAnalyticsReady() {
    currentValue = true;
    // Notifica a todos los suscriptores
    subscribers.forEach((callback) => callback());
}

// Registra el listener una sola vez (a nivel de módulo)
if (typeof window !== 'undefined') {
    window.addEventListener('dotcms:analytics:ready', handleAnalyticsReady);
}

/**
 * @internal
 * A React hook that checks whether DotCMS Analytics is active.
 *
 * Uses React's useSyncExternalStore to subscribe to changes in the analytics state.
 * Components automatically re-render when analytics is initialized after initial mount.
 *
 * This hook listens to the 'dotcms:analytics:ready' custom event that is dispatched
 * when Analytics completes initialization. This event-driven approach ensures:
 * - No polling overhead
 * - Components work regardless of initialization order
 * - Instant notification when analytics becomes active
 *
 * @returns {boolean} True if analytics is active, false otherwise
 *
 * @example
 * ```tsx
 * const isAnalyticsActive = useIsAnalyticsActive()
 *
 * if (isAnalyticsActive) {
 *   // Analytics is active - add data attributes
 * }
 * ```
 */
export const useIsAnalyticsActive = (): boolean => {
    return useSyncExternalStore(
        // subscribe: Add callback to be notified when analytics becomes ready
        (callback) => {
            subscribers.add(callback);

            // Cleanup: remove subscriber
            return () => {
                subscribers.delete(callback);
            };
        },
        // getSnapshot: Get current analytics state (client-side)
        () => {
            // Initialize if necessary (in case analytics was ready before hook mounted)
            if (currentValue === null && typeof window !== 'undefined') {
                currentValue = isAnalyticsActive();
            }
            return currentValue ?? false;
        },
        // getServerSnapshot: Always false during SSR
        () => false
    );
};
