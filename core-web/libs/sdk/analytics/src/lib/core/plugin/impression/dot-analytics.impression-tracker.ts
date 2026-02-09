import { getUVEState } from '@dotcms/uve';

import {
    getViewportMetrics,
    isElementMeetingVisibilityThreshold
} from './dot-analytics.impression.utils';

import { DEFAULT_IMPRESSION_CONFIG, IMPRESSION_EVENT_TYPE } from '../../shared/constants';
import {
    DotCMSAnalyticsConfig,
    DotCMSContentImpressionPayload,
    ImpressionConfig
} from '../../shared/models';
import {
    createContentletObserver,
    createPluginLogger,
    extractContentletData,
    extractContentletIdentifier,
    findContentlets,
    INITIAL_SCAN_DELAY_MS,
    isBrowser
} from '../../shared/utils/dot-analytics.utils';

/** Tracks the state of an element's impression (timer, visibility, tracked status) */
interface ImpressionState {
    timer: number | null;
    visibleSince: number | null;
    tracked: boolean;
    element: HTMLElement | null;
}

/** Callback function for impression events */
export type ImpressionCallback = (
    eventName: string,
    payload: DotCMSContentImpressionPayload
) => void;

/** Subscription object with unsubscribe method */
export interface ImpressionSubscription {
    unsubscribe: () => void;
}

/**
 * Tracks content impressions using IntersectionObserver and dwell time.
 * Emits events through subscriptions when impressions are detected.
 */
export class DotCMSImpressionTracker {
    private observer: IntersectionObserver | null = null;
    private mutationObserver: MutationObserver | null = null;
    private elementImpressionStates = new Map<string, ImpressionState>();
    private sessionTrackedImpressions = new Set<string>();
    private impressionConfig: Required<ImpressionConfig>;
    private currentPagePath = '';
    private subscribers = new Set<ImpressionCallback>();
    private logger: ReturnType<typeof createPluginLogger>;

    constructor(config: DotCMSAnalyticsConfig) {
        this.logger = createPluginLogger('Impression', config);
        this.impressionConfig = this.resolveImpressionConfig(config.impressions);
    }

    /**
     * Subscribe to impression events
     * @param callback - Function called when impression is detected
     * @returns Subscription object with unsubscribe method
     */
    public onImpression(callback: ImpressionCallback): ImpressionSubscription {
        this.subscribers.add(callback);

        return {
            unsubscribe: () => {
                this.subscribers.delete(callback);
            }
        };
    }

    /** Notifies all subscribers of an impression */
    private notifySubscribers(eventName: string, payload: DotCMSContentImpressionPayload): void {
        this.subscribers.forEach((callback) => {
            try {
                callback(eventName, payload);
            } catch (error) {
                this.logger.error('Error in impression subscriber:', error);
            }
        });
    }

    /** Merges user config with defaults */
    private resolveImpressionConfig(
        userConfig: ImpressionConfig | boolean | undefined
    ): Required<ImpressionConfig> {
        // If boolean/undefined, use defaults
        if (typeof userConfig !== 'object' || userConfig === null) {
            return { ...DEFAULT_IMPRESSION_CONFIG };
        }

        // Merge user config with defaults
        return {
            ...DEFAULT_IMPRESSION_CONFIG,
            ...userConfig
        };
    }

