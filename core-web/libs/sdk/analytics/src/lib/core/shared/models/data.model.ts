/**
 * Data models for DotCMS Analytics
 * Contains interfaces for data structures used in analytics events
 */

/**
 * Analytics context shared across all events in DotCMS.
 * Contains session and user identification data that provides
 * continuity across multiple analytics events.
 */
export interface DotCMSAnalyticsContext {
    /** The site key for the DotCMS instance */
    site_key: string;
    /** Unique session identifier */
    session_id: string;
    /** Unique user identifier */
    user_id: string;
}

/**
 * Device and browser information for DotCMS analytics tracking.
 * Contains technical details about the user's device and browser environment.
 */
export interface DotCMSDeviceData {
    /** Screen resolution as a string (e.g., "1920x1080") */
    screen_resolution: string | undefined;
    /** User's preferred language */
    language: string | undefined;
    /** Viewport width in pixels */
    viewport_width: string | undefined;
    /** Viewport height in pixels */
    viewport_height: string | undefined;
}

/**
 * UTM (Urchin Tracking Module) parameters for DotCMS campaign tracking.
 * Contains marketing campaign attribution data extracted from URL parameters.
 */
export interface DotCMSUtmData {
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
 * Page data structure for DotCMS analytics.
 * Contains comprehensive information about the current page and its context
 * within the DotCMS environment.
 */
export interface DotCMSPageData {
    /** The current page URL */
    url: string | undefined;
    /** Document encoding */
    doc_encoding: string | undefined;
    /** Document hash fragment */
    doc_hash: string;
    /** Document protocol (http/https) */
    doc_protocol: string | undefined;
    /** Document search parameters */
    doc_search: string;
    /** Document host domain */
    doc_host: string | undefined;
    /** Document path */
    doc_path: string | undefined;
    /** Page title */
    title: string | undefined;
    /** Language identifier */
    language_id?: string;
    /** Persona identifier */
    persona?: string;
}
