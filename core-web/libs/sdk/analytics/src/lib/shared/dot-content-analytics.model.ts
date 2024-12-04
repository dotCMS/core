import { EXPECTED_UTM_KEYS } from './dot-content-analytics.constants';

/**
 * Configuration interface for DotAnalytics SDK.
 *
 * @interface DotAnalyticsConfig
 */
export interface DotContentAnalyticsConfig {
    /**
     * The URL of the Analytics server endpoint.
     */
    server: string;

    /**
     * Enable debug mode to get additional logging information.
     */
    debug: boolean;

    /**
     * Automatically track page views when set to true.
     */
    autoPageView: boolean;

    /**
     * The API key for authenticating with the Analytics service.
     */
    key: string;

    /**
     * Custom redirect function handler.
     * When provided, this function will be called instead of the default browser redirect
     * for handling URL redirections.
     *
     * @param {string} url - The URL to redirect to
     */
    redirectFn?: (url: string) => void;
}

/**
 * The type of event.
 */
export enum EventType {
    Track = 'track'
}

// UTM parameters generated from the expected UTM keys
type UTMParams = {
    [key in (typeof EXPECTED_UTM_KEYS)[number] as key extends `utm_${infer U}`
        ? U
        : never]?: string;
};

/**
 * The data for a page view event.
 */
export interface PageViewEvent {
    type: EventType;
    key: string;
    utc_time: string;
    local_tz_offset: number;
    referer: string;
    page_title: string;
    doc_path: string;
    doc_host: string;
    doc_search: string;
    screen_resolution: string;
    vp_size: string;
    user_agent: string;
    user_language: string;
    doc_encoding: string;
    doc_protocol: string;
    doc_hash: string;
    utm: UTMParams;
    src: string;
    event_type: string;
    timestamp?: string;
}
