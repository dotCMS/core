import { PageData } from 'analytics';

import {
    ACTIVITY_EVENTS,
    DEFAULT_SESSION_TIMEOUT_MINUTES,
    EXPECTED_UTM_KEYS,
    SESSION_START_KEY,
    SESSION_STORAGE_KEY,
    SESSION_UTM_KEY,
    USER_ID_KEY
} from './dot-content-analytics.constants';
import {
    BrowserEventData,
    DeviceData,
    DotAnalyticsContext,
    DotAnalyticsPayload,
    DotContentAnalyticsConfig,
    UtmData
} from './dot-content-analytics.model';

// Activity tracking state
let activityListeners: (() => void)[] = [];
let lastActivityTime = Date.now();
let inactivityTimer: NodeJS.Timeout | null = null;
let isThrottled = false;
const ACTIVITY_THROTTLE_MS = 1000; // Throttle activity events to max 1 per second

// Performance cache for static browser data that rarely changes
let staticBrowserData: {
    user_language: string | null;
    doc_encoding: string | null;
    screen_resolution: string | null;
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
 * Updates activity timestamp (throttled for performance)
 */
const updateActivityTime = (): void => {
    lastActivityTime = Date.now();

    // Reset inactivity timer
    if (inactivityTimer) {
        clearTimeout(inactivityTimer);
    }

    inactivityTimer = setTimeout(
        () => {
            // User became inactive - timer fired
        },
        DEFAULT_SESSION_TIMEOUT_MINUTES * 60 * 1000
    );
};

/**
 * Checks if user has been inactive
 */
const isUserInactive = (): boolean => {
    const timeoutMs = DEFAULT_SESSION_TIMEOUT_MINUTES * 60 * 1000;

    return Date.now() - lastActivityTime > timeoutMs;
};

/**
 * Gets last activity time
 */
const getLastActivity = (): number => {
    return lastActivityTime;
};

/**
 * Compares UTM parameters to detect campaign changes
 */
const hasUTMChanged = (currentUTM: Record<string, string>): boolean => {
    try {
        const storedUTM = sessionStorage.getItem(SESSION_UTM_KEY);
        if (!storedUTM) {
            sessionStorage.setItem(SESSION_UTM_KEY, JSON.stringify(currentUTM));

            return false;
        }

        const previousUTM = JSON.parse(storedUTM);

        const significantParams = ['source', 'medium', 'campaign'];
        for (const param of significantParams) {
            if (currentUTM[param] !== previousUTM[param]) {
                sessionStorage.setItem(SESSION_UTM_KEY, JSON.stringify(currentUTM));

                return true;
            }
        }

        return false;
    } catch (error) {
        return false;
    }
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

    const currentUTM = extractUTMParameters(window.location);

    try {
        const existingSessionId = sessionStorage.getItem(SESSION_STORAGE_KEY);
        const sessionStart = sessionStorage.getItem(SESSION_START_KEY);

        if (existingSessionId && sessionStart) {
            const sessionStartTime = parseInt(sessionStart, 10);

            const hasNotPassedMidnight = !hasPassedMidnight(sessionStartTime);
            const hasNotChangedUTM = !hasUTMChanged(currentUTM);
            const isActive = !isUserInactive();

            if (isActive && hasNotPassedMidnight && hasNotChangedUTM) {
                updateActivityTime();

                return existingSessionId;
            }
        }

        // Create new session when validation fails
        const newSessionId = generateSecureId('session');
        sessionStorage.setItem(SESSION_STORAGE_KEY, newSessionId);
        sessionStorage.setItem(SESSION_START_KEY, now.toString());
        sessionStorage.setItem(SESSION_UTM_KEY, JSON.stringify(currentUTM));
        updateActivityTime();

        return newSessionId;
    } catch (error) {
        console.warn(
            'DotAnalytics: SessionStorage not available, using timestamp-based session ID'
        );

        return generateSecureId('session_fallback');
    }
};

/**
 * Updates session activity
 */
export const updateSessionActivity = (): void => {
    updateActivityTime();
};

/**
 * Gets session information for debugging
 */
export const getSessionInfo = () => {
    const sessionId = sessionStorage.getItem(SESSION_STORAGE_KEY) || '';
    const sessionStart = parseInt(sessionStorage.getItem(SESSION_START_KEY) || '0', 10);
    const userId = getUserId();

    return {
        sessionId,
        sessionStart,
        lastActivity: getLastActivity(),
        isInactive: isUserInactive(),
        userId
    };
};

/**
 * Initializes activity tracking (optimized for performance)
 */
export const initializeActivityTracking = (config: DotContentAnalyticsConfig): void => {
    cleanupActivityTracking();

    // Early return if window is not available (SSR/build time)
    if (typeof window === 'undefined') {
        return;
    }

    const throttledHandler = createThrottledActivityHandler(config);

    // Set up event listeners for basic activity events
    ACTIVITY_EVENTS.forEach((eventType: string) => {
        const cleanup = () => window.removeEventListener(eventType, throttledHandler);
        activityListeners.push(cleanup);
        window.addEventListener(eventType, throttledHandler, { passive: true });
    });

    // Handle visibilitychange separately for tab switching
    const handleVisibilityChange = () => {
        if (!document.hidden) {
            // User returned to the tab - reactivate session
            updateActivityTime();
            if (config.debug) {
                console.warn('DotAnalytics: User returned to tab, session reactivated');
            }
        }
        // Don't do anything when tab becomes hidden - only when it becomes visible
    };

    document.addEventListener('visibilitychange', handleVisibilityChange, { passive: true });
    activityListeners.push(() =>
        document.removeEventListener('visibilitychange', handleVisibilityChange)
    );

    updateActivityTime();
};

/**
 * Cleans up activity tracking
 */
export const cleanupActivityTracking = (): void => {
    activityListeners.forEach((cleanup) => cleanup());
    activityListeners = [];

    if (inactivityTimer) {
        clearTimeout(inactivityTimer);
        inactivityTimer = null;
    }
};

/**
 * Gets analytics context with user and session identification
 */
export const getAnalyticsContext = (config: DotContentAnalyticsConfig): DotAnalyticsContext => {
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
export const getDataAnalyticsAttributes = (location: Location): DotContentAnalyticsConfig => {
    const script = getAnalyticsScriptTag();

    const attributes = {
        server: script.getAttribute('data-analytics-server') || location.origin,
        debug: script.hasAttribute('data-analytics-debug'),
        autoPageView: script.hasAttribute('data-analytics-auto-page-view'),
        siteKey: script.getAttribute('data-analytics-site-key') || ''
    };

    return attributes;
};

/**
 * Retrieves the analytics script tag from the document.
 */
export const getAnalyticsScriptTag = (): HTMLScriptElement => {
    const scripts = document.querySelector('script[data-analytics-site-key]');

    if (!scripts) {
        throw new Error('Dot Analytics: Script not found');
    }

    return scripts as HTMLScriptElement;
};

/**
 * Gets static browser data that rarely changes (cached for performance)
 */
const getStaticBrowserData = () => {
    if (!staticBrowserData) {
        // Return null values when window is not available (SSR/build time)
        if (typeof window === 'undefined') {
            staticBrowserData = {
                user_language: null,
                doc_encoding: null,
                screen_resolution: null
            };
        } else {
            staticBrowserData = {
                user_language: navigator.language,
                doc_encoding: document.characterSet,
                screen_resolution: `${window.screen.width}x${window.screen.height}`
            };
        }
    }

    return staticBrowserData;
};

/**
 * Retrieves the browser event data - optimized but accurate.
 */
export const getBrowserEventData = (location: Location): BrowserEventData => {
    const staticData = getStaticBrowserData();

    // Return null values when window is not available (SSR/build time)
    if (typeof window === 'undefined') {
        return {
            utc_time: new Date().toISOString(),
            local_tz_offset: new Date().getTimezoneOffset(),
            vp_size: null,
            page_title: null,
            referrer: null,
            doc_path: location?.pathname || null,
            doc_host: location?.hostname || null,
            doc_protocol: location?.protocol || null,
            doc_hash: location?.hash || '',
            doc_search: location?.search || '',
            url: location?.href || null,
            utm: {},
            ...staticData
        };
    }

    // Always get fresh data for dynamic properties
    return {
        // Always fresh - critical for analytics accuracy
        utc_time: new Date().toISOString(),
        local_tz_offset: new Date().getTimezoneOffset(),
        vp_size: `${window.innerWidth}x${window.innerHeight}`, // Can change with resize
        page_title: document.title, // Can change dynamically
        referrer: document.referrer, // Can change with navigation
        doc_path: location.pathname,
        doc_host: location.hostname,
        doc_protocol: location.protocol,
        doc_hash: location.hash,
        doc_search: location.search,
        url: location.href,
        utm: extractUTMParameters(location),

        // Cached static data - safe to cache
        ...staticData
    };
};

/**
 * Extracts UTM parameters from a given URL location with caching.
 */
export const extractUTMParameters = (location: Location): Record<string, string> => {
    // Return cached UTM if search hasn't changed
    if (utmCache && utmCache.search === location.search) {
        return utmCache.params;
    }

    const urlParams = new URLSearchParams(location.search);

    const params = EXPECTED_UTM_KEYS.reduce(
        (acc, key) => {
            const value = urlParams.get(key);
            if (value !== null) {
                acc[key.replace('utm_', '')] = value;
            }

            return acc;
        },
        {} as Record<string, string>
    );

    // Cache the result
    utmCache = {
        search: location.search,
        params
    };

    return params;
};

/**
 * A function to redirect the user to a new URL.
 */
export const defaultRedirectFn = (href: string) => (window.location.href = href);

/**
 * Checks if the current environment is inside the dotCMS editor.
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
 * Gets the timezone offset in ISO 8601 format (e.g., "-05:00", "+02:00", "Z")
 * @returns The timezone offset string
 */
const getTimezoneOffset = (): string => {
    const now = new Date();
    const offsetMinutes = now.getTimezoneOffset();

    // If offset is 0, return "Z" for UTC
    if (offsetMinutes === 0) {
        return 'Z';
    }

    // Convert minutes to hours and minutes
    const offsetHours = Math.floor(Math.abs(offsetMinutes) / 60);
    const offsetMins = Math.abs(offsetMinutes) % 60;

    // Determine sign (getTimezoneOffset returns positive for west of UTC)
    const sign = offsetMinutes > 0 ? '-' : '+';

    // Format as ±HH:MM
    return `${sign}${String(offsetHours).padStart(2, '0')}:${String(offsetMins).padStart(2, '0')}`;
};

/**
 * Gets the local time in ISO 8601 format with timezone offset
 * Examples: "2025-06-09T14:30:00-05:00", "2025-06-09T14:30:00+02:00", "2025-06-09T14:30:00Z"
 * Uses modern browser APIs with fallback for compatibility
 */
export const getLocalTime = (): string => {
    const now = new Date();

    try {
        // Use Intl.DateTimeFormat for consistent formatting
        const formatter = new Intl.DateTimeFormat('sv-SE', {
            year: 'numeric',
            month: '2-digit',
            day: '2-digit',
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit',
            hour12: false
        });

        const localDateTime = formatter.format(now).replace(' ', 'T');
        const timezoneOffset = getTimezoneOffset();

        return `${localDateTime}${timezoneOffset}`;
    } catch (error) {
        // Fallback: Manual construction for older browsers
        const year = now.getFullYear();
        const month = String(now.getMonth() + 1).padStart(2, '0');
        const day = String(now.getDate()).padStart(2, '0');
        const hours = String(now.getHours()).padStart(2, '0');
        const minutes = String(now.getMinutes()).padStart(2, '0');
        const seconds = String(now.getSeconds()).padStart(2, '0');
        const timezoneOffset = getTimezoneOffset();

        return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}${timezoneOffset}`;
    }
};

/**
 * Gets page data from browser event data and payload
 */
export const getPageData = (
    browserData: BrowserEventData,
    payload: DotAnalyticsPayload
): PageData => {
    // Now properly typed - no need for 'any'
    const { language_id, persona } = payload.properties;

    return {
        url: browserData.url || '',
        doc_encoding: browserData.doc_encoding || '',
        title: browserData.page_title || '',
        language_id,
        persona,
        dot_path: browserData.doc_path,
        dot_host: browserData.doc_host,
        doc_protocol: browserData.doc_protocol,
        doc_hash: browserData.doc_hash,
        doc_search: browserData.doc_search
    };
};

/**
 * Gets device data from browser event data
 */
export const getDeviceData = (browserData: BrowserEventData): DeviceData => {
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
export const getUtmData = (browserData: BrowserEventData): UtmData => {
    const utm = browserData.utm as Record<string, string>;
    const result: UtmData = {};

    if (utm.medium) result.medium = utm.medium;
    if (utm.source) result.source = utm.source;
    if (utm.campaign) result.campaign = utm.campaign;
    if (utm.term) result.term = utm.term;
    if (utm.content) result.content = utm.content;

    return result;
};

/**
 * Enriches payload with UTM data
 */
export const enrichWithUtmData = (payload: DotAnalyticsPayload) => {
    const browserData = getBrowserEventData(window.location);
    const utmData = getUtmData(browserData);

    if (Object.keys(utmData).length > 0) {
        return {
            ...payload,
            properties: {
                ...payload.properties,
                utm: utmData
            }
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
    payload: DotAnalyticsPayload,
    location: Location = typeof window !== 'undefined' ? window.location : ({} as Location)
) => {
    const staticData = getStaticBrowserData();
    const local_time = getLocalTime();
    const utm = extractUTMParameters(location);

    const pageData: PageData = {
        url: (payload.properties.url as string) || location.href,
        title: (payload.properties.title as string) || document.title,
        doc_encoding: staticData.doc_encoding,
        language_id: payload.properties.language_id as string,
        persona: payload.properties.persona as string,
        dot_path: (payload.properties.path as string) || location.pathname,
        dot_host: location.hostname,
        doc_protocol: location.protocol,
        doc_hash: (payload.properties.hash as string) || location.hash,
        doc_search: (payload.properties.search as string) || location.search
    };

    const deviceData: DeviceData = {
        screen_resolution: staticData.screen_resolution,
        language: staticData.user_language,
        viewport_width: payload.properties.width?.toString() || window.innerWidth.toString(),
        viewport_height: payload.properties.height?.toString() || window.innerHeight.toString()
    };

    const utmData: UtmData = {};
    if (utm.medium) utmData.medium = utm.medium;
    if (utm.source) utmData.source = utm.source;
    if (utm.campaign) utmData.campaign = utm.campaign;
    if (utm.term) utmData.term = utm.term;
    if (utm.content) utmData.content = utm.content;

    return {
        ...payload,
        page: pageData,
        device: deviceData,
        local_time: local_time,
        ...(Object.keys(utmData).length > 0 && { utm: utmData })
    };
};

/**
 * @deprecated Use enrichPagePayloadOptimized instead to avoid data duplication
 * Legacy function that enriches page payload with all data in one call
 * This function duplicates data already available in analytics.js payload
 */
export const enrichPagePayload = (
    payload: DotAnalyticsPayload,
    location: Location = window.location
) => {
    const browserData = getBrowserEventData(location);
    const pageData = getPageData(browserData, payload);
    const deviceData = getDeviceData(browserData);
    const utmData = getUtmData(browserData);
    const local_time = getLocalTime();

    return {
        ...payload,
        page: pageData,
        device: deviceData,
        local_time: local_time,
        ...(Object.keys(utmData).length > 0 && { utm: utmData })
    };
};

/**
 * Throttled activity handler to prevent excessive calls
 */
const createThrottledActivityHandler = (config: DotContentAnalyticsConfig) => {
    return () => {
        if (isThrottled) return;

        isThrottled = true;
        updateActivityTime();

        if (config.debug) {
            console.warn('DotAnalytics: User activity detected');
        }

        // Throttle for 1 second
        setTimeout(() => {
            isThrottled = false;
        }, ACTIVITY_THROTTLE_MS);
    };
};
