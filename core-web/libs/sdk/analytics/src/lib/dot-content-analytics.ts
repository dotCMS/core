import Analytics, { AnalyticsInstance } from 'analytics';

import { dotAnalyticsPlugin } from './plugin/dot-analytics.plugin';
import { DotContentAnalyticsConfig } from './shared/dot-content-analytics.model';
import { defaultRedirectFn } from './shared/dot-content-analytics.utils';

/**
 * DotAnalytics class for sending events to Content Analytics.
 * This class handles tracking events and automatically collects browser information
 * like user agent, viewport size, and other relevant browser metadata to provide
 * better analytics insights.
 *
 * The class follows a singleton pattern to ensure only one analytics instance
 * is running at a time.
 */
export class DotContentAnalytics {
    private static instance: DotContentAnalytics | null = null;
    #initialized = false;
    #analytics: AnalyticsInstance | null = null;
    #config: DotContentAnalyticsConfig;
    #currentLocation: Location = location;
    /**
     * Returns a custom redirect function. If a custom redirect function is not configured,
     * the default redirect function will be used.
     *
     * @return {function} A function that accepts a URL string parameter and performs a redirect.
     *                    If no parameter is provided, the function will not perform any action.
     */
    get customRedirectFn(): (url: string) => void {
        return this.#config.redirectFn ?? defaultRedirectFn;
    }

    /**
     * Retrieves the current location.
     *
     * @returns {Location} The current location.
     */
    public get location(): Location {
        return this.#currentLocation;
    }

    private constructor(config: DotContentAnalyticsConfig) {
        this.#config = config;
    }

    /**
     * Returns the singleton instance of DotContentAnalytics
     */
    static getInstance(config: DotContentAnalyticsConfig): DotContentAnalytics {
        if (!DotContentAnalytics.instance) {
            DotContentAnalytics.instance = new DotContentAnalytics(config);
        }

        return DotContentAnalytics.instance;
    }

    /**
     * Initializes the analytics instance
     */
    async ready(): Promise<void> {
        if (this.#initialized) {
            return Promise.resolve();
        }

        try {
            this.#analytics = Analytics({
                app: 'dotAnalytics',
                debug: this.#config.debug,
                plugins: [dotAnalyticsPlugin(this.#config)]
            });

            this.#initialized = true;
        } catch (error) {
            console.error('Failed to initialize DotAnalytics:', error);
            throw error;
        }
    }

    /**
     * Track a custom event with optional payload
     */
    track(eventName: string, payload?: Record<string, unknown>): void {
        if (!this.#analytics || !this.#initialized) {
            console.warn('DotContentAnalytics not initialized');
            return;
        }

        this.#analytics.track(eventName, payload);
    }

    /**
     * Track page view with current location
     */
    trackPageView(): void {
        if (!this.#analytics || !this.#initialized) {
            console.warn('DotContentAnalytics not initialized');
            return;
        }

        this.track('pageview');
    }
}
