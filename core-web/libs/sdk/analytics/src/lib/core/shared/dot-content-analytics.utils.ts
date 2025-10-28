import { PageData } from 'analytics';

import {
    ANALYTICS_JS_DEFAULT_PROPERTIES,
    ANALYTICS_MINIFIED_SCRIPT_NAME,
    DEFAULT_SESSION_TIMEOUT_MINUTES,
    EXPECTED_UTM_KEYS,
    SESSION_STORAGE_KEY,
    USER_ID_KEY
} from './constants';
import {
    AnalyticsBasePayloadWithContext,
    DotCMSAnalyticsConfig,
    DotCMSAnalyticsEventContext,
    DotCMSBrowserData,
    DotCMSEventDeviceData,
    DotCMSEventPageData,
    DotCMSEventUtmData,
    EnrichedAnalyticsPayload,
    JsonObject
} from './models';

// Export activity tracking functions from separate module
export {
    cleanupActivityTracking,
    getLastActivity,
    getSessionInfo,
    initializeActivityTracking,
    isUserInactive,
    updateSessionActivity
} from './dot-content-analytics.activity-tracker';

/**
 * Validates required configuration fields for Analytics initialization.
 *
 * @param config - The analytics configuration to validate
 * @returns Array of missing field names, or null if all required fields are present
 *
 * @example
 * ```ts
 * const missing = validateAnalyticsConfig(config);
 * if (missing) {
 *   console.error(`Missing: ${missing.join(' and ')}`);
 * }
 * ```
 */
export function validateAnalyticsConfig(config: DotCMSAnalyticsConfig): string[] | null {
    const missing: string[] = [];

    if (!config.siteAuth?.trim()) missing.push('"siteAuth"');
    if (!config.server?.trim()) missing.push('"server"');

    return missing.length > 0 ? missing : null;
}

// Performance cache for static browser data that rarely changes
let staticBrowserData: Pick<
    DotCMSBrowserData,
    'user_language' | 'doc_encoding' | 'screen_resolution'
> | null = null;

// UTM parameters cache to avoid repetitive URL parsing
let utmCache: { search: string; params: DotCMSEventUtmData } | null = null;

/**
 * Generates a cryptographically secure random ID.
 * @internal This function is for internal use only and should not be used outside of the SDK.
 * @param prefix - The prefix for the generated ID
 * @returns A unique ID string with the given prefix
 */
export const generateSecureId = (prefix: string): string => {
    const timestamp = Date.now();
    const randomPart = Math.random().toString(36).substr(2, 9);
    const extraRandom = Math.random().toString(36).substr(2, 9);

    return `${prefix}_${timestamp}_${randomPart}${extraRandom}`;
};

/**
 * Safe localStorage wrapper with error handling.
 * @internal This is for internal use only.
 */
const safeLocalStorage = {
    getItem: (key: string): string | null => {
        try {
            return localStorage.getItem(key);
        } catch {
            return null;
        }
    },
    setItem: (key: string, value: string): void => {
        try {
            localStorage.setItem(key, value);
        } catch {
            console.warn(`DotCMS Analytics: Could not save ${key} to localStorage`);
        }
    }
};

/**
 * Safe sessionStorage wrapper with error handling
 */
export const safeSessionStorage = {
    getItem: (key: string): string | null => {
        try {
            return sessionStorage.getItem(key);
        } catch {
            return null;
        }
    },
    setItem: (key: string, value: string): void => {
        try {
            sessionStorage.setItem(key, value);
        } catch {
            console.warn(`DotCMS Analytics: Could not save ${key} to sessionStorage`);
        }
    }
};

/**
 * Gets or generates a user ID from localStorage.
 * @internal This function is for internal use only.
 * @returns The user ID string
 */
export const getUserId = (): string => {
    let userId = safeLocalStorage.getItem(USER_ID_KEY);

    if (!userId) {
        userId = generateSecureId('user');
        safeLocalStorage.setItem(USER_ID_KEY, userId);
    }

    return userId;
};

/**
 * Checks if a new day has started since session creation
 */
const hasPassedMidnight = (sessionStartTime: number): boolean => {
    const sessionStart = new Date(sessionStartTime);
    const now = new Date();

    const sessionStartUTC = new Date(
        sessionStart.getUTCFullYear(),
        sessionStart.getUTCMonth(),
        sessionStart.getUTCDate()
    );
    const nowUTC = new Date(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate());

    return sessionStartUTC.getTime() !== nowUTC.getTime();
};

