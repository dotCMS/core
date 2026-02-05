import { AnalyticsPlugin, PageData } from 'analytics';

import {
    ANALYTICS_JS_DEFAULT_PROPERTIES,
    ANALYTICS_MINIFIED_SCRIPT_NAME,
    CONTENTLET_CLASS,
    DEFAULT_IMPRESSION_MUTATION_OBSERVER_DEBOUNCE_MS,
    DEFAULT_SESSION_TIMEOUT_MINUTES,
    DotCMSPredefinedEventType,
    EXPECTED_UTM_KEYS,
    SESSION_STORAGE_KEY,
    USER_ID_KEY
} from '../constants';
import { DotLogger, LogLevel } from '../dot-analytics.logger';
import {
    AnalyticsBasePayloadWithContext,
    ContentletData,
    DotCMSAnalyticsConfig,
    DotCMSAnalyticsEventContext,
    DotCMSBrowserData,
    DotCMSEventDeviceData,
    DotCMSEventPageData,
    DotCMSEventUtmData,
    EnrichedAnalyticsPayload,
    ImpressionConfig,
    JsonObject,
    JsonValue,
    QueueConfig
} from '../models';

// Export activity tracking functions from identity plugin
export {
    cleanupActivityTracking,
    initializeActivityTracking,
    updateSessionActivity
} from '../../plugin/identity/dot-analytics.identity.activity-tracker';

/**
 * Type guard to check if an event is a predefined event type.
 * Enables TypeScript type narrowing for better type safety.
 *
 * @param event - Event name to check
 * @returns True if event is a predefined type, false for custom events
 *
 * @example
 * ```typescript
 * if (isPredefinedEventType(eventName)) {
 *   // TypeScript knows eventName is DotCMSPredefinedEventType here
 *   console.log('Predefined event:', eventName);
 * } else {
 *   // TypeScript knows eventName is string (custom) here
 *   console.log('Custom event:', eventName);
 * }
 * ```
 */
export function isPredefinedEventType(event: string): event is DotCMSPredefinedEventType {
    return Object.values(DotCMSPredefinedEventType).includes(event as DotCMSPredefinedEventType);
}

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
        } catch (e) {
            console.warn(`DotCMS Analytics [Core]: Could not save ${key} to localStorage`);
            throw e;
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
        } catch (e) {
            console.warn(`DotCMS Analytics [Core]: Could not save ${key} to sessionStorage`);
            throw e;
        }
    },
    removeItem: (key: string): void => {
        try {
            sessionStorage.removeItem(key);
        } catch {
            console.warn(`DotCMS Analytics [Core]: Could not remove ${key} from sessionStorage`);
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
        try {
            safeLocalStorage.setItem(USER_ID_KEY, userId);
        } catch {
            // Ignore storage errors for user ID (ephemeral user)
        }
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
        // Parse advanced configuration from data-analytics-config (JSON)
        const advancedConfigAttr = script.getAttribute('data-analytics-config');
        let advancedConfig: Partial<DotCMSAnalyticsConfig> = {};

        if (advancedConfigAttr) {
            try {
                // 1. Try native JSON parse first (strict and correct)
                const parsedConfig = JSON.parse(advancedConfigAttr);
                advancedConfig = sanitizeAdvancedConfig(parsedConfig);
            } catch {
                try {
                    // 2. Targeted fallback for VTL-style single-quoted JSON {'key': 'value'}
                    // Only replace quotes around keys and values, preserving apostrophes in content
                    const jsonStr = advancedConfigAttr
                        .replace(/([{,]\s*)'(.+?)'(\s*:)/g, '$1"$2"$3') // Keys
                        .replace(/(:\s*)'(.+?)'(\s*[,}\]])/g, '$1"$2"$3'); // Values (handles objects and arrays)

                    const parsedConfig = JSON.parse(jsonStr);
                    advancedConfig = sanitizeAdvancedConfig(parsedConfig);
                } catch (e) {
                    // eslint-disable-next-line no-console
                    console.error('Failed to parse data-analytics-config JSON:', e);
                }
            }
        }

        const serverAttr = script.getAttribute('data-analytics-server');
        const debugAttr = script.getAttribute('data-analytics-debug');
        const autoPageViewAttr = script.getAttribute('data-analytics-auto-page-view');
        const siteAuthAttr = script.getAttribute('data-analytics-auth');
        const impressionsAttr = script.getAttribute('data-analytics-impressions');
        const clicksAttr = script.getAttribute('data-analytics-clicks');

        return {
            // 1. Default fallback values
            server: window.location.origin,
            debug: false,
            autoPageView: false,
            siteAuth: '',

            // 2. Advanced config (JSON)
            ...advancedConfig,

            // 3. Explicit attributes (highest priority)
            ...(serverAttr && { server: serverAttr }),
            ...(debugAttr && { debug: debugAttr === 'true' }),
            ...(autoPageViewAttr && { autoPageView: autoPageViewAttr === 'true' }),
            ...(siteAuthAttr && { siteAuth: siteAuthAttr }),
            ...(impressionsAttr && { impressions: impressionsAttr === 'true' }),
            ...(clicksAttr && { clicks: clicksAttr === 'true' })
        };
    }

    // No script found, return defaults with current domain as server
    // eslint-disable-next-line no-console
    console.warn('[DotCMS Analytics] Script wrapper not found - verify installation');
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
            userProvidedProperties[key] = properties[key as keyof typeof properties] as JsonValue;
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

