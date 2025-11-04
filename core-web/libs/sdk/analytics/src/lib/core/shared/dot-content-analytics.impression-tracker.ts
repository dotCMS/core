import { getUVEState } from '@dotcms/uve';

import {
    ANALYTICS_CONTENTLET_CLASS,
    DEFAULT_IMPRESSION_DWELL_MS,
    DEFAULT_IMPRESSION_MAX_NODES,
    DEFAULT_IMPRESSION_MUTATION_OBSERVER_DEBOUNCE_MS,
    DEFAULT_IMPRESSION_THROTTLE_MS,
    DEFAULT_IMPRESSION_VISIBILITY_THRESHOLD,
    IMPRESSION_EVENT_TYPE
} from './constants';
import { DotCMSAnalyticsConfig, DotCMSContentImpressionPayload, ImpressionConfig } from './models';

import {
    AnalyticsTrackFn,
    createDebounce,
    extractContentletData,
    extractContentletIdentifier,
    getViewportMetrics,
    isElementMeetingVisibilityThreshold
} from '../plugin/impression/dot-analytics.impression.utils';

/** Tracks the state of an element's impression (timer, visibility, tracked status) */
interface ImpressionState {
    timer: number | null;
    visibleSince: number | null;
    tracked: boolean;
    element: HTMLElement | null;
}

/**
 * Tracks content impressions using IntersectionObserver and dwell time.
 * Fires events when elements are visible for the configured duration.
 */
export class DotCMSImpressionTracker {
    private observer: IntersectionObserver | null = null;
    private mutationObserver: MutationObserver | null = null;
    private elementImpressionStates = new Map<string, ImpressionState>();
    private sessionTrackedImpressions = new Set<string>();
    private config: DotCMSAnalyticsConfig;
    private track: AnalyticsTrackFn;
    private impressionConfig: Required<ImpressionConfig>;
    private currentPagePath = '';

    constructor(config: DotCMSAnalyticsConfig, track: AnalyticsTrackFn) {
        this.config = config;
        this.track = track;

        // Merge user config with defaults
        this.impressionConfig = this.resolveImpressionConfig(config.impressions);
    }

    /** Merges user config with defaults */
    private resolveImpressionConfig(
        userConfig: ImpressionConfig | boolean | undefined
    ): Required<ImpressionConfig> {
        // Normalize to object: if boolean/undefined, treat as empty config
        const config = typeof userConfig === 'object' && userConfig !== null ? userConfig : {};

        // Single return with defaults merged
        return {
            visibilityThreshold:
                config.visibilityThreshold ?? DEFAULT_IMPRESSION_VISIBILITY_THRESHOLD,
            dwellMs: config.dwellMs ?? DEFAULT_IMPRESSION_DWELL_MS,
            maxNodes: config.maxNodes ?? DEFAULT_IMPRESSION_MAX_NODES,
            throttleMs: config.throttleMs ?? DEFAULT_IMPRESSION_THROTTLE_MS,
            useIdleCallback: config.useIdleCallback ?? false
        };
    }

