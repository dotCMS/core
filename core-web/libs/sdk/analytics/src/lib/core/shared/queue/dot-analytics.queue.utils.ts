import smartQueue, { type Queue } from '@analytics/queue-utils';

import { DEFAULT_QUEUE_CONFIG } from '../constants';
import { sendAnalyticsEvent } from '../dot-content-analytics.http';
import {
    DotCMSAnalyticsConfig,
    DotCMSAnalyticsEventContext,
    DotCMSEvent,
    QueueConfig
} from '../models';

/**
 * Creates a queue manager for batching analytics events.
 * Uses factory function pattern consistent with the plugin architecture.
 */
export const createAnalyticsQueue = (config: DotCMSAnalyticsConfig) => {
    let eventQueue: Queue<DotCMSEvent> | null = null;
    let currentContext: DotCMSAnalyticsEventContext | null = null;

    /**
     * Whether to use keepalive mode for sending events
     * true for page unload (visibilitychange, pagehide), false for normal sends
     */
    let useKeepalive = false;

    // Merge user config with defaults (allows partial configuration)
    const queueConfig: QueueConfig = {
        ...DEFAULT_QUEUE_CONFIG,
        ...(typeof config.queue === 'object' ? config.queue : {})
    };

    /**
     * Send batch of events to server
     * Called by smartQueue - uses keepalive mode when flushing on page unload
     */
    const sendBatch = (events: DotCMSEvent[]): void => {
        if (!currentContext) return;

        if (config.debug) {
            // eslint-disable-next-line no-console
            console.log(`DotCMS Analytics Queue: Sending batch of ${events.length} event(s)`, {
                events,
                keepalive: useKeepalive
            });
        }

        const payload = { context: currentContext, events };
        sendAnalyticsEvent(payload, config, useKeepalive);
    };

    /**
     * Flush remaining events when page becomes hidden or unloads
     * Enables keepalive mode and triggers smartQueue to flush ALL events
     */
    const flushRemaining = (): void => {
        if (!eventQueue || eventQueue.size() === 0 || !currentContext) return;

        if (config.debug) {
            console.warn(
                `DotCMS Analytics: Flushing ${eventQueue.size()} events (page hidden/unload)`
            );
        }

        // Use keepalive mode for reliable delivery during page unload
        useKeepalive = true;

        // Flush all events - flush(true) makes smartQueue recursively batch until empty
        eventQueue.flush(true);
    };

    /**
     * Handle visibility change - flush when page becomes hidden
     * This is more reliable than beforeunload/unload, especially on mobile
     */
    const handleVisibilityChange = (): void => {
        if (document.visibilityState === 'hidden') {
            flushRemaining();
        }
    };

    return {
        /**
         * Initialize the queue with smart batching
         */
        initialize: (): void => {
            eventQueue = smartQueue(
                (items: DotCMSEvent[]) => {
                    sendBatch(items);
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

            if (config.debug) {
                // Calculate predicted size before push to show correct order in logs
                const predictedSize = eventQueue.size() + 1;
                const maxSize = queueConfig.eventBatchSize ?? DEFAULT_QUEUE_CONFIG.eventBatchSize;
                const willBeFull = predictedSize >= maxSize;
                // eslint-disable-next-line no-console
                console.log(
                    `DotCMS Analytics Queue: Event added. Queue size: ${predictedSize}/${maxSize}${willBeFull ? ' (full, sending...)' : ''}`,
                    { eventType: event.event_type, event }
                );
            }

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
            useKeepalive = false;
        }
    };
};
