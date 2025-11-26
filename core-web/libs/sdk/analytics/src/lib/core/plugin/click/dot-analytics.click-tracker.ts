import { handleContentletClick } from './dot-analytics.click.utils';

import { DEFAULT_CLICK_THROTTLE_MS } from '../../shared/constants/dot-analytics.constants';
import { DotCMSAnalyticsConfig, DotCMSContentClickPayload } from '../../shared/models';
import {
    createContentletObserver,
    createPluginLogger,
    findContentlets,
    INITIAL_SCAN_DELAY_MS,
    isBrowser
} from '../../shared/utils/dot-analytics.utils';

/** Callback function for click events */
export type ClickCallback = (eventName: string, payload: DotCMSContentClickPayload) => void;

/** Subscription object with unsubscribe method */
export interface ClickSubscription {
    unsubscribe: () => void;
}

/**
 * Tracks content clicks using event listeners on contentlet containers.
 * Detects clicks on <a> and <button> elements inside contentlets and fires events.
 *
 * Features:
 * - Attaches event listeners to contentlet containers
 * - Tracks clicks on anchor and button elements only
 * - Uses MutationObserver to detect dynamically added content
 * - Throttles rapid clicks to prevent duplicates (300ms)
 * - Subscription-based event system for decoupling
 *
 * @example
 * ```typescript
 * const tracker = new DotCMSClickTracker(config);
 * const subscription = tracker.onClick((eventName, payload) => {
 *   console.log('Click detected:', payload);
 * });
 * tracker.initialize();
 * // Later: subscription.unsubscribe();
 * ```
 */
export class DotCMSClickTracker {
    private mutationObserver: MutationObserver | null = null;
    private lastClickTime = { value: 0 };
    private logger: ReturnType<typeof createPluginLogger>;
    private subscribers = new Set<ClickCallback>();

    // Track which elements already have listeners to avoid duplicates
    private trackedElements = new WeakSet<HTMLElement>();
    // Store handlers for cleanup
    private elementHandlers = new WeakMap<HTMLElement, (event: MouseEvent) => void>();

    constructor(private config: DotCMSAnalyticsConfig) {
        this.logger = createPluginLogger('Click', config);
    }

    /**
     * Subscribe to click events
     * @param callback - Function called when click is detected
     * @returns Subscription object with unsubscribe method
     */
    public onClick(callback: ClickCallback): ClickSubscription {
        this.subscribers.add(callback);

        return {
            unsubscribe: () => {
                this.subscribers.delete(callback);
            }
        };
    }

    /**
     * Notifies all subscribers of a click event
     * @param eventName - Name of the event (e.g., 'content_click')
     * @param payload - Click event payload with content and element data
     */
    private notifySubscribers(eventName: string, payload: DotCMSContentClickPayload): void {
        this.subscribers.forEach((callback) => callback(eventName, payload));
    }

    /**
     * Initialize click tracking system
     *
     * Performs the following:
     * - Validates browser environment
     * - Scans for existing contentlets after a delay (100ms)
     * - Sets up MutationObserver for dynamic content
     *
     * The delay allows React/Next.js to finish initial rendering
     * before attaching listeners.
     */
    public initialize(): void {
        if (!isBrowser()) {
            this.logger.warn('No document, skipping');
            return;
        }

        this.logger.debug('Plugin initializing');

        // Wait for DOM to be ready before scanning
        if (typeof window !== 'undefined') {
            // Use setTimeout to let React/Next.js finish rendering
            setTimeout(() => {
                this.logger.debug('Running initial scan after timeout...');
                // Initial scan for existing contentlets
                this.findAndAttachListeners();
            }, INITIAL_SCAN_DELAY_MS);
        }

        // Setup observer for dynamic content
        this.initializeMutationObserver();

        this.logger.info('Plugin initialized');
    }

