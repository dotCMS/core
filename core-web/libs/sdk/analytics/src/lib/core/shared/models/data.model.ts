/**
 * Data models for DotCMS Analytics
 * Contains interfaces for data structures used in analytics events
 */

/**
 * Browser event data collected from the user's session in DotCMS.
 * Contains comprehensive information about the user's browser environment,
 * page context, and session details for analytics tracking.
 *
 * This is an internal type used by utility functions.
 */
export interface DotCMSBrowserData {
    /** UTC timestamp when the event occurred */
    utc_time: string;
    /** Local timezone offset in minutes */
    local_tz_offset: number;
    /** Screen resolution as a string (e.g., "1920x1080") */
    screen_resolution: string;
    /** Viewport size as a string (e.g., "1200x800") */
    vp_size: string;
    /** User's preferred language */
    user_language: string;
    /** Document encoding */
    doc_encoding: string;
    /** Document path */
    doc_path: string;
    /** Document host */
    doc_host: string;
    /** Document protocol (http/https) */
    doc_protocol: string;
    /** Document hash fragment */
    doc_hash: string;
    /** Document search parameters */
    doc_search: string;
    /** Referrer URL */
    referrer: string;
    /** Page title */
    page_title: string;
    /** Current page URL */
    url: string;
    /** UTM parameters for campaign tracking */
    utm: DotCMSEventUtmData;
}

/**
 * Analytics context shared across all events in DotCMS.
 * Contains session and user identification data that provides
 * continuity across multiple analytics events.
 */
export interface DotCMSAnalyticsEventContext {
    /** The site key for the DotCMS instance */
    site_auth: string;
    /** Unique session identifier */
    session_id: string;
    /** Unique user identifier */
    user_id: string;

    /** Device and browser information */
    device: DotCMSEventDeviceData;
}

/**
 * Device and browser information for DotCMS analytics tracking.
 * Contains technical details about the user's device and browser environment.
 */
export interface DotCMSEventDeviceData {
    /** Screen resolution as a string (e.g., "1920x1080") */
    screen_resolution: string;
    /** User's preferred language */
    language: string;
    /** Viewport width in pixels */
    viewport_width: string;
    /** Viewport height in pixels */
    viewport_height: string;
}

/**
 * UTM (Urchin Tracking Module) parameters for DotCMS campaign tracking.
 * Contains marketing campaign attribution data extracted from URL parameters.
 */
export interface DotCMSEventUtmData {
    /** The marketing medium (e.g., email, social, cpc) */
    medium?: string;
    /** The traffic source (e.g., google, newsletter) */
    source?: string;
    /** The campaign name */
    campaign?: string;
    /** The campaign term or keyword */
    term?: string;
    /** The campaign content or ad variation */
    content?: string;
    /** The campaign ID for tracking specific campaigns */
    id?: string;
}

/**
 * Page data structure for DotCMS analytics (used in pageview events).
 * Contains comprehensive information about the current page and its context
 * within the DotCMS environment.
 */
export type DotCMSEventPageData = Pick<
    DotCMSBrowserData,
    'url' | 'doc_path' | 'doc_hash' | 'doc_search' | 'doc_host' | 'doc_protocol' | 'doc_encoding'
> & {
    /** Page title */
    title: string | undefined;
    /** Language identifier */
    language_id?: string;
    /** Persona identifier */
    persona?: string;
};

/**
 * Minimal page data for content impression events.
 * Contains only essential page information (title and url) to keep payload lightweight.
 */
export type DotCMSContentImpressionPageData = Pick<DotCMSEventPageData, 'title' | 'url'>;

/**
 * Data structure for content impression events.
 * Tracks when a contentlet becomes visible in the viewport.
 */
export interface DotCMSImpressionEventData {
    /** Contentlet identification data extracted from data-dot-analytics-* attributes */
    contentlet: {
        /** Unique identifier of the contentlet */
        identifier: string;
        /** Inode of the contentlet */
        inode: string;
        /** Content type name */
        contentType: string;
        /** Title of the contentlet */
        title: string;
        /** Base type of the contentlet (e.g., CONTENT, WIDGET) */
        baseType: string;
    };
    /** Viewport position and visibility metrics */
    viewport: {
        /** Percentage offset from top of viewport (0-100) */
        offsetPercentage: number;
        /** Percentage of element visible in viewport (0-1) */
        visibilityRatio: number;
    };
    /** Timing information about the impression */
    timing: {
        /** Time in milliseconds the element was continuously visible */
        dwellTime: number;
        /** ISO 8601 timestamp when the impression was fired */
        timestamp: string;
    };
}
