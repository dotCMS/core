import Analytics from 'analytics';

import { EXPECTED_UTM_KEYS } from './dot-content-analytics.constants';
import { BrowserEventData, DotContentAnalyticsConfig } from './dot-content-analytics.model';

import { dotAnalyticsEnricherPlugin } from '../plugin/dot-analytics.enricher.plugin';
import { dotAnalytics } from '../plugin/dot-analytics.plugin';

/**
 * Retrieves analytics attributes from a given script element.
 *
 * @return {DotAnalyticsConfig | null} - The analytics attributes or null if there are no valid attributes present.
 */
export const getDataAnalyticsAttributes = (location: Location): DotContentAnalyticsConfig => {
    const script = getAnalyticsScriptTag();

    const attributes = {
        server: script.getAttribute('data-analytics-server') || location.origin,
        debug: script.hasAttribute('data-analytics-debug'),
        autoPageView: script.hasAttribute('data-analytics-auto-page-view'),
        apiKey: script.getAttribute('data-analytics-key') || ''
    };

    return attributes;
};

/**
 * Retrieves the analytics script tag from the document.
 *
 * @returns {HTMLScriptElement} - The analytics script tag.
 */
export const getAnalyticsScriptTag = (): HTMLScriptElement => {
    const scripts = document.querySelector('script[data-analytics-key]');

    if (!scripts) {
        throw new Error('Dot Analytics: Script not found');
    }

    return scripts as HTMLScriptElement;
};

/**
 * Retrieves the browser event data.
 *
 * @param {Location} location - The location object.
 * @returns {BrowserEventData} - The browser event data.
 */
export const getBrowserEventData = (location: Location): BrowserEventData => ({
    utc_time: new Date().toISOString(),
    local_tz_offset: new Date().getTimezoneOffset(),
    screen_resolution: `${window.screen.width}x${window.screen.height}`,
    vp_size: `${window.innerWidth}x${window.innerHeight}`,
    userAgent: navigator.userAgent,
    user_language: navigator.language,
    doc_encoding: document.characterSet,
    doc_path: location.pathname,
    doc_host: location.hostname,
    doc_protocol: location.protocol,
    doc_hash: location.hash,
    doc_search: location.search,
    referrer: document.referrer,
    page_title: document.title,
    utm: extractUTMParameters(window.location)
});

/**
 * Extracts UTM parameters from a given URL location.
 *
 * @param {Location} location - The location object containing the URL.
 * @returns {Record<string, string>} - An object containing the extracted UTM parameters.
 */
export const extractUTMParameters = (location: Location): Record<string, string> => {
    const urlParams = new URLSearchParams(location.search);

    return EXPECTED_UTM_KEYS.reduce(
        (acc, key) => {
            const value = urlParams.get(key);
            if (value !== null) {
                acc[key.replace('utm_', '')] = value;
            }

            return acc;
        },
        {} as Record<string, string>
    );
};

/**
 * A function to redirect the user to a new URL.
 *
 * @param {string} href - The URL to redirect to.
 * @returns {void}
 */
export const defaultRedirectFn = (href: string) => (window.location.href = href);

/**
 * Checks if the current environment is inside the dotCMS editor.
 *
 * @returns {boolean} - True if inside the editor, false otherwise.
 */
export const isInsideEditor = (): boolean => {
    try {
        if (typeof window === 'undefined') return false;

        if (!window.parent) return false;

        return window.parent !== window;
    } catch (e) {
        return false;
    }
};

/**
 * Creates an analytics instance.
 *
 * @param {DotContentAnalyticsConfig} config - The configuration object for the analytics instance.
 * @returns {Analytics | null} - The analytics instance or null if there is an error.
 */
export const createAnalyticsInstance = (config: DotContentAnalyticsConfig) => {
    if (!config.apiKey) {
        console.error('DotContentAnalytics: Missing "apiKey" in configuration');

        return null;
    }

    if (!config.server) {
        console.error('DotContentAnalytics: Missing "server" in configuration');

        return null;
    }

    return Analytics({
        app: 'dotAnalytics',
        debug: config.debug,
        plugins: [dotAnalyticsEnricherPlugin, dotAnalytics(config)]
    });
};