    /** Initializes tracking: sets up observers, finds contentlets, handles visibility/navigation */
    public initialize(): void {
        // Early return if SSR
        if (!isBrowser()) {
            return;
        }

        // Early return if in editor mode (check for UVE markers)
        if (getUVEState()) {
            this.logger.warn('Impression tracking disabled in editor mode');
            return;
        }

        // Setup IntersectionObserver
        this.initializeIntersectionObserver();

        // Wait for DOM to be ready before scanning
        // The delay allows React/Next.js to finish initial rendering
        // before searching for contentlet elements
        if (typeof window !== 'undefined') {
            setTimeout(() => {
                this.logger.debug('Running initial scan after timeout...');
                // Find and observe contentlet elements
                this.findAndObserveContentletElements();
            }, INITIAL_SCAN_DELAY_MS);
        }

        // Setup mutation observer for dynamic content
        this.initializeDynamicContentDetector();

        // Listen for visibility changes to pause/resume tracking
        this.initializePageVisibilityHandler();

        // Listen for page navigation to reset tracking
        this.initializePageNavigationHandler();

        this.logger.info('Impression tracking initialized with config:', this.impressionConfig);
    }

    /** Sets up IntersectionObserver with configured visibility threshold */
    private initializeIntersectionObserver(): void {
        const options: IntersectionObserverInit = {
            root: null, // Use viewport as root
            rootMargin: '0px',
            threshold: this.impressionConfig.visibilityThreshold
        };

        this.observer = new IntersectionObserver((entries) => {
            this.processIntersectionChanges(entries);
        }, options);
    }

    /** Finds contentlets in DOM, validates them, and starts observing (respects maxNodes limit) */
    private findAndObserveContentletElements(): void {
        if (!this.observer) return;

        const contentlets = findContentlets();

        if (contentlets.length === 0) {
            this.logger.warn('No contentlets found to track');
            return;
        }

        // Limit to maxNodes for performance
        const maxNodesToTrack = Math.min(contentlets.length, this.impressionConfig.maxNodes);
        let observedCount = 0;

        for (let i = 0; i < maxNodesToTrack; i++) {
            const element = contentlets[i];
            const identifier = extractContentletIdentifier(element);

            if (identifier) {
                // Skip elements that shouldn't be tracked
                const skipReason = this.shouldSkipElement(element);
                if (skipReason) {
                    this.logger.debug(`Skipping element ${identifier} (${skipReason})`);
                    continue;
                }

                // Only observe and initialize if this is a new element
                if (!this.elementImpressionStates.has(identifier)) {
                    if (!element.dataset.dotAnalyticsDomIndex) {
                        element.dataset.dotAnalyticsDomIndex = String(i);
                    }

                    this.observer.observe(element);
                    this.elementImpressionStates.set(identifier, {
                        timer: null,
                        visibleSince: null,
                        tracked: this.hasBeenTrackedInSession(identifier),
                        element
                    });

                    observedCount++;
                }
            }
        }

        this.logger.info(`Observing ${observedCount} contentlets`);
        if (contentlets.length > maxNodesToTrack) {
            this.logger.warn(
                `${contentlets.length - maxNodesToTrack} contentlets not tracked (maxNodes limit: ${this.impressionConfig.maxNodes})`
            );
        }
    }

    /** Watches for new contentlets added to DOM (debounced for performance) */
    private initializeDynamicContentDetector(): void {
        if (!isBrowser()) {
            return;
        }

        this.mutationObserver = createContentletObserver(() => {
            this.findAndObserveContentletElements();
        });

        this.logger.info('MutationObserver enabled for dynamic content detection');
    }

    /** Cancels all timers when page is hidden (prevents false impressions) */
    private initializePageVisibilityHandler(): void {
        document.addEventListener('visibilitychange', () => {
            if (document.visibilityState === 'hidden') {
                // Page is hidden, cancel all timers
                this.elementImpressionStates.forEach((state) => {
                    if (state.timer !== null) {
                        window.clearTimeout(state.timer);
                        state.timer = null;
                        state.visibleSince = null;
                    }
                });

                this.logger.warn('Page hidden, all impression timers cancelled');
            }
        });
    }

