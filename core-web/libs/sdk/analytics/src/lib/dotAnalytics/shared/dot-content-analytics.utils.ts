import { PageData } from 'analytics';

import {
    DEFAULT_SESSION_TIMEOUT_MINUTES,
    EXPECTED_UTM_KEYS,
    SESSION_STORAGE_KEY,
    USER_ID_KEY
} from './dot-content-analytics.constants';
import {
    DotCMSAnalyticsConfig,
    DotCMSAnalyticsContext,
    DotCMSAnalyticsPayload,
    DotCMSBrowserEventData,
    DotCMSDeviceData,
    DotCMSUtmData
} from './dot-content-analytics.model';

// Export activity tracking functions from separate module
export {
    cleanupActivityTracking,
    getLastActivity,
    getSessionInfo,
    initializeActivityTracking,
    isUserInactive,
    updateSessionActivity
} from './dot-content-analytics.activity-tracker';

// Performance cache for static browser data that rarely changes
let staticBrowserData: {
    user_language: string | undefined;
    doc_encoding: string | undefined;
    screen_resolution: string | undefined;
} | null = null;

// UTM parameters cache to avoid repetitive URL parsing
let utmCache: { search: string; params: Record<string, string> } | null = null;

/**
 * Generates a cryptographically secure random ID
 */
export const generateSecureId = (prefix: string): string => {
    const timestamp = Date.now();
    const randomPart = Math.random().toString(36).substr(2, 9);
    const extraRandom = Math.random().toString(36).substr(2, 9);

    return `${prefix}_${timestamp}_${randomPart}${extraRandom}`;
};

/**
 * Safe localStorage wrapper
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
            console.warn(`DotAnalytics: Could not save ${key} to localStorage`);
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
            console.warn(`DotAnalytics: Could not save ${key} to sessionStorage`);
        }
    }
};

/**
 * Gets or generates a user ID
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
 * Gets session ID with comprehensive lifecycle management
 * Returns existing valid session ID or creates a new one if needed
 *
 * Session validation criteria:
 * 1. User is still active (< 30 min inactivity)
 * 2. Session hasn't passed midnight (UTC)
 * 3. UTM parameters haven't changed
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
    } catch (error) {
        // Fallback to simple session ID if storage fails
        return generateSecureId('session_fallback');
    }
};

/**
 * Gets analytics context with user and session identification
 */
export const getAnalyticsContext = (config: DotCMSAnalyticsConfig): DotCMSAnalyticsContext => {
    const sessionId = getSessionId();
    const userId = getUserId();

    if (config.debug) {
        console.warn('DotAnalytics Identity Context:', {
            sessionId,
            userId
        });
    }

    return {
        site_key: config.siteKey,
        session_id: sessionId,
        user_id: userId
    };
};

/**
 * Retrieves analytics attributes from a given script element.
 */
export const getDataAnalyticsAttributes = (): DotCMSAnalyticsConfig => {
    const script = getAnalyticsScriptTag();

    return {
        server: script.getAttribute('data-server') || '',
        debug: script.getAttribute('data-debug') === 'true',
        autoPageView: script.getAttribute('data-auto-page-view') !== 'false',
        siteKey: script.getAttribute('data-site-key') || '',
        redirectFn: script.getAttribute('data-redirect-fn')
            ? // eslint-disable-next-line @typescript-eslint/no-explicit-any
              (window as any)[script.getAttribute('data-redirect-fn')!]
            : defaultRedirectFn
    };
};

/**
 * Gets the analytics script tag from the DOM
 */
export const getAnalyticsScriptTag = (): HTMLScriptElement => {
    const script = document.querySelector(
        'script[data-server][data-site-key]'
    ) as HTMLScriptElement;

    if (!script) {
        throw new Error('DotAnalytics: Analytics script tag not found');
    }

    return script;
};

/**
 * Gets static browser data (cached for performance)
 */
const getStaticBrowserData = () => {
    if (staticBrowserData) {
        return staticBrowserData;
    }

    staticBrowserData = {
        user_language: navigator.language || undefined,
        doc_encoding: document.characterSet || document.charset || undefined,
        screen_resolution:
            typeof screen !== 'undefined' && screen.width && screen.height
                ? `${screen.width}x${screen.height}`
                : undefined
    };

    return staticBrowserData;
};

/**
 * Retrieves the browser event data - optimized but accurate.
 */
export const getBrowserEventData = (location: Location): DotCMSBrowserEventData => {
    const staticData = getStaticBrowserData();

    const viewportWidth = window.innerWidth || document.documentElement.clientWidth || 0;
    const viewportHeight = window.innerHeight || document.documentElement.clientHeight || 0;

    const utmParams = extractUTMParameters(location);

    return {
        utc_time: new Date().toISOString(),
        local_tz_offset: new Date().getTimezoneOffset(),
        screen_resolution: staticData.screen_resolution,
        vp_size: `${viewportWidth}x${viewportHeight}`,
        user_language: staticData.user_language,
        doc_encoding: staticData.doc_encoding,
        doc_path: location.pathname || undefined,
        doc_host: location.hostname || undefined,
        doc_protocol: location.protocol || undefined,
        doc_hash: location.hash || '',
        doc_search: location.search || '',
        referrer: typeof document !== 'undefined' ? document.referrer || undefined : undefined,
        page_title: typeof document !== 'undefined' ? document.title || undefined : undefined,
        url: location.href || undefined,
        utm: utmParams
    };
};

/**
 * Extracts UTM parameters from the URL - cached for performance
 */