/**
 * Creates a throttled version of a callback function
 * Ensures the callback is executed at most once every `limitMs` milliseconds
 * @param callback - The function to throttle
 * @param limitMs - The time limit in milliseconds
 * @returns A throttled function
 */
export function createThrottle<T extends (...args: unknown[]) => void>(
    callback: T,
    limitMs: number
): (...args: Parameters<T>) => void {
    let lastRun = 0;

    return (...args: Parameters<T>) => {
        const now = Date.now();
        if (now - lastRun >= limitMs) {
            callback(...args);
            lastRun = now;
        }
    };
}

/**
 * Extracts the contentlet identifier from a DOM element
 * @param element - The HTML element containing data attributes
 * @returns The contentlet identifier or null if not found
 */
export function extractContentletIdentifier(element: HTMLElement): string | null {
    return element.dataset.dotIdentifier || null;
}

/**
 * Extracts all contentlet data from a DOM element's data attributes
 * @param element - The HTML element containing data attributes
 * @returns Complete contentlet data object
 */
export function extractContentletData(element: HTMLElement): ContentletData {
    return {
        identifier: element.dataset.dotIdentifier || '',
        inode: element.dataset.dotInode || '',
        contentType: element.dataset.dotType || '',
        title: element.dataset.dotTitle || '',
        baseType: element.dataset.dotBasetype || ''
    };
}

/**
 * Initial scan delay for DOM readiness
 * Allows React/Next.js to finish rendering before scanning for contentlets
 */
export const INITIAL_SCAN_DELAY_MS = 100;

/**
 * Checks if code is running in a browser environment
 * @returns true if window and document are available
 */
export const isBrowser = (): boolean => {
    return typeof window !== 'undefined' && typeof document !== 'undefined';
};

/**
 * Finds all contentlet elements in the DOM
 * @returns Array of contentlet HTMLElements
 */
export const findContentlets = (): HTMLElement[] => {
    return Array.from(document.querySelectorAll<HTMLElement>(`.${CONTENTLET_CLASS}`));
};

/**
 * Creates a MutationObserver that watches for contentlet changes in the DOM
 * @param callback - Function to call when mutations are detected
 * @param debounceMs - Debounce time in milliseconds (default: 250ms)
 * @returns Configured and active MutationObserver
 */
export const createContentletObserver = (
    callback: () => void,
    debounceMs: number = DEFAULT_IMPRESSION_MUTATION_OBSERVER_DEBOUNCE_MS
): MutationObserver => {
    const throttledCallback = createThrottle(callback, debounceMs);

    const observer = new MutationObserver((mutations) => {
        // This reduces observer callback executions by ~90% on dynamic sites
        const hasRelevantChanges = mutations.some((mutation) => {
            // Skip if no nodes were added or removed
            if (mutation.addedNodes.length === 0 && mutation.removedNodes.length === 0) {
                return false;
            }

            // Check if any added/removed nodes are or contain contentlets
            const nodes = [
                ...Array.from(mutation.addedNodes),
                ...Array.from(mutation.removedNodes)
            ];

            return nodes.some((node) => {
                // Only check element nodes
                if (node.nodeType !== Node.ELEMENT_NODE) {
                    return false;
                }

                const element = node as HTMLElement;

                // Check if node itself is a contentlet
                if (element.classList?.contains(CONTENTLET_CLASS)) {
                    return true;
                }

                // Check if node contains contentlets
                return element.querySelector?.(`.${CONTENTLET_CLASS}`) !== null;
            });
        });

        // Only invoke callback if relevant changes detected
        if (hasRelevantChanges) {
            throttledCallback();
        }
    });

    // Handle case where document.body might not be available yet if script is in <head>
    const target = document.body || document.documentElement;
    if (target) {
        observer.observe(target, {
            childList: true,
            subtree: true,
            attributes: false,
            characterData: false
        });
    } else {
        // Fallback: wait for DOMContentLoaded if everything is null (rare)
        window.addEventListener('DOMContentLoaded', () => {
            if (document.body) {
                observer.observe(document.body, {
                    childList: true,
                    subtree: true,
                    attributes: false,
                    characterData: false
                });
            }
        });
    }

    return observer;
};

/**
 * Sets up cleanup handlers for page unload events
 * Registers cleanup function to both 'beforeunload' and 'pagehide' for maximum compatibility
 * @param cleanup - Function to call on page unload
 */
