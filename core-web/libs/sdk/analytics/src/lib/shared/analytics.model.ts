import { EXPECTED_UTM_KEYS } from './analytics.constants';

export interface DotAnalyticsConfig {
    // Analytics server
    server: string;
    // Analytics debug
    debug: boolean;
    // Auto track page view
    autoPageView: boolean;
    // Analytics key
    key: string;
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
    url: string;
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