/**
 * Gets session ID with comprehensive lifecycle management.
 * Returns existing valid session ID or creates a new one if needed.
 * @internal This function is for internal use only.
 *
 * Session validation criteria:
 * 1. User is still active (< 30 min inactivity)
 * 2. Session hasn't passed midnight (UTC)
 *
 * @returns The session ID string
 */
export const getSessionId = (): string => {
    const now = Date.now();

    // Early return if window is not available (SSR/build time)
    if (typeof window === 'undefined') {
        return generateSecureId('session_fallback');
    }

    try {
        const sessionData = sessionStorage.getItem(SESSION_STORAGE_KEY);

        if (sessionData) {
            const { sessionId, startTime, lastActivity } = JSON.parse(sessionData);

            // Check if session is still valid
            const hasNotPassedMidnight = !hasPassedMidnight(startTime);
            const hasRecentActivity =
                now - lastActivity < DEFAULT_SESSION_TIMEOUT_MINUTES * 60 * 1000;

            if (hasNotPassedMidnight && hasRecentActivity) {
                // Update last activity time
                sessionStorage.setItem(
                    SESSION_STORAGE_KEY,
                    JSON.stringify({
                        sessionId,
                        startTime,
                        lastActivity: now
                    })
                );

                return sessionId;
            }
        }

        // Create new session
        const newSessionId = generateSecureId('session');
        const sessionDataToStore = {
            sessionId: newSessionId,
            startTime: now,
            lastActivity: now
        };

        sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(sessionDataToStore));

        return newSessionId;
    } catch {
        // Fallback to simple session ID if storage fails
        return generateSecureId('session_fallback');
    }
};

/**
 * Gets analytics context with user and session identification.
 * Used by the identity plugin to inject context into analytics events.
 *
 * @param config - The analytics configuration object
 * @returns The analytics context with site_key, session_id, and user_id
 */
export const getAnalyticsContext = (config: DotCMSAnalyticsConfig): DotCMSAnalyticsEventContext => {
    const sessionId = getSessionId();
    const userId = getUserId();
    const device = getDeviceDataForContext();

    if (config.debug) {
        console.warn('DotCMS Analytics Identity Context:', {
            sessionId,
            userId
        });
    }

    return {
        site_auth: config.siteAuth,
        session_id: sessionId,
        user_id: userId,
        device
    };
};

/**
 * Configuration result with warnings for analytics setup
 */
export interface AnalyticsConfigResult {
    config: DotCMSAnalyticsConfig;
    warnings?: string[];
    missingAttributes?: string[];
    hasIssues: boolean;
}

/**
 * Gets analytics configuration from script tag attributes.
 * Always returns a config (with defaults if needed).
 *
 * - If no data-analytics-server attribute is found, uses the current domain as the server endpoint
 * - Both debug and autoPageView default to false (must be explicitly set to "true")
 *
 * @returns The analytics configuration object
 */
export const getAnalyticsConfig = (): DotCMSAnalyticsConfig => {
    // Try to find the analytics script with data-analytics-auth (required attribute)
    const script = document.querySelector(
        `script[src*="${ANALYTICS_MINIFIED_SCRIPT_NAME}"][data-analytics-auth]`
    ) as HTMLScriptElement;

    if (script) {
        return {
            server: script.getAttribute('data-analytics-server') || window.location.origin,
            debug: script.getAttribute('data-analytics-debug') === 'true',
            autoPageView: script.getAttribute('data-analytics-auto-page-view') === 'true',
            siteAuth: script.getAttribute('data-analytics-auth') || ''
        };
    }

    // No script found, return defaults with current domain as server
    return {
        server: window.location.origin,
        debug: false,
        autoPageView: false,
        siteAuth: ''
    };
};

/**
 * Gets static browser data (cached for performance)
 */
const getStaticBrowserData = () => {
    if (staticBrowserData) {
        return staticBrowserData;
    }

    staticBrowserData = {
        user_language: navigator.language,
        doc_encoding: document.characterSet || document.charset,
        screen_resolution:
            typeof screen !== 'undefined' && screen.width && screen.height
                ? `${screen.width}x${screen.height}`
                : ''
    };

    return staticBrowserData;
};

/**
 * Gets current device data for analytics.
 * Combines static browser data with dynamic viewport information.
 * Used by the identity plugin to inject device data into context.
 *
 * @returns Device data with screen resolution, language, and viewport dimensions
 */