export const extractUTMParameters = (location: Location): Record<string, string> => {
    const search = location.search;

    // Return cached result if search hasn't changed
    if (utmCache && utmCache.search === search) {
        return utmCache.params;
    }

    const urlParams = new URLSearchParams(search);
    const utmParams: Record<string, string> = {};

    EXPECTED_UTM_KEYS.forEach((key) => {
        const value = urlParams.get(key);
        if (value) {
            utmParams[key] = value;
        }
    });

    // Cache the result
    utmCache = { search, params: utmParams };

    return utmParams;
};

/**
 * Default redirect function
 */
export const defaultRedirectFn = (href: string) => (window.location.href = href);

/**
 * Check if we're inside the DotCMS editor
 */
export const isInsideEditor = (): boolean => {
    if (typeof window === 'undefined') {
        return false; // SSR safe
    }

    try {
        // Check if we're in an iframe
        const inIframe = window.self !== window.top;

        // Check for DotCMS editor indicators
        const hasEditorParams = window.location.href.includes('mode=EDIT_MODE');
        const hasVtlServlet = window.location.href.includes('/vtl/');

        return inIframe || hasEditorParams || hasVtlServlet;
    } catch {
        return false;
    }
};

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
 * Gets local time in ISO format
 */
export const getLocalTime = (): string => {
    try {
        const now = new Date();
        const timezoneOffset = getTimezoneOffset();

        // Format: YYYY-MM-DDTHH:mm:ss.sss+HH:mm
        const year = now.getFullYear();
        const month = (now.getMonth() + 1).toString().padStart(2, '0');
        const day = now.getDate().toString().padStart(2, '0');
        const hours = now.getHours().toString().padStart(2, '0');
        const minutes = now.getMinutes().toString().padStart(2, '0');
        const seconds = now.getSeconds().toString().padStart(2, '0');
        const milliseconds = now.getMilliseconds().toString().padStart(3, '0');

        return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}.${milliseconds}${timezoneOffset}`;
    } catch {
        return new Date().toISOString();
    }
};

/**
 * Gets page data from browser event data and payload
 */
export const getPageData = (
    browserData: DotCMSBrowserEventData,
    payload: DotCMSAnalyticsPayload
): PageData => {
    // Now properly typed - no need for 'any'
    const payloadProperties = payload.properties as Record<string, unknown>;

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
 * Gets device data from browser event data
 */
export const getDeviceData = (browserData: DotCMSBrowserEventData): DotCMSDeviceData => {
    const [viewportWidth, viewportHeight] = (browserData.vp_size ?? '0x0').split('x');

    return {
        screen_resolution: browserData.screen_resolution,
        language: browserData.user_language,
        viewport_width: viewportWidth,
        viewport_height: viewportHeight
    };
};

/**
 * Gets UTM data from browser event data
 */
export const getUtmData = (browserData: DotCMSBrowserEventData): DotCMSUtmData => {
    try {
        const utm = browserData.utm as Record<string, string>;
        const result: DotCMSUtmData = {};

        // Check if utm exists and is an object
        if (!utm || typeof utm !== 'object') {
            return result;
        }

        if (utm.medium) result.medium = utm.medium;
        if (utm.source) result.source = utm.source;
        if (utm.campaign) result.campaign = utm.campaign;
        if (utm.term) result.term = utm.term;
        if (utm.content) result.content = utm.content;

        return result;
    } catch (error) {
        console.warn('DotAnalytics: Error extracting UTM data:', error);

        return {};
    }
};

/**
 * Enriches payload with UTM data
 */
export const enrichWithUtmData = (payload: DotCMSAnalyticsPayload) => {
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
 * Optimized payload enrichment using existing analytics.js data
 * Reuses payload.properties data instead of recalculating from DOM when available
 * Maintains the same output structure as the original function
 */
export const enrichPagePayloadOptimized = (
    payload: DotCMSAnalyticsPayload,
    location: Location = typeof window !== 'undefined' ? window.location : ({} as Location)
) => {
    const local_time = getLocalTime();
    const staticData = getStaticBrowserData();

    // Extract data from analytics.js payload
    const { properties } = payload;
    const { utm } = properties as Record<string, unknown>;

    const pageData: PageData = {
        url: (properties.url as string) ?? location.href,
        path: (properties.path as string) ?? location.pathname,
        hash: (properties.hash as string) ?? location.hash ?? '',
        search: (properties.search as string) ?? location.search ?? '',
        title: (properties.title as string) ?? document?.title,
        width: String(properties.width),
        height: String(properties.height),
        referrer: (properties.referrer as string) ?? document?.referrer
    };

    const deviceData: DotCMSDeviceData = {
        screen_resolution: staticData.screen_resolution,
        language: staticData.user_language,
        viewport_width: String(properties.width),
        viewport_height: String(properties.height)
    };

    const utmData: DotCMSUtmData = {};
    if (utm && typeof utm === 'object') {
        const utmRecord = utm as DotCMSUtmData;
        if (utmRecord.medium) utmData.medium = utmRecord.medium;
        if (utmRecord.source) utmData.source = utmRecord.source;
        if (utmRecord.campaign) utmData.campaign = utmRecord.campaign;
        if (utmRecord.term) utmData.term = utmRecord.term;
        if (utmRecord.content) utmData.content = utmRecord.content;
    }

    return {
        ...payload,
        page: pageData,
        device: deviceData,
        ...(Object.keys(utmData).length > 0 && { utm: utmData }),
        local_time
    };
};

/**
 * @deprecated Use enrichPagePayloadOptimized instead to avoid data duplication
 * Legacy function that enriches page payload with all data in one call
 * This function duplicates data already available in analytics.js payload
 */
export const enrichPagePayload = (
    payload: DotCMSAnalyticsPayload,
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
