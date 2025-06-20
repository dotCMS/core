import {
    ACTIVITY_EVENTS,
    DEFAULT_SESSION_TIMEOUT_MINUTES,
    SESSION_START_KEY,
    SESSION_STORAGE_KEY,
    SESSION_UTM_KEY,
    USER_ID_KEY
} from '../shared/dot-content-analytics.constants';
import {
    AnalyticsHookParams,
    DotContentAnalyticsConfig,
    DotAnalyticsContext
} from '../shared/dot-content-analytics.model';

// Activity tracking state
let activityListeners: (() => void)[] = [];
let lastActivityTime = Date.now();
let inactivityTimer: number | null = null;

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
 * Safe sessionStorage wrapper
 */
const safeSessionStorage = {
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
 * Generates a cryptographically secure random ID
 */
const generateSecureId = (prefix: string): string => {
    const timestamp = Date.now();
    const randomPart = Math.random().toString(36).substr(2, 9);
    const extraRandom = Math.random().toString(36).substr(2, 9);

    return `${prefix}_${timestamp}_${randomPart}${extraRandom}`;
};

/**
 * Gets or generates a persistent user ID
 */
const getUserId = (): string => {
    let userId = safeLocalStorage.getItem(USER_ID_KEY);

    if (!userId) {
        userId = generateSecureId('user');
        safeLocalStorage.setItem(USER_ID_KEY, userId);
    }

    return userId;
};

/**
 * Updates activity timestamp
 */
const updateActivityTime = (): void => {
    lastActivityTime = Date.now();

    if (inactivityTimer) {
        clearTimeout(inactivityTimer);
    }

    inactivityTimer = setTimeout(
        () => {
            // User became inactive
        },
        DEFAULT_SESSION_TIMEOUT_MINUTES * 60 * 1000
    ) as unknown as number;
};

/**
 * Checks if user has been inactive
 */
const isUserInactive = (): boolean => {
    const timeoutMs = DEFAULT_SESSION_TIMEOUT_MINUTES * 60 * 1000;

    return Date.now() - lastActivityTime > timeoutMs;
};

/**
 * Extracts UTM parameters from current location
 */
const extractUTMParameters = (): Record<string, string> => {
    const urlParams = new URLSearchParams(window.location.search);
    const utmParams: Record<string, string> = {};

    ['utm_source', 'utm_medium', 'utm_campaign', 'utm_id'].forEach((key) => {
        const value = urlParams.get(key);
        if (value !== null) {
            utmParams[key.replace('utm_', '')] = value;
        }
    });

    return utmParams;
};

/**
 * Compares UTM parameters to detect campaign changes
 */
const hasUTMChanged = (currentUTM: Record<string, string>): boolean => {
    try {
        const storedUTM = safeSessionStorage.getItem(SESSION_UTM_KEY);
        if (!storedUTM) {
            safeSessionStorage.setItem(SESSION_UTM_KEY, JSON.stringify(currentUTM));

            return false;
        }

        const previousUTM = JSON.parse(storedUTM);
        const significantParams = ['source', 'medium', 'campaign'];

        for (const param of significantParams) {
            if (currentUTM[param] !== previousUTM[param]) {
                safeSessionStorage.setItem(SESSION_UTM_KEY, JSON.stringify(currentUTM));

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
 * Generates session ID with comprehensive lifecycle management
 */
const generateSessionId = (): string => {
    const now = Date.now();
    const currentUTM = extractUTMParameters();

    try {
        const existingSessionId = safeSessionStorage.getItem(SESSION_STORAGE_KEY);
        const sessionStart = safeSessionStorage.getItem(SESSION_START_KEY);

        if (existingSessionId && sessionStart) {
            const sessionStartTime = parseInt(sessionStart, 10);

            const isActive = !isUserInactive();
            const hasNotPassedMidnight = !hasPassedMidnight(sessionStartTime);
            const hasNotChangedUTM = !hasUTMChanged(currentUTM);

            if (isActive && hasNotPassedMidnight && hasNotChangedUTM) {
                updateActivityTime();

                return existingSessionId;
            }
        }

        // Generate new session
        const newSessionId = generateSecureId('session');
        safeSessionStorage.setItem(SESSION_STORAGE_KEY, newSessionId);
        safeSessionStorage.setItem(SESSION_START_KEY, now.toString());
        safeSessionStorage.setItem(SESSION_UTM_KEY, JSON.stringify(currentUTM));
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
 * Gets analytics context with user and session identification
 */
const getIdentityContext = (config: DotContentAnalyticsConfig): DotAnalyticsContext => {
    const sessionId = generateSessionId();
    const userId = getUserId();

    if (config.debug) {
        console.warn('DotAnalytics Identity Context:', {
            sessionId,
            userId,
            lastActivity: lastActivityTime
        });
    }

    return {
        site_key: config.siteKey,
        session_id: sessionId,
        user_id: userId
    };
};

/**
 * Initializes activity tracking
 */
const initializeActivityTracking = (config: DotContentAnalyticsConfig): void => {
    // Clean up existing listeners
    activityListeners.forEach((cleanup) => cleanup());
    activityListeners = [];

    const handleActivity = () => {
        updateActivityTime();

        if (config.debug) {
            console.warn('DotAnalytics: User activity detected');
        }
    };

    // Set up event listeners
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
const cleanupActivityTracking = (): void => {
    activityListeners.forEach((cleanup) => cleanup());
    activityListeners = [];

    if (inactivityTimer) {
        clearTimeout(inactivityTimer);
        inactivityTimer = null;
    }
};

/**
 * Identity Plugin for DotAnalytics
 * Handles user ID generation, session management, and activity tracking.
 * This plugin provides consistent identity context across all analytics events.
 */
export const dotAnalyticsIdentityPlugin = (config: DotContentAnalyticsConfig) => {
    return {
        name: 'dot-analytics-identity',

        /**
         * Initialize the identity plugin
         */
        initialize: () => {
            initializeActivityTracking(config);

            if (config.debug) {
                console.warn('DotAnalytics: Identity plugin initialized');
            }

            return Promise.resolve();
        },

        /**
         * Inject identity context into page events
         * This runs BEFORE the enricher plugin
         */
        pageStart: ({ payload }: AnalyticsHookParams) => {
            const context = getIdentityContext(config);

            return {
                ...payload,
                context
            };
        },

        /**
         * Inject identity context into track events
         * This runs BEFORE the enricher plugin
         */
        trackStart: ({ payload }: AnalyticsHookParams) => {
            const context = getIdentityContext(config);

            return {
                ...payload,
                context
            };
        },

        /**
         * Clean up on plugin unload
         */
        loaded: () => {
            // Set up cleanup on page unload
            if (typeof window !== 'undefined') {
                window.addEventListener('beforeunload', cleanupActivityTracking);
            }

            return true;
        }
    };
};