export const getDeviceDataForContext = (): DotCMSEventDeviceData => {
    const staticData = getStaticBrowserData();
    const viewportWidth = window.innerWidth || document.documentElement.clientWidth || 0;
    const viewportHeight = window.innerHeight || document.documentElement.clientHeight || 0;

    return {
        screen_resolution: staticData.screen_resolution ?? '',
        language: staticData.user_language ?? '',
        viewport_width: String(viewportWidth),
        viewport_height: String(viewportHeight)
    };
};

/**
 * Retrieves the browser event data - optimized but accurate.
 * @internal This function is for internal use only.
 * @param location - The Location object to extract data from
 * @returns Browser event data with all relevant information
 */
export const getBrowserEventData = (location: Location): DotCMSBrowserData => {
    const staticData = getStaticBrowserData();

    const viewportWidth = window.innerWidth || document.documentElement.clientWidth || 0;
    const viewportHeight = window.innerHeight || document.documentElement.clientHeight || 0;

    const utmParams = extractUTMParameters(location);

    return {
        utc_time: new Date().toISOString(),
        local_tz_offset: new Date().getTimezoneOffset(),
        screen_resolution: staticData.screen_resolution,
        vp_size: `${viewportWidth}x${viewportHeight}`,
        user_language: staticData.user_language ?? '',
        doc_encoding: staticData.doc_encoding ?? '',
        doc_path: location.pathname,
        doc_host: location.hostname,
        doc_protocol: location.protocol,
        doc_hash: location.hash || '',
        doc_search: location.search || '',
        referrer: typeof document !== 'undefined' ? document.referrer || '' : '',
        page_title: document.title,
        url: location.href,
        utm: utmParams
    };
};

/**
 * Extracts and transforms UTM parameters from the URL - cached for performance.
 * Returns UTM data in DotCMS format (without 'utm_' prefix).
 * @internal This function is for internal use only.
 * @param location - The Location object to extract UTM parameters from
 * @returns DotCMSEventUtmData object with transformed UTM parameters (source, medium, campaign, etc.)
 */
export const extractUTMParameters = (location: Location): DotCMSEventUtmData => {
    const search = location.search;

    // Return cached result if search hasn't changed
    if (utmCache && utmCache.search === search) {
        return utmCache.params as DotCMSEventUtmData;
    }

    const urlParams = new URLSearchParams(search);
    const utmData: DotCMSEventUtmData = {};

    EXPECTED_UTM_KEYS.forEach((key) => {
        const value = urlParams.get(key);
        if (value) {
            // Transform utm_source -> source, utm_medium -> medium, etc.
            const cleanKey = key.replace('utm_', '') as keyof DotCMSEventUtmData;
            utmData[cleanKey] = value;
        }
    });

    // Cache the result
    utmCache = { search, params: utmData };

    return utmData;
};

/**
 * Default redirect function.
 * @internal This function is for internal use only.
 * @param href - The URL to redirect to
 */
export const defaultRedirectFn = (href: string) => (window.location.href = href);

/**
 * Gets timezone offset in the format +HH:mm or -HH:mm
 */
const getTimezoneOffset = (): string => {
    try {
        const offset = new Date().getTimezoneOffset();
        const sign = offset > 0 ? '-' : '+';
        const absOffset = Math.abs(offset);
        const hours = Math.floor(absOffset / 60);
        const minutes = absOffset % 60;

        return `${sign}${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`;
    } catch {
        return '+00:00';
    }
};

/**
 * Gets local time in ISO format without milliseconds.
 * Used by enricher plugins to add local_time to events.
 *
 * @returns Local time string in ISO 8601 format with timezone offset (e.g., "2024-01-01T12:00:00-05:00")
 */