export const setupPluginCleanup = (cleanup: () => void): void => {
    if (!isBrowser()) return;

    window.addEventListener('beforeunload', cleanup);
    window.addEventListener('pagehide', cleanup);
};

/**
 * Creates a logger with plugin-specific prefix and configurable log level
 * @param pluginName - Name of the plugin (e.g., 'Click', 'Impression')
 * @param config - Analytics configuration with debug flag and optional logLevel
 * @returns DotLogger instance with configured log level
 */
export const createPluginLogger = (
    pluginName: string,
    config: { debug: boolean; logLevel?: LogLevel }
): DotLogger => {
    // Use explicit logLevel if provided, otherwise fall back to debug flag
    const level = config.logLevel ?? (config.debug ? 'debug' : 'warn');
    return new DotLogger('Analytics', pluginName, level);
};

/**
 * Gets enhanced tracking plugins based on configuration
 * Returns content impression and click tracking plugins if enabled
 * @param config - Analytics configuration
 * @param impressionPlugin - Impression tracking plugin factory
 * @param clickPlugin - Click tracking plugin factory
 * @returns Array of enabled tracking plugins
 */
export const getEnhancedTrackingPlugins = (
    config: DotCMSAnalyticsConfig,
    impressionPlugin: (config: DotCMSAnalyticsConfig) => AnalyticsPlugin,
    clickPlugin: (config: DotCMSAnalyticsConfig) => AnalyticsPlugin
): AnalyticsPlugin[] => {
    return [
        config.impressions && impressionPlugin(config),
        config.clicks && clickPlugin(config)
    ].filter(Boolean) as AnalyticsPlugin[];
};

/**
 * Sanitizes the advanced configuration object to ensure only valid keys and types are allowed.
 * Prevents pollution from unknown properties.
 * @param config - The parsed JSON configuration object
 * @returns A sanitized Partial<DotCMSAnalyticsConfig>
 */
function sanitizeAdvancedConfig(config: unknown): Partial<DotCMSAnalyticsConfig> {
    if (typeof config !== 'object' || config === null) {
        return {};
    }

    const cleanConfig: Partial<DotCMSAnalyticsConfig> = {};
    const c = config as Partial<DotCMSAnalyticsConfig>;

    // Core validation
    if (typeof c.server === 'string' && c.server.trim().length > 0) {
        cleanConfig.server = c.server.trim();
    }
    if (typeof c.siteAuth === 'string' && c.siteAuth.trim().length > 0) {
        cleanConfig.siteAuth = c.siteAuth.trim();
    }

    // Queue validation
    if (c.queue !== undefined) {
        if (typeof c.queue === 'boolean') {
            cleanConfig.queue = c.queue;
        } else if (typeof c.queue === 'object' && c.queue !== null) {
            const cleanQueue: QueueConfig = {};
            if (typeof c.queue.eventBatchSize === 'number') {
                cleanQueue.eventBatchSize = c.queue.eventBatchSize;
            }
            if (typeof c.queue.flushInterval === 'number') {
                cleanQueue.flushInterval = c.queue.flushInterval;
            }
            // Only add if we found valid properties
            if (Object.keys(cleanQueue).length > 0) {
                cleanConfig.queue = cleanQueue;
            }
        }
    }

    // Impressions validation
    if (c.impressions !== undefined) {
        if (typeof c.impressions === 'boolean') {
            cleanConfig.impressions = c.impressions;
        } else if (typeof c.impressions === 'object' && c.impressions !== null) {
            const cleanImpressions: ImpressionConfig = {};
            if (typeof c.impressions.visibilityThreshold === 'number') {
                cleanImpressions.visibilityThreshold = c.impressions.visibilityThreshold;
            }
            if (typeof c.impressions.dwellMs === 'number') {
                cleanImpressions.dwellMs = c.impressions.dwellMs;
            }
            if (typeof c.impressions.maxNodes === 'number') {
                cleanImpressions.maxNodes = c.impressions.maxNodes;
            }
            if (typeof c.impressions.throttleMs === 'number') {
                cleanImpressions.throttleMs = c.impressions.throttleMs;
            }
            if (Object.keys(cleanImpressions).length > 0) {
                cleanConfig.impressions = cleanImpressions;
            }
        }
    }

    // Simple booleans and other scalar types
    if (typeof c.clicks === 'boolean') {
        cleanConfig.clicks = c.clicks;
    }
    if (typeof c.autoPageView === 'boolean') {
        cleanConfig.autoPageView = c.autoPageView;
    }
    if (typeof c.debug === 'boolean') {
        cleanConfig.debug = c.debug;
    }
    // LogLevel validation
    const allowedLogLevels = ['debug', 'info', 'warn', 'error'];
    if (typeof c.logLevel === 'string' && allowedLogLevels.includes(c.logLevel)) {
        cleanConfig.logLevel = c.logLevel as LogLevel;
    }

    return cleanConfig;
}
