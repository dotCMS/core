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

    /** Notifies all subscribers of a click */
    private notifySubscribers(eventName: string, payload: DotCMSContentClickPayload): void {
        this.subscribers.forEach((callback) => callback(eventName, payload));
    }

    /**
     * Initialize click tracking
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
     */
    private attachClickListener(element: HTMLElement): void {
        if (this.trackedElements.has(element)) {
            const identifier = element.dataset.dotAnalyticsIdentifier || 'unknown';
            this.logger.debug(`Element ${identifier} already has listener, skipping`);
            return; // Already tracked
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
        this.logger.log(`✅ Attached listener to contentlet ${identifier}`, element);
    }

    /**
     * Find and attach listeners to all contentlet elements
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
     * Remove all click listeners
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
     * Cleanup: removes all listeners and disconnects observers
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
