import smartQueue, { type Queue } from '@analytics/queue-utils';
import onRouteChange from '@analytics/router-utils';

import { DEFAULT_QUEUE_CONFIG } from '../constants';
import { sendAnalyticsEvent } from '../http/dot-analytics.http';
import {
    DotCMSAnalyticsConfig,
    DotCMSAnalyticsEventContext,
    DotCMSEvent,
    QueueConfig
} from '../models';
import { createPluginLogger } from '../utils/dot-analytics.utils';

/**
 * Creates a queue manager for batching analytics events.
 * Uses factory function pattern consistent with the plugin architecture.
 */
export const createAnalyticsQueue = (config: DotCMSAnalyticsConfig) => {
    const logger = createPluginLogger('Queue', config);
    let eventQueue: Queue<DotCMSEvent> | null = null;
    let currentContext: DotCMSAnalyticsEventContext | null = null;

    /**
     * Whether to use keepalive mode for sending events
     * true for page unload (visibilitychange, pagehide), false for normal sends
     */
    let keepalive = false;

    /**
     * Track if we're in a SPA navigation to avoid unnecessary flushes
     */
    let isSPANavigation = false;
    let currentPath = typeof window !== 'undefined' ? window.location.pathname : '';

    // Merge user config with defaults (allows partial configuration)
    // After merge, queueConfig always has all required values
    const queueConfig: Required<QueueConfig> = {
        ...DEFAULT_QUEUE_CONFIG,
        ...(typeof config.queue === 'object' ? config.queue : {})
    };

    /**
     * Send batch of events to server
     * Called by smartQueue - uses keepalive mode when flushing on page unload
     * @param events - The batch of events to send
     * @param _rest - Remaining events in queue (unused, required by smartQueue API)
     */
    const sendBatch = (events: DotCMSEvent[], _rest: DotCMSEvent[]): void => {
        if (!currentContext) return;

        logger.debug(`Sending batch of ${events.length} event(s)`, {
            events,
            keepalive
        });

        const payload = { context: currentContext, events };
        sendAnalyticsEvent(payload, config, keepalive);
    };

    /**
     * Flush remaining events when page becomes hidden or unloads
     * Enables keepalive mode and triggers smartQueue to flush ALL events
     */
    const flushRemaining = (): void => {
        if (!eventQueue || eventQueue.size() === 0 || !currentContext) return;

        logger.info(`Flushing ${eventQueue.size()} events (page hidden/unload)`);

        // Use keepalive mode for reliable delivery during page unload
        keepalive = true;

        // Flush all events - flush(true) makes smartQueue recursively batch until empty
        eventQueue.flush(true);
    };

    /**
     * Handle visibility change - flush when page becomes hidden
     * This is more reliable than beforeunload/unload, especially on mobile
     *
     * IMPORTANT: We skip flush if this is a SPA navigation (client-side routing)
     * Only flush on real tab changes or browser close
     */
    const handleVisibilityChange = (): void => {
        logger.debug('handleVisibilityChange', document.visibilityState);

        if (document.visibilityState === 'hidden') {
            // Don't flush if this is just a SPA navigation
            if (isSPANavigation) {
                logger.debug('Skipping flush (SPA navigation detected)');
                return;
            }

            // Real visibility change (tab switch, browser close, etc.)
            flushRemaining();
        } else if (document.visibilityState === 'visible') {
            // Reset SPA navigation flag when page becomes visible again
            isSPANavigation = false;
        }
    };

    return {
        /**
         * Initialize the queue with smart batching
         */
        initialize: (): void => {
            eventQueue = smartQueue(
                (items: DotCMSEvent[], rest: DotCMSEvent[]) => {
                    sendBatch(items, rest);
                },
                {
                    max: queueConfig.eventBatchSize,
                    interval: queueConfig.flushInterval,
                    throttle: false // Always false - enables both batch size and interval triggers
                }
            );

            // Setup page visibility and unload listeners
            // visibilitychange is more reliable than beforeunload, especially on mobile
            if (typeof window !== 'undefined' && typeof document !== 'undefined') {
                document.addEventListener('visibilitychange', handleVisibilityChange);
                // pagehide as fallback for browsers without bfcache or older browsers
                window.addEventListener('pagehide', flushRemaining);

                // Detect SPA navigations using @analytics/router-utils
                // Handles Next.js, React Router, Vue Router, etc.
                onRouteChange((newPath: string) => {
                    isSPANavigation = true;
                    currentPath = newPath;
                    logger.debug(`SPA navigation detected (${currentPath})`);

                    // Reset flag after a short delay (SPA navigation is done)
                    setTimeout(() => {
                        isSPANavigation = false;
                    }, 100);
                });
            }
        },

        /**
         * Add event to queue
         * smartQueue handles all batching logic automatically:
         * - Sends immediately when eventBatchSize reached (with throttle: false)
         * - Sends pending events every flushInterval
         */
        enqueue: (event: DotCMSEvent, context: DotCMSAnalyticsEventContext): void => {
            currentContext = context;
            if (!eventQueue) return;

            // Calculate predicted size before push to show correct order in logs
            const predictedSize = eventQueue.size() + 1;
            const maxSize = queueConfig.eventBatchSize;
            const willBeFull = predictedSize >= maxSize;

            logger.debug(
                `Event added. Queue size: ${predictedSize}/${maxSize}${willBeFull ? ' (full, sending...)' : ''}`,
                { eventType: event.event_type, event }
            );

            // Push triggers sendBatch callback if queue is full (throttle: false)
            eventQueue.push(event);
        },

        /**
         * Get queue size for debugging
         * Returns the number of events in smartQueue
         */
        size: (): number => eventQueue?.size() ?? 0,

        /**
         * Clean up queue resources
         * Flushes remaining events and cleans up listeners
         */
        cleanup: (): void => {
            flushRemaining();

            // Remove event listeners
            if (typeof window !== 'undefined' && typeof document !== 'undefined') {
                document.removeEventListener('visibilitychange', handleVisibilityChange);
                window.removeEventListener('pagehide', flushRemaining);
            }

            eventQueue = null;
            currentContext = null;
            keepalive = false;
        }
    };
};
