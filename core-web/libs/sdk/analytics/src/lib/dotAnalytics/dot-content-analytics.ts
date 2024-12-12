import Analytics, { AnalyticsInstance } from 'analytics';

import { dotAnalyticsEnricherPlugin } from './plugin/dot-analytics.enricher.plugin';
import { dotAnalytics } from './plugin/dot-analytics.plugin';
import { DotContentAnalyticsConfig } from './shared/dot-content-analytics.model';
import { DotLogger } from './utils/DotLogger';

/**
 * DotContentAnalytics class for sending events to Content Analytics.
 * This class handles tracking events and automatically collects browser information
 * like user agent, viewport size, and other relevant browser metadata to provide
 * better analytics insights.
 *
 * The class follows a singleton pattern to ensure only one analytics instance
 * is running at a time. It can be initialized with configuration options including
 * server URL, debug mode, auto page view tracking, and API key.
 */
export class DotContentAnalytics {
    private static instance: DotContentAnalytics | null = null;
    private readonly logger: DotLogger;
    #initialized = false;
    #analytics: AnalyticsInstance | null = null;
    #config: DotContentAnalyticsConfig;

    private constructor(config: DotContentAnalyticsConfig) {
        this.#config = config;
        this.logger = new DotLogger(config.debug, 'DotContentAnalytics');

        if (!config.apiKey) {
            this.#initialized = false;
        }
    }

    /**
     * Returns the singleton instance of DotContentAnalytics
     */
    static getInstance(config: DotContentAnalyticsConfig): DotContentAnalytics {
        if (!config.apiKey) {
            console.error(
                `DotContentAnalytics: Missing "apiKey" in configuration - Events will not be sent to Content Analytics`
            );
        }

        if (!config.server) {
            console.error(
                `DotContentAnalytics: Missing "server" in configuration - Events will not be sent to Content Analytics`
            );
        }

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
            this.logger.log('Already initialized');

            return Promise.resolve();
        }

        try {
            this.logger.group('Initialization');
            this.logger.time('Init');

            const plugins = this.#getPlugins();

            this.#analytics = Analytics({
                app: 'dotAnalytics',
                debug: this.#config.debug,
                plugins
            });

            this.#initialized = true;
            this.logger.log('dotAnalytics initialized');
            this.logger.timeEnd('Init');
            this.logger.groupEnd();

            return Promise.resolve();
        } catch (error) {
            this.logger.error(`Failed to initialize: ${error}`);
            throw error;
        }
    }

    /**
     * Returns the plugins to be used in the analytics instance
     */
    #getPlugins() {
        const hasRequiredConfig = this.#config.apiKey && this.#config.server;

        if (!hasRequiredConfig) {
            return [];
        }

        return [dotAnalyticsEnricherPlugin, dotAnalytics(this.#config)];
    }

    /**
     * Sends a page view event to the analytics instance.
     *
     * @param {Record<string, unknown>} payload - The payload to send to the analytics instance.
     * @returns {void}
     */
    pageView(payload: Record<string, unknown> = {}): void {
        if (!this.#analytics || !this.#initialized) {
            this.logger.warn('Not initialized');

            return;
        }

        this.#analytics.page(payload);
    }

    /**
     * Sends a track event to the analytics instance.
     *
     * @param {string} eventName - The name of the event to send.
     * @param {Record<string, unknown>} payload - The payload to send to the analytics instance.
     * @returns {void}
     */
    track(eventName: string, payload: Record<string, unknown> = {}): void {
        if (!this.#analytics || !this.#initialized) {
            this.logger.warn('Not initialized');

            return;
        }

        this.#analytics.track(eventName, payload);
    }
}
