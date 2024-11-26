import { ANALYTICS_SOURCE_TYPE, EXPECTED_UTM_KEYS } from './analytics.constants';
import { DotAnalyticsConfig, PageViewEvent } from './analytics.model';

/**
 * Retrieves analytics attributes from a given script element.
 *
 * @return {DotAnalyticsConfig | null} - The analytics attributes or null if there are no valid attributes present.
 */
export const getDataAnalyticsAttributes = (location: Location): DotAnalyticsConfig => {
    const script = getAnalyticsScriptTag();

    const attributes = {
        server: script.getAttribute('data-analytics-server') || location.href,
        debug: script.hasAttribute('data-analytics-debug'),
        autoPageView: script.hasAttribute('data-analytics-auto-page-view'),
        key: script.getAttribute('data-analytics-key') || ''
    };

    return attributes;
};

/**
 * Retrieves the analytics script tag from the document.
 *
 * @returns {HTMLScriptElement} - The analytics script tag.
 */
export const getAnalyticsScriptTag = (): HTMLScriptElement => {
    const scripts = document.querySelector('script[data-analytics-server]');

    if (!scripts) {
        throw new Error('Dot Analytics: Script not found');
    }

    return scripts as HTMLScriptElement;
};

/**
 * Creates the data for a page view event.
 *
 * @param {string} event_type - The type of event.
 * @param {Location} location - The location object.
 * @returns {PageViewEvent} - The data for the page view event.
 */
export const createAnalyticsPageViewData = (
    event_type: string,
    location: Location
): Omit<PageViewEvent, 'type' | 'key'> => {
    const utmParams = extractUTMParameters(location);

    const vpWidth = window.innerWidth;
    const vpHeight = window.innerHeight;

    const userLanguage = navigator.language;
    const docEncoding = document.characterSet;

    return {
        event_type,
        utc_time: new Date().toISOString(),
        local_tz_offset: new Date().getTimezoneOffset(),
        referer: document.referrer,
        url: location.href,
        page_title: document.title,
        doc_path: location.pathname,
        doc_host: location.hostname,
        doc_protocol: location.protocol,
        doc_hash: location.hash,
        doc_search: location.search,
        screen_resolution: `${window.screen.width}x${window.screen.height}`,
        vp_size: `${vpWidth}x${vpHeight}`,
        user_agent: navigator.userAgent,
        user_language: userLanguage,
        doc_encoding: docEncoding,
        utm: utmParams,
        src: ANALYTICS_SOURCE_TYPE
    };
};

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
