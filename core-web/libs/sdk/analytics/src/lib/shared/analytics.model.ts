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
 * The data for a page view event.
 */
export interface PageViewEvent {
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
    utm: {
        source?: string;
        medium?: string;
        campaign?: string;
        id?: string;
    };
    src: string;
    event_type: string;
}