    /** Resets tracking on SPA navigation (listens to pushState, replaceState, popstate) */
    private initializePageNavigationHandler(): void {
        // Store initial path
        this.currentPagePath = window.location.pathname;

        // Check for path changes periodically (for SPAs that don't trigger events)
        const checkPathChange = () => {
            const newPath = window.location.pathname;

            if (newPath !== this.currentPagePath) {
                this.logger.warn(
                    `Navigation detected (${this.currentPagePath} → ${newPath}), resetting impression tracking`
                );

                // Update current path
                this.currentPagePath = newPath;

                // Reset tracked impressions for the new page
                this.sessionTrackedImpressions.clear();

                // Cancel all active timers
                this.elementImpressionStates.forEach((state) => {
                    if (state.timer !== null) {
                        window.clearTimeout(state.timer);
                        state.timer = null;
                        state.visibleSince = null;
                    }
                });

                // Clear element states
                this.elementImpressionStates.clear();
            }
        };

        // Listen for popstate (back/forward navigation)
        window.addEventListener('popstate', checkPathChange);

        // Listen for pushstate/replacestate (programmatic navigation)
        const originalPushState = history.pushState;
        const originalReplaceState = history.replaceState;

        history.pushState = function (...args) {
            originalPushState.apply(this, args);
            checkPathChange();
        };

        history.replaceState = function (...args) {
            originalReplaceState.apply(this, args);
            checkPathChange();
        };
    }

    /** Handles visibility changes: starts timer on enter, cancels on exit */
    private processIntersectionChanges(entries: IntersectionObserverEntry[]): void {
        // Ignore if page is not visible
        if (document.visibilityState !== 'visible') {
            return;
        }

        entries.forEach((entry) => {
            const element = entry.target as HTMLElement;
            const identifier = extractContentletIdentifier(element);

            if (!identifier) return;

            if (entry.isIntersecting) {
                // Element is visible, start tracking
                this.startImpressionDwellTimer(identifier, element);
            } else {
                // Element is not visible, cancel tracking
                this.cancelImpressionDwellTimer(identifier);
            }
        });
    }

    /** Starts dwell timer; fires impression if element still visible when timer expires */
    private startImpressionDwellTimer(identifier: string, element: HTMLElement): void {
        const state = this.elementImpressionStates.get(identifier);

        if (!state) return;

        // Already tracked, skip
        if (state.tracked || this.hasBeenTrackedInSession(identifier)) {
            return;
        }

        // Timer already running, skip
        if (state.timer !== null) {
            return;
        }

        // Page not visible, skip
        if (document.visibilityState !== 'visible') {
            return;
        }

        // Start timer
        state.visibleSince = Date.now();
        state.element = element;
        state.timer = window.setTimeout(() => {
            // Verify element is still visible before firing (post-dwell check)
            if (this.isElementStillVisible(element)) {
                this.trackAndSendImpression(identifier, element);
            } else {
                this.logger.warn(
                    `Dwell timer expired for ${identifier} but element no longer visible, skipping impression`
                );
                // Clear state since we're not tracking
                state.timer = null;
                state.visibleSince = null;
            }
        }, this.impressionConfig.dwellMs);

        this.logger.debug(
            `Started dwell timer for ${identifier} (${this.impressionConfig.dwellMs}ms)`
        );
    }

    /** Cancels active dwell timer (element left viewport before dwell time) */
    private cancelImpressionDwellTimer(identifier: string): void {
        const state = this.elementImpressionStates.get(identifier);

        if (!state || state.timer === null) return;

        window.clearTimeout(state.timer);
        state.timer = null;
        state.visibleSince = null;

        this.logger.debug(`Cancelled dwell timer for ${identifier}`);
    }

