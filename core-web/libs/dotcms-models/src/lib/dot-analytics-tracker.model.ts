export interface BaseAnalyticsEvent {
    anonymousId?: string;
    src: string;
    utc_time: string;
    local_tz_offset: number;
    doc_path: string;
    doc_host: string;
    event_type: string;
    key?: string;
}

export enum EVENT_TYPES {
    TRACK = 'track',
    PAGEV_VIEW = 'pageview'
}
