import { AnalyticsInstance } from 'analytics';

import { handleDocumentClick, trackClick } from './dot-analytics.click.utils';

import { DotCMSAnalyticsConfig } from '../../shared/models';
import {
    createContentletObserver,
    createPluginLogger,
    findContentlets,
    INITIAL_SCAN_DELAY_MS,
    isBrowser
} from '../../shared/utils/dot-analytics.utils';

/**
 * Tracks content clicks using event listeners on contentlet containers.
 * Detects clicks on <a> and <button> elements inside contentlets and fires events.
 */
export class DotCMSClickTracker {
    private instance: AnalyticsInstance | null = null;
    private mutationObserver: MutationObserver | null = null;
    private lastClickTime = { value: 0 };
    private logger: ReturnType<typeof createPluginLogger>;

    // Track which elements already have listeners to avoid duplicates
    private trackedElements = new WeakSet<HTMLElement>();
    // Store handlers for cleanup
    private elementHandlers = new WeakMap<HTMLElement, (event: MouseEvent) => void>();

    constructor(private config: DotCMSAnalyticsConfig) {
        this.logger = createPluginLogger('Click', config);
    }

    /**
     * Initialize click tracking with analytics instance
     * @param instance - Analytics.js instance to use for tracking
     */
    public initialize(instance: AnalyticsInstance): void {
        if (!isBrowser()) {
            this.logger.warn('No document, skipping');
            return;
        }

        // Store instance for use in click handlers
        this.instance = instance;

        this.logger.debug(`Plugin initializing, instance assigned: ${!!this.instance}`);

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

        if (!this.instance) {
            this.logger.warn('Instance not ready, cannot attach listener');
            return; // Instance not ready yet
        }

        const currentInstance = this.instance;
        const clickHandler = (event: MouseEvent) => {
            this.logger.debug('Click handler triggered on contentlet');
            handleDocumentClick(
                event,
                (eventName, payload) => {
                    trackClick(
                        eventName,
                        payload,
                        currentInstance,
                        this.config,
                        this.lastClickTime
                    );
                },
                this.config.debug
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
        this.logger.debug(`findAndAttachListeners called, instance=${!!this.instance}`);

        if (!this.instance) {
            this.logger.error('❌ Instance is null, cannot attach listeners!');
            return;
        }

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