export const getLocalTime = (): string => {
    try {
        const now = new Date();
        const timezoneOffset = getTimezoneOffset();

        // Format: YYYY-MM-DDTHH:mm:ss+HH:mm (without milliseconds)
        const year = now.getFullYear();
        const month = (now.getMonth() + 1).toString().padStart(2, '0');
        const day = now.getDate().toString().padStart(2, '0');
        const hours = now.getHours().toString().padStart(2, '0');
        const minutes = now.getMinutes().toString().padStart(2, '0');
        const seconds = now.getSeconds().toString().padStart(2, '0');

        return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}${timezoneOffset}`;
    } catch {
        return new Date().toISOString();
    }
};

/**
 * Gets page data from browser event data and payload.
 * @internal This function is for internal use only.
 * @param browserData - Browser event data
 * @param payload - Payload with properties
 * @returns PageData object for Analytics.js
 */
export const getPageData = (
    browserData: DotCMSBrowserData,
    payload: { properties: Record<string, unknown> }
): PageData => {
    const payloadProperties = payload.properties;

    return {
        url: browserData.url,
        path: browserData.doc_path,
        hash: browserData.doc_hash,
        search: browserData.doc_search,
        title: browserData.page_title ?? (payloadProperties.title as string),
        width: String(payloadProperties.width),
        height: String(payloadProperties.height),
        referrer: browserData.referrer
    };
};

/**
 * Gets device data from browser event data.
 * @internal This function is for internal use only.
 * @param browserData - Browser event data
 * @returns Device data with screen resolution, language, and viewport dimensions
 */
export const getDeviceData = (browserData: DotCMSBrowserData): DotCMSEventDeviceData => {
    const [viewportWidth, viewportHeight] = (browserData.vp_size ?? '0x0').split('x');

    return {
        screen_resolution: browserData.screen_resolution,
        language: browserData.user_language,
        viewport_width: viewportWidth,
        viewport_height: viewportHeight
    };
};

/**
 * Gets UTM data from browser event data.
 * @internal This function is for internal use only.
 * @param browserData - Browser event data
 * @returns UTM data with source, medium, campaign, etc.
 */
export const getUtmData = (browserData: DotCMSBrowserData): DotCMSEventUtmData => {
    // UTM data is already in DotCMS format (transformed by extractUTMParameters)
    return browserData.utm || {};
};

/**
 * Enriches payload with UTM data.
 * @internal This function is for internal use only.
 * @param payload - The payload to enrich
 * @returns The payload with UTM data added
 */
export const enrichWithUtmData = <T extends Record<string, unknown>>(payload: T) => {
    const browserData = getBrowserEventData(window.location);
    const utmData = getUtmData(browserData);

    if (Object.keys(utmData).length > 0) {
        return {
            ...payload,
            utm: utmData
        };
    }

    return payload;
};

/**
 * Optimized payload enrichment using existing analytics.js data.
 * Filters out Analytics.js default properties and only keeps user-provided properties in custom.
 * Used by the enricher plugin to transform Analytics.js payload into DotCMS event format.
 *
 * @param payload - The Analytics.js payload with context already injected by identity plugin
 * @param location - The Location object to extract page data from (defaults to window.location)
 * @returns Enriched payload with page, UTM, custom data, and local_time (device is in context)
 */
export const enrichPagePayloadOptimized = (
    payload: AnalyticsBasePayloadWithContext,
    location: Location = typeof window !== 'undefined' ? window.location : ({} as Location)
): EnrichedAnalyticsPayload => {
    const local_time = getLocalTime();
    const staticData = getStaticBrowserData();

    // Extract data from analytics.js payload
    const { properties } = payload;

    // Filter out Analytics.js default properties to get only user-provided properties
    const userProvidedProperties: JsonObject = {};
    Object.keys(properties).forEach((key) => {
        if (!(ANALYTICS_JS_DEFAULT_PROPERTIES as readonly string[]).includes(key)) {
            userProvidedProperties[key] = properties[key as keyof typeof properties];
        }
    });

    const pageData: DotCMSEventPageData = {
        url: location.href,
        doc_encoding: staticData.doc_encoding,
        doc_hash: location.hash,
        doc_protocol: location.protocol,
        doc_search: location.search,
        doc_host: location.hostname,
        doc_path: location.pathname,
        title: (properties.title as string) ?? document?.title
    };

    // Extract UTM parameters from the current URL (already transformed to DotCMS format)
    const utmData = extractUTMParameters(location);

    return {
        ...payload,
        page: pageData,
        ...(Object.keys(utmData).length > 0 && { utm: utmData }),
        // Only include custom if there are user-provided properties
        ...(Object.keys(userProvidedProperties).length > 0 && { custom: userProvidedProperties }),
        local_time
    };
};

/**
 * Legacy function that enriches page payload with all data in one call.
 * @internal This function is for internal use only.
 * @param payload - The payload to enrich
 * @param location - The Location object to extract data from
 * @returns Object with enriched payload
 */
export const enrichPagePayload = (
    payload: { properties: Record<string, unknown> } & Record<string, unknown>,
    location: Location = window.location
) => {
    const browserData = getBrowserEventData(location);
    const pageData = getPageData(browserData, payload);
    const deviceData = getDeviceData(browserData);
    const utmData = getUtmData(browserData);
    const local_time = getLocalTime();

    return {
        payload: {
            ...payload,
            page: pageData,
            device: deviceData,
            ...(Object.keys(utmData).length > 0 && { utm: utmData }),
            local_time
        }
    };
};