    /**
     * Attach click listener to a contentlet container
     *
     * Skips if element already has a listener attached.
     * The listener delegates to handleContentletClick which:
     * - Finds clicked anchor/button elements
     * - Extracts contentlet and element data
     * - Applies throttling (300ms)
     * - Notifies subscribers
     *
     * @param element - Contentlet container element to track
     */
    private attachClickListener(element: HTMLElement): void {
        if (this.trackedElements.has(element)) {
            const identifier = element.dataset.dotAnalyticsIdentifier || 'unknown';
            this.logger.debug(`Element ${identifier} already has listener, skipping`);
            return; // Already tracked
        }

        // Cache DOM index as data-attribute to avoid O(3n) query on each click
        if (!element.dataset.dotAnalyticsDomIndex) {
            const allContentlets = findContentlets();
            element.dataset.dotAnalyticsDomIndex = String(allContentlets.indexOf(element));
        }

        const clickHandler = (event: MouseEvent) => {
            this.logger.debug('Click handler triggered on contentlet');

            // Pass the contentlet element directly - we already have it!
            handleContentletClick(
                event,
                element,
                (eventName, payload) => {
                    // Apply throttling
                    const now = Date.now();
                    if (now - this.lastClickTime.value < DEFAULT_CLICK_THROTTLE_MS) {
                        return;
                    }
                    this.lastClickTime.value = now;

                    // Notify subscribers
                    this.notifySubscribers(eventName, payload);

                    // Debug logging
                    this.logger.info(
                        `Fired click event for ${payload.content.identifier}`,
                        payload
                    );
                },
                this.logger
            );
        };

        element.addEventListener('click', clickHandler);
        this.trackedElements.add(element);
        this.elementHandlers.set(element, clickHandler);

        const identifier = element.dataset.dotAnalyticsIdentifier || 'unknown';
        this.logger.log(`Attached listener to contentlet ${identifier}`, element);
    }

    /**
     * Find and attach listeners to all contentlet elements in the DOM
     *
     * Scans the entire document for elements with the
     * `.dotcms-analytics-contentlet` class and attaches click
     * listeners if not already tracked.
     *
     * Called during initialization and whenever DOM mutations are detected.
     */
    private findAndAttachListeners(): void {
        this.logger.debug('findAndAttachListeners called');

        const contentlets = findContentlets();

        this.logger.debug(`Scanning... found ${contentlets.length} contentlets`);

        let attached = 0;
        contentlets.forEach((element) => {
            const wasNew = !this.trackedElements.has(element);
            this.attachClickListener(element);
            if (wasNew && this.trackedElements.has(element)) {
                attached++;
            }
        });

        if (attached > 0) {
            this.logger.info(`Attached ${attached} new click listeners`);
        }
    }

    /**
     * Initialize MutationObserver to detect new contentlet containers
     * Uses same simple strategy as impression tracker - no complex filtering
     */
    private initializeMutationObserver(): void {
        if (!isBrowser()) {
            return;
        }

        this.mutationObserver = createContentletObserver(() => {
            this.findAndAttachListeners();
        });

        this.logger.info('MutationObserver enabled for click tracking');
    }

    /**
     * Remove all click listeners from tracked contentlets
     *
     * Iterates through all contentlet elements and removes their
     * click event handlers, cleaning up WeakMap references.
     */
    private removeAllListeners(): void {
        const contentlets = findContentlets();

        contentlets.forEach((element) => {
            const handler = this.elementHandlers.get(element);
            if (handler) {
                element.removeEventListener('click', handler);
                this.elementHandlers.delete(element);
            }
        });
    }

    /**
     * Cleanup all resources used by the click tracker
     *
     * Performs:
     * - Removes all event listeners from contentlets
     * - Disconnects MutationObserver
     * - Clears internal references
     *
     * Should be called when the plugin is disabled or on page unload.
     */
    public cleanup(): void {
        this.removeAllListeners();

        if (this.mutationObserver) {
            this.mutationObserver.disconnect();
            this.mutationObserver = null;
        }

        this.logger.info('Click tracking cleaned up');
    }
}
