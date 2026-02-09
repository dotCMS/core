import smartQueue, { type Queue } from '@analytics/queue-utils';
import onRouteChange from '@analytics/router-utils';

import {
    DEFAULT_QUEUE_CONFIG,
    MAX_EVENT_AGE_MS,
    QUEUE_STORAGE_KEY_PREFIX,
    TAB_ID_STORAGE_KEY
} from '../constants';
import { sendAnalyticsEvent } from '../http/dot-analytics.http';
import {
    DotCMSAnalyticsConfig,
    DotCMSAnalyticsEventContext,
    DotCMSEvent,
    PersistedQueue,
    QueueConfig
} from '../models';
import {
    createPluginLogger,
    generateSecureId,
    getAnalyticsContext,
    safeSessionStorage
} from '../utils/dot-analytics.utils';

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

    /**
     * Unique identifier for this browser tab
     * Used to prevent race conditions when multiple tabs write to sessionStorage
     */
    let tabId = '';

    /**
     * Parallel array to track events for persistence
     * smartQueue doesn't expose its internal array, so we maintain a copy
     */
    let eventsForPersistence: DotCMSEvent[] = [];

    // Merge user config with defaults (allows partial configuration)
    // After merge, queueConfig always has all required values
    const queueConfig: Required<QueueConfig> = {
        ...DEFAULT_QUEUE_CONFIG,
        ...(typeof config.queue === 'object' ? config.queue : {})
    };

    /**
     * Get storage key for this tab's queue
     */
    const getStorageKey = (): string => {
        return `${QUEUE_STORAGE_KEY_PREFIX}_${tabId}`;
    };

    /**
     * Validate persisted queue structure
     * @param data - Data from sessionStorage
     * @returns Validated queue or null if invalid
     */
    const validatePersistedQueue = (data: unknown): PersistedQueue | null => {
        const q = data as Partial<PersistedQueue>;

        if (!q || typeof q !== 'object' || Array.isArray(q)) {
            return null;
        }

        // Validate minimal structural requirements
        if (
            typeof q.tabId !== 'string' ||
            typeof q.timestamp !== 'number' ||
            !Number.isFinite(q.timestamp) ||
            !Array.isArray(q.events)
        ) {
            logger.warn('Invalid persisted queue: structural mismatch');
            return null;
        }

        // Validate minimal event structure
        const validEvents = q.events.filter(
            (event) => event && typeof event === 'object' && 'event_type' in event
        );

        return {
            tabId: q.tabId,
            timestamp: q.timestamp,
            events: validEvents
        };
    };

    /**
     * Load events from sessionStorage
     * @returns Persisted queue or null if not found/invalid
     */
    const loadFromStorage = (): PersistedQueue | null => {
        try {
            const key = getStorageKey();
            const stored = safeSessionStorage.getItem(key);

            if (!stored) {
                logger.debug('No persisted queue found');
                return null;
            }

            const parsed = JSON.parse(stored);
            const validated = validatePersistedQueue(parsed);

            if (!validated) {
                // Clear invalid storage
                safeSessionStorage.removeItem(key);
                return null;
            }

            // Check if events are too old (24 hours)
            const age = Date.now() - validated.timestamp;
            if (age > MAX_EVENT_AGE_MS) {
                logger.warn(
                    `Persisted events too old (${Math.round(age / 1000 / 60 / 60)}h), discarding`
                );
                safeSessionStorage.removeItem(key);
                return null;
            }

            logger.info(
                `Loaded ${validated.events.length} persisted event(s) from storage (age: ${Math.round(age / 1000)}s)`
            );

            return validated;
        } catch (error) {
            logger.error('Failed to load persisted queue', error);
            // Clear corrupted storage
            safeSessionStorage.removeItem(getStorageKey());
            return null;
        }
    };

    /**
     * Persist current queue to sessionStorage
     */
    const persistToStorage = (): void => {
        if (eventsForPersistence.length === 0) {
            return;
        }

        try {
            const key = getStorageKey();
            const queueData: PersistedQueue = {
                tabId,
                timestamp: Date.now(),
                events: eventsForPersistence
            };

            safeSessionStorage.setItem(key, JSON.stringify(queueData));
            logger.debug(`Persisted ${eventsForPersistence.length} event(s) to storage`);
        } catch (error) {
            if (error instanceof Error && error.name === 'QuotaExceededError') {
                logger.warn('sessionStorage quota exceeded, continuing without persistence');
            } else {
                logger.error('Failed to persist queue', error);
            }
        }
    };

    /**
     * Clear persisted queue from sessionStorage
     */
    const clearStorage = (): void => {
        try {
            const key = getStorageKey();
            safeSessionStorage.removeItem(key);
            logger.debug('Persisted queue cleared from storage');
        } catch (error) {
            logger.error('Failed to clear persisted queue', error);
        }
    };

    /**
     * Send events immediately with keepalive mode
     * Used for sending persisted events on page load
     * @param events - Events to send
     * @param useKeepalive - whether to use keepalive
     * @returns Promise<boolean> - true if success
     */
    const sendImmediately = async (
        events: DotCMSEvent[],
        useKeepalive = true
    ): Promise<boolean> => {
        if (events.length === 0) {
            return true;
        }

        logger.info(`Sending ${events.length} persisted event(s) immediately`);

        // Get current context (generates new one if needed)
        const context = getAnalyticsContext(config);
        const payload = { context, events };
        return sendAnalyticsEvent(payload, config, useKeepalive); // keepalive = useKeepalive
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

        // Send events (fire-and-forget with keepalive)
        sendAnalyticsEvent(payload, config, keepalive);

        // Remove sent events from parallel tracking array
        eventsForPersistence = eventsForPersistence.filter(
            (e) => !events.some((sent) => sent === e)
        );

        // Clear storage after normal flush (not keepalive)
        // For keepalive flushes (page unload), keep events in storage as backup
        if (!keepalive) {
            if (eventsForPersistence.length === 0) {
                clearStorage();
            } else {
                // Update storage with remaining events
                persistToStorage();
            }
        }
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
                logger.debug('Skipping flush (SPA navigation detected), persisting to storage');
                // For SPA navigation, persist events without flushing
                // Events stay in memory and are preserved across client-side routing
                persistToStorage();
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
            // 1. Load or generate persistent tab ID
            // Tab ID must remain constant across page navigations within the same browser tab
            if (typeof window !== 'undefined') {
                // Try to load existing tab ID from sessionStorage
                const storedTabId = safeSessionStorage.getItem(TAB_ID_STORAGE_KEY);

                if (storedTabId) {
                    // Reuse existing tab ID from previous page
                    tabId = storedTabId;
                    logger.debug(`Reusing Tab ID: ${tabId}`);
                } else {
                    // Generate new tab ID (Robust check for crypto.randomUUID)
                    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
                        tabId = crypto.randomUUID();
                    } else {
                        // Fallback for non-secure contexts or older browsers/environments
                        tabId = generateSecureId('tab');
                        logger.debug('crypto.randomUUID not available, using fallback generator');
                    }

                    try {
                        safeSessionStorage.setItem(TAB_ID_STORAGE_KEY, tabId);
                    } catch {
                        // Ignore storage error for tab ID, will just generate a new one on next load
                    }
                    logger.debug(`Generated new Tab ID: ${tabId}`);
                }
            }

            // 2. Load persisted events from previous page (if any)
            const persisted = loadFromStorage();
            if (persisted && persisted.events.length > 0) {
                // Send persisted events immediately WITHOUT keepalive (to confirm success)
                // This ensures events from previous page navigation are sent reliably
                sendImmediately(persisted.events, false).then((success) => {
                    if (success) {
                        // Clear storage ONLY after successful transmission
                        clearStorage();
                    } else {
                        logger.warn(
                            'Failed to send persisted events, keeping in storage for next retry'
                        );
                    }
                });
            }

            // 3. Initialize smartQueue for new events
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

            // 4. Setup page visibility and unload listeners
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

            // Add to parallel tracking array for persistence
            eventsForPersistence.push(event);

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

            // Persist to sessionStorage after enqueue
            persistToStorage();
        },

        /**
         * Get queue size for debugging
         * Returns the number of events in smartQueue
         */
        size: (): number => eventQueue?.size() ?? 0,

        /**
         * Clean up queue resources
         * Flushes remaining events and cleans up listeners
         *
         * IMPORTANT: Does NOT clear sessionStorage
         * - Storage is cleared only after sendBatch succeeds or in initialize()
         * - This allows events to persist across traditional page navigations
         */
        cleanup: (): void => {
            // Flush remaining events with keepalive
            flushRemaining();

            // Remove event listeners
            if (typeof window !== 'undefined' && typeof document !== 'undefined') {
                document.removeEventListener('visibilitychange', handleVisibilityChange);
                window.removeEventListener('pagehide', flushRemaining);
            }

            // Clean up memory only (not sessionStorage)
            eventQueue = null;
            currentContext = null;
            keepalive = false;
            eventsForPersistence = [];
        }
    };
};