    /** Fires impression event with content & position data (page data added by enricher plugin) */
    private trackAndSendImpression(identifier: string, element: HTMLElement): void {
        const state = this.elementImpressionStates.get(identifier);

        if (!state) return;

        // Calculate actual dwell time
        const dwellTime = state.visibleSince ? Date.now() - state.visibleSince : 0;

        // Extract contentlet data using utility
        const contentletData = extractContentletData(element);

        // Calculate viewport metrics using utility
        const viewportMetrics = getViewportMetrics(element);

        // Read cached DOM index instead of expensive query
        // Falls back to -1 if not cached (should never happen in normal flow)
        const domIndex = parseInt(element.dataset.dotAnalyticsDomIndex || '-1', 10);

        // Build impression payload (enricher plugin adds page data automatically)
        const impressionPayload: DotCMSContentImpressionPayload = {
            content: {
                identifier: contentletData.identifier,
                inode: contentletData.inode,
                title: contentletData.title,
                content_type: contentletData.contentType
            },
            position: {
                viewport_offset_pct: viewportMetrics.offsetPercentage,
                dom_index: domIndex
            }
        };

        // Notify subscribers (plugin will call analytics.track)
        this.notifySubscribers(IMPRESSION_EVENT_TYPE, impressionPayload);

        // Mark as tracked in session
        this.markImpressionAsTracked(identifier);

        // Clear state
        state.timer = null;
        state.visibleSince = null;
        state.tracked = true;

        // Stop observing this element (no longer needed)
        if (this.observer) {
            this.observer.unobserve(element);
        }

        this.logger.info(
            `Fired impression for ${identifier} (dwell: ${dwellTime}ms) - element unobserved`,
            contentletData
        );
    }

    /** Returns skip reason if element is hidden/too small, null if trackable */
    private shouldSkipElement(element: HTMLElement): string | null {
        const rect = element.getBoundingClientRect();
        const styles = window.getComputedStyle(element);

        // Check for zero dimensions (empty/collapsed elements)
        if (rect.height === 0 || rect.width === 0) {
            return `zero dimensions: ${rect.width}x${rect.height}`;
        }

        // Check for very small elements (likely tracking pixels or artifacts)
        const MIN_SIZE = 10; // pixels
        if (rect.height < MIN_SIZE || rect.width < MIN_SIZE) {
            return `too small: ${rect.width}x${rect.height} (minimum: ${MIN_SIZE}px)`;
        }

        // Check for hidden elements (visibility: hidden)
        if (styles.visibility === 'hidden') {
            return 'visibility: hidden';
        }

        // Check for transparent elements (opacity: 0)
        if (parseFloat(styles.opacity) === 0) {
            return 'opacity: 0';
        }

        // Check for elements with display: none
        if (styles.display === 'none') {
            return 'display: none';
        }

        // Element passes all checks
        return null;
    }

    /** Post-dwell check: verifies element still meets visibility threshold */
    private isElementStillVisible(element: HTMLElement): boolean {
        // Check page visibility first
        if (document.visibilityState !== 'visible') {
            return false;
        }

        // Check if element meets visibility threshold using utility
        return isElementMeetingVisibilityThreshold(
            element,
            this.impressionConfig.visibilityThreshold
        );
    }

    /** Checks if impression already fired in current page session */
    private hasBeenTrackedInSession(identifier: string): boolean {
        return this.sessionTrackedImpressions.has(identifier);
    }

    /** Marks impression as tracked (prevents duplicates in same page session) */
    private markImpressionAsTracked(identifier: string): void {
        this.sessionTrackedImpressions.add(identifier);
    }

    /** Cleanup: disconnects observers, clears timers and state */
    public cleanup(): void {
        // Disconnect intersection observer
        if (this.observer) {
            this.observer.disconnect();
            this.observer = null;
        }

        // Disconnect mutation observer
        if (this.mutationObserver) {
            this.mutationObserver.disconnect();
            this.mutationObserver = null;
        }

        // Clear all active dwell timers
        this.elementImpressionStates.forEach((state) => {
            if (state.timer !== null) {
                window.clearTimeout(state.timer);
            }
        });

        // Clear all state
        this.elementImpressionStates.clear();

        // Clear all subscribers
        this.subscribers.clear();

        this.logger.info('Impression tracking cleaned up');
    }
}
