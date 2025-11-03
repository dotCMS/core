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

/**
 * State for tracking an individual element's impression
 */
interface ImpressionState {
    /** Timer ID for dwell time tracking */
    timer: number | null;
    /** Timestamp when element became visible */
    visibleSince: number | null;
    /** Whether impression has been fired for this element */
    tracked: boolean;
    /** Reference to the element for post-dwell visibility check */
    element: HTMLElement | null;
}

/**
 * Impression tracking manager for DotCMS Analytics.
 * Tracks when contentlets become visible in the viewport using IntersectionObserver.
 * Fires impression events when elements meet visibility and dwell time thresholds.
 *
 * Singleton pattern - one tracker per analytics instance.
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

    /**
     * Resolves impression config by merging user config with defaults
     */
    private resolveImpressionConfig(
        userConfig: ImpressionConfig | boolean | undefined
    ): Required<ImpressionConfig> {
        // TODO: Mejorar esto, se esta duplicando esa asignación en el return.
        // If boolean true or undefined, use all defaults
        if (typeof userConfig !== 'object' || userConfig === null) {
            return {
                visibilityThreshold: DEFAULT_IMPRESSION_VISIBILITY_THRESHOLD,
                dwellMs: DEFAULT_IMPRESSION_DWELL_MS,
                maxNodes: DEFAULT_IMPRESSION_MAX_NODES,
                throttleMs: DEFAULT_IMPRESSION_THROTTLE_MS,
                useIdleCallback: false
            };
        }

        // If object, merge with defaults
        return {
            visibilityThreshold:
                userConfig.visibilityThreshold ?? DEFAULT_IMPRESSION_VISIBILITY_THRESHOLD,
            dwellMs: userConfig.dwellMs ?? DEFAULT_IMPRESSION_DWELL_MS,
            maxNodes: userConfig.maxNodes ?? DEFAULT_IMPRESSION_MAX_NODES,
            throttleMs: userConfig.throttleMs ?? DEFAULT_IMPRESSION_THROTTLE_MS,
            useIdleCallback: userConfig.useIdleCallback ?? false
        };
    }

    /**
     * Initializes the impression tracking system.
     *
     * Flow:
     * 1. Check for SSR and editor mode (early returns)
     * 2. Load previously tracked impressions from session storage
     * 3. Setup IntersectionObserver with configured visibility threshold
     * 4. Find and observe all contentlet elements in the DOM
     * 5. Setup MutationObserver for dynamic content detection (if enabled)
     * 6. Setup page visibility handler to pause/resume tracking
     *
     * @example
     * const tracker = new DotCMSImpressionTracker(config, analytics);
     * tracker.initialize(); // Starts tracking impressions
     */
    public initialize(): void {
        // Early return if SSR
        if (typeof window === 'undefined' || typeof document === 'undefined') {
            return;
        }

        // Early return if in editor mode (check for UVE markers)
        if (this.isEditorMode()) {
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

    /**
     * Checks if we're in editor mode (UVE)
     */
    private isEditorMode(): boolean {
        // Check for UVE markers in window
        return (
            !!(window as Window & { dotcmsUVE?: unknown }).dotcmsUVE ||
            document.body.classList.contains('dotcms-edit-mode')
        );
    }

    /**
     * Initializes the IntersectionObserver with configured visibility threshold.
     *
     * @remarks
     * The observer uses the browser viewport as the root and triggers callbacks
     * when elements cross the configured visibility threshold.
     */
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

    /**
     * Finds and observes all contentlet elements in the DOM.
     *
     * @remarks
     * - Queries for elements with the analytics contentlet class
     * - Limits tracking to maxNodes for performance
     * - Initializes or updates state for each element
     * - Skips elements without identifiers
     */
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

    /**
     * Initializes MutationObserver to detect dynamically added contentlet elements.
     *
     * @remarks
     * - Observes the document body for DOM changes
     * - Debounces scans to avoid excessive processing
     * - Automatically finds and observes new contentlets
     */
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

    /**
     * Initializes page visibility change handler.
     *
     * @remarks
     * Cancels all active dwell timers when the page becomes hidden
     * to prevent incorrect impressions from being fired.
     */
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

    /**
     * Initializes page navigation handler to reset tracking on route changes.
     *
     * @remarks
     * - Detects when the user navigates to a different page (SPA navigation)
     * - Resets tracked impressions to allow re-tracking on the new page
     * - Maintains deduplication within the same page
     * - Works with client-side routing (NextJS, React Router, etc.)
     */
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

    /**
     * Processes IntersectionObserver callbacks.
     *
     * @param entries - Array of intersection entries from the observer
     *
     * @remarks
     * - Starts dwell timer when element enters viewport
     * - Cancels dwell timer when element exits viewport
     * - Ignores callbacks when page is not visible
     */
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

    /**
     * Starts the dwell timer for an element impression.
     *
     * @param identifier - The contentlet identifier
     * @param element - The HTML element to track
     *
     * @remarks
     * - Skips if already tracked in session
     * - Skips if timer is already running
     * - Skips if page is not visible
     * - Verifies element is still visible when timer expires (post-dwell check)
     * - Only fires impression if all conditions are met
     */
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

    /**
     * Cancels the dwell timer for an element impression.
     *
     * @param identifier - The contentlet identifier
     *
     * @remarks
     * Called when an element exits the viewport before the dwell time expires.
     */
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

    /**
     * Tracks and sends an impression event to analytics.
     *
     * @param identifier - The contentlet identifier
     * @param element - The HTML element that was impressed
     *
     * @remarks
     * - Calculates actual dwell time
     * - Extracts contentlet data using utility
     * - Calculates viewport metrics using utility
     * - Sends ONLY impression-specific data (content, position)
     * - Page data will be added automatically by the enricher plugin
     * - Marks impression as tracked in session
     * - Updates element state
     */
    private trackAndSendImpression(identifier: string, element: HTMLElement): void {
        const state = this.elementImpressionStates.get(identifier);

        if (!state) return;

        // Calculate actual dwell time
        const dwellTime = state.visibleSince ? Date.now() - state.visibleSince : 0;

        // Extract contentlet data using utility
        const contentletData = extractContentletData(element);

        // Calculate viewport metrics using utility
        const viewportMetrics = getViewportMetrics(element);

        // Build impression payload with ONLY impression-specific data
        // The enricher plugin will add page data automatically
        // TODO: Mejorar este type
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

        // Fire the impression event directly using the track function
        // This goes through the full Analytics.js pipeline:
        // 1. Identity Plugin → Adds context (user_id, session_id, device)
        // 2. Enricher Plugin → Adds page data automatically
        // 3. Main Plugin → Sends to server/queue
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

    /**
     * Determines if an element should be skipped from tracking.
     *
     * @param element - The HTML element to check
     * @returns A string describing why the element should be skipped, or null if it should be tracked
     *
     * @remarks
     * Filters out elements that are:
     * - Zero dimensions (empty/collapsed)
     * - Too small (< 10x10px, likely tracking pixels)
     * - Hidden with CSS (visibility: hidden, opacity: 0)
     */
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

    /**
     * Checks if an element is still visible after dwell time expires.
     *
     * @param element - The HTML element to check
     * @returns True if element is still visible and meets threshold
     *
     * @remarks
     * This is the critical post-dwell check that prevents incorrect impressions.
     */
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

    /**
     * Checks if an impression has already been tracked in this session.
     *
     * @param identifier - The contentlet identifier
     * @returns True if the impression was already tracked
     */
    private hasBeenTrackedInSession(identifier: string): boolean {
        return this.sessionTrackedImpressions.has(identifier);
    }

    /**
     * Marks an impression as tracked in the current page session.
     *
     * @param identifier - The contentlet identifier
     *
     * @remarks
     * Adds to in-memory Set for deduplication within the current page.
     * Tracked impressions are reset on page reload/navigation.
     */
    private markImpressionAsTracked(identifier: string): void {
        this.sessionTrackedImpressions.add(identifier);
    }

    /**
     * Cleans up all tracking resources.
     *
     * @remarks
     * - Disconnects IntersectionObserver
     * - Disconnects MutationObserver
     * - Clears all active dwell timers
     * - Clears all state
     *
     * Should be called when the tracker is no longer needed (e.g., page unload).
     */
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
