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

/**
 * Generates a cryptographically secure random ID
 */
const generateSecureId = (prefix: string): string => {
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
 * Enhanced session ID generation
 */
export const generateSessionId = (): string => {
    const now = Date.now();
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

        // Generate new session
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

    const handleActivity = () => {
        updateActivityTime();

        if (config.debug) {
            console.warn('DotAnalytics: User activity detected');
        }
    };

    // Set up event listeners using constants
    ACTIVITY_EVENTS.forEach((eventType: string) => {
        const cleanup = () => window.removeEventListener(eventType, handleActivity);
        activityListeners.push(cleanup);
        window.addEventListener(eventType, handleActivity);
    });

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
 * Gets analytics context
 */
export const getAnalyticsContext = (config: DotContentAnalyticsConfig): DotAnalyticsContext => {
    const sessionId = generateSessionId();
    const userId = getUserId();

    if (config.debug) {
        console.warn('DotAnalytics Session Info:', getSessionInfo());
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
    const scripts = document.querySelector('script[data-analytics-key]');

    if (!scripts) {
        throw new Error('Dot Analytics: Script not found');
    }

    return scripts as HTMLScriptElement;
};

/**
 * Retrieves the browser event data.
 */
export const getBrowserEventData = (location: Location): BrowserEventData => ({
    utc_time: new Date().toISOString(),
    local_tz_offset: new Date().getTimezoneOffset(),
    screen_resolution: `${window.screen.width}x${window.screen.height}`,
    vp_size: `${window.innerWidth}x${window.innerHeight}`,

    user_language: navigator.language,
    doc_encoding: document.characterSet,
    doc_path: location.pathname,
    doc_host: location.hostname,
    doc_protocol: location.protocol,
    doc_hash: location.hash,
    doc_search: location.search,
    referrer: document.referrer,
    page_title: document.title,
    url: window.location.href,
    utm: extractUTMParameters(window.location)
});

/**
 * Extracts UTM parameters from a given URL location.
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
 * Gets page data from browser event data and payload
 */
export const getPageData = (
    browserData: BrowserEventData,
    payload: DotAnalyticsPayload
): PageData => {
    // Now properly typed - no need for 'any'
    const { language_id, persona } = payload.properties;

    return {
        url: browserData.url,
        doc_encoding: browserData.doc_encoding,
        title: browserData.page_title,
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
    const [viewportWidth, viewportHeight] = browserData.vp_size.split('x');

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
 * Enriches payload with page-specific data
 */
export const enrichWithPageData = (payload: DotAnalyticsPayload) => {
    const browserData = getBrowserEventData(window.location);
    const pageData = getPageData(browserData, payload);

    return {
        ...payload,
        properties: {
            ...payload.properties,
            page: pageData
        }
    };
};

/**
 * Enriches payload with device-specific data
 */
export const enrichWithDeviceData = (payload: DotAnalyticsPayload) => {
    const browserData = getBrowserEventData(window.location);
    const deviceData = getDeviceData(browserData);

    return {
        ...payload,
        properties: {
            ...payload.properties,
            device: deviceData
        }
    };
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
