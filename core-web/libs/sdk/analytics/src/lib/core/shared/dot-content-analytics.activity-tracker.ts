import { ANALYTICS_WINDOWS_ACTIVE_KEY, ANALYTICS_WINDOWS_CLEANUP_KEY } from '@dotcms/uve/internal';

import { ACTIVITY_EVENTS, DEFAULT_SESSION_TIMEOUT_MINUTES } from './constants';
import { DotCMSAnalyticsConfig } from './models';

// Extend window interface for cleanup function
declare global {
    interface Window {
        [ANALYTICS_WINDOWS_CLEANUP_KEY]?: () => void;
        [ANALYTICS_WINDOWS_ACTIVE_KEY]?: boolean;
    }
}

/**
 * Activity tracking manager for DotCMS Analytics.
 * Handles user activity monitoring, session management, and inactivity detection.
 * Singleton pattern since we only handle one site at a time.
 */
class DotCMSActivityTracker {
    private activityListeners: (() => void)[] = [];
    private lastActivityTime = Date.now();
    private inactivityTimer: ReturnType<typeof setTimeout> | null = null;
    private isThrottled = false;
    private config: DotCMSAnalyticsConfig | null = null;
    private readonly ACTIVITY_THROTTLE_MS = 1000; // Throttle activity events to max 1 per second

    /**
     * Updates activity timestamp (throttled for performance)
     */
    private updateActivityTime(): void {
        this.lastActivityTime = Date.now();

        // Reset inactivity timer
        if (this.inactivityTimer) {
            clearTimeout(this.inactivityTimer);
        }

        this.inactivityTimer = setTimeout(
            () => {
                // User became inactive - handle session timeout
                if (this.config?.debug) {
                    console.warn('DotCMS Analytics: User became inactive after timeout');
                }

                // Timer has fired, set to null to indicate no active timer
                this.inactivityTimer = null;
            },
            DEFAULT_SESSION_TIMEOUT_MINUTES * 60 * 1000
        );
    }

    /**
     * Checks if user has been inactive
     */
    public isUserInactive(): boolean {
        const timeoutMs = DEFAULT_SESSION_TIMEOUT_MINUTES * 60 * 1000;

        return Date.now() - this.lastActivityTime > timeoutMs;
    }

    /**
     * Gets last activity time
     */
    public getLastActivity(): number {
        return this.lastActivityTime;
    }

    /**
     * Updates session activity with throttling
     */
    public updateSessionActivity(): void {
        if (this.isThrottled) return;

        this.isThrottled = true;
        this.updateActivityTime();

        setTimeout(() => {
            this.isThrottled = false;
        }, this.ACTIVITY_THROTTLE_MS);
    }

    /**
     * Initializes activity tracking with event listeners
     */
    public initialize(config: DotCMSAnalyticsConfig): void {
        this.cleanup();
        this.config = config;

        // Early return if window is not available (SSR/build time)
        if (typeof window === 'undefined') {
            return;
        }

        const throttledHandler = () => this.updateSessionActivity();

        // Add activity event listeners
        ACTIVITY_EVENTS.forEach((eventType) => {
            window.addEventListener(eventType, throttledHandler, { passive: true });
            this.activityListeners.push(() =>
                window.removeEventListener(eventType, throttledHandler)
            );
        });

        // Handle visibility changes for mobile/background scenarios
        const handleVisibilityChange = () => {
            if (document.visibilityState === 'visible') {
                this.updateSessionActivity();
                if (config.debug) {
                    console.warn('DotCMS Analytics: User returned to tab, session reactivated');
                }
            }
        };

        document.addEventListener('visibilitychange', handleVisibilityChange);
        this.activityListeners.push(() =>
            document.removeEventListener('visibilitychange', handleVisibilityChange)
        );

        // Initialize activity time
        this.updateActivityTime();

        if (config.debug) {
            console.warn('DotCMS Analytics: Activity tracking initialized');
        }
    }

    /**
     * Cleans up all activity tracking listeners
     */
    public cleanup(): void {
        this.activityListeners.forEach((cleanup) => cleanup());
        this.activityListeners = [];

        if (this.inactivityTimer) {
            clearTimeout(this.inactivityTimer);
            this.inactivityTimer = null;
        }

        this.config = null;
    }

    /**
     * Gets session information for debugging
     */
    public getSessionInfo() {
        return {
            lastActivity: this.getLastActivity(),
            isActive: !this.isUserInactive()
        };
    }
}

// Create singleton instance
const activityTracker = new DotCMSActivityTracker();

/**
 * Updates session activity with throttling
 */
export const updateSessionActivity = (): void => {
    activityTracker.updateSessionActivity();
};

/**
 * Gets session information for debugging
 */
export const getSessionInfo = () => {
    return activityTracker.getSessionInfo();
};

/**
 * Initializes activity tracking
 */
export const initializeActivityTracking = (config: DotCMSAnalyticsConfig): void => {
    activityTracker.initialize(config);
};

/**
 * Cleans up activity tracking listeners and resets analytics state
 */
export const cleanupActivityTracking = (): void => {
    activityTracker.cleanup();

    // Reset analytics state and cleanup window properties
    if (typeof window !== 'undefined') {
        window[ANALYTICS_WINDOWS_ACTIVE_KEY] = false;
        window[ANALYTICS_WINDOWS_CLEANUP_KEY] = undefined;

        // Dispatch cleanup event to notify any subscribers
        window.dispatchEvent(new CustomEvent('dotcms:analytics:cleanup'));
    }
};

/**
 * Checks if user has been inactive
 */
export const isUserInactive = (): boolean => {
    return activityTracker.isUserInactive();
};

/**
 * Gets last activity time
 */
export const getLastActivity = (): number => {
    return activityTracker.getLastActivity();
};
