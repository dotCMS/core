import Analytics, { AnalyticsInstance } from 'analytics';

import { dotAnalyticsPlugin } from './plugin/dot-analytics.plugin';
import { DotAnalyticsConfig } from './shared/analytics.model';

/**
 * DotAnalytics class for sending events to Content Analytics.
 * This class handles tracking events and automatically collects browser information
 * like user agent, viewport size, and other relevant browser metadata to provide
 * better analytics insights.
 *
 * The class follows a singleton pattern to ensure only one analytics instance
 * is running at a time.
 */
export class DotAnalytics {
    private static instance: DotAnalytics | null = null;
    #initialized = false;
    #analytics: AnalyticsInstance | null = null;
    #config: DotAnalyticsConfig;

    private constructor(config: DotAnalyticsConfig) {
        this.#config = config;
    }

    static getInstance(config: DotAnalyticsConfig): DotAnalytics {
        if (!DotAnalytics.instance) {
            DotAnalytics.instance = new DotAnalytics(config);
        }

        return DotAnalytics.instance;
    }

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
}