    /** Initializes tracking: sets up observers, finds contentlets, handles visibility/navigation */
    public initialize(): void {
        // Early return if SSR
        if (typeof window === 'undefined' || typeof document === 'undefined') {
            return;
        }

        // Early return if in editor mode (check for UVE markers)
        if (getUVEState()) {
            if (this.config.debug) {
                console.warn('DotCMS Analytics: Impression tracking disabled in editor mode');
            }

            return;
        }

        // Setup IntersectionObserver
        this.initializeIntersectionObserver();

        // Find and observe contentlet elements
        this.findAndObserveContentletElements();

        // Setup mutation observer for dynamic content
        this.initializeDynamicContentDetector();

        // Listen for visibility changes to pause/resume tracking
        this.initializePageVisibilityHandler();

        // Listen for page navigation to reset tracking
        this.initializePageNavigationHandler();

        if (this.config.debug) {
            console.warn(
                `DotCMS Analytics: Impression tracking initialized with config:`,
                this.impressionConfig
            );
        }
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

        const contentlets = document.querySelectorAll<HTMLElement>(
            `.${ANALYTICS_CONTENTLET_CLASS}`
        );

        if (contentlets.length === 0) {
            if (this.config.debug) {
                console.warn('DotCMS Analytics: No contentlets found to track');
            }

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
                    if (this.config.debug) {
                        console.warn(
                            `DotCMS Analytics: Skipping element ${identifier} (${skipReason})`
                        );
                    }
                    continue;
                }

                // Only observe and initialize if this is a new element
                if (!this.elementImpressionStates.has(identifier)) {
                    this.observer.observe(element);
                    this.elementImpressionStates.set(identifier, {
                        timer: null,
                        visibleSince: null,
                        tracked: this.hasBeenTrackedInSession(identifier),
                        element
                    });

                    observedCount++;

                    // Add visual debugging indicators in development
                    if (this.config.debug) {
                        element.dataset.dotAnalyticsObserved = 'true';
                        element.style.outline = '2px solid red';
                        element.style.outlineOffset = '-2px';
                        element.style.transition = 'outline-color 0.5s ease-in-out';
                    }
                }
            }
        }

        if (this.config.debug) {
            console.warn(`DotCMS Analytics: Observing ${observedCount} contentlets`);
            if (contentlets.length > maxNodesToTrack) {
                console.warn(
                    `DotCMS Analytics: ${contentlets.length - maxNodesToTrack} contentlets not tracked (maxNodes limit: ${this.impressionConfig.maxNodes})`
                );
            }
        }
    }

    /** Watches for new contentlets added to DOM (debounced for performance) */
    private initializeDynamicContentDetector(): void {
        if (typeof window === 'undefined' || typeof document === 'undefined') {
            return;
        }

        const debouncedScan = createDebounce(() => {
            this.findAndObserveContentletElements();
        }, DEFAULT_IMPRESSION_MUTATION_OBSERVER_DEBOUNCE_MS);

        this.mutationObserver = new MutationObserver(() => {
            debouncedScan();
        });

        this.mutationObserver.observe(document.body, {
            childList: true,
            subtree: true
        });

        if (this.config.debug) {
            console.warn(
                'DotCMS Analytics: MutationObserver enabled for dynamic content detection'
            );
        }
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

                if (this.config.debug) {
                    console.warn('DotCMS Analytics: Page hidden, all impression timers cancelled');
                }
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
                if (this.config.debug) {
                    console.warn(
                        `DotCMS Analytics: Navigation detected (${this.currentPagePath} → ${newPath}), resetting impression tracking`
                    );
                }

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

        // Fallback: check periodically for path changes (catches edge cases)
        setInterval(checkPathChange, 1000);
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
                if (this.config.debug) {
                    console.warn(
                        `DotCMS Analytics: Dwell timer expired for ${identifier} but element no longer visible, skipping impression`
                    );
                }
                // Clear state since we're not tracking
                state.timer = null;
                state.visibleSince = null;
            }
        }, this.impressionConfig.dwellMs);

        if (this.config.debug) {
            console.warn(
                `DotCMS Analytics: Started dwell timer for ${identifier} (${this.impressionConfig.dwellMs}ms)`
            );
        }
    }

    /** Cancels active dwell timer (element left viewport before dwell time) */
    private cancelImpressionDwellTimer(identifier: string): void {
        const state = this.elementImpressionStates.get(identifier);

        if (!state || state.timer === null) return;

        window.clearTimeout(state.timer);
        state.timer = null;
        state.visibleSince = null;

        if (this.config.debug) {
            console.warn(`DotCMS Analytics: Cancelled dwell timer for ${identifier}`);
        }
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
                dom_index: Array.from(
                    document.querySelectorAll(`.${ANALYTICS_CONTENTLET_CLASS}`)
                ).indexOf(element)
            }
        };

        // Send through Analytics.js pipeline (Identity → Enricher → Main plugin)
        this.track(IMPRESSION_EVENT_TYPE, impressionPayload);

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

        // Update visual indicator in debug mode
        if (this.config.debug) {
            element.style.outline = '2px solid green';
            element.style.outlineOffset = '-2px';

            console.warn(
                `DotCMS Analytics: Fired impression for ${identifier} (dwell: ${dwellTime}ms) - element unobserved`,
                contentletData
            );
        }
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

        if (this.config.debug) {
            console.warn('DotCMS Analytics: Impression tracking cleaned up');
        }
    }
}
