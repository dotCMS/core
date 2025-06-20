import {
    ACTIVITY_EVENTS,
    DEFAULT_SESSION_TIMEOUT_MINUTES,
    EXPECTED_UTM_KEYS,
    SESSION_START_KEY,
    SESSION_STORAGE_KEY,
    SESSION_UTM_KEY,
    USER_ID_KEY
} from '../../shared/dot-content-analytics.constants';
import { DotContentAnalyticsConfig } from '../../shared/dot-content-analytics.model';

// Activity tracking state
let activityListeners: (() => void)[] = [];
let lastActivityTime = Date.now();
let inactivityTimer: number | null = null;

/**
 * Updates activity timestamp
 */
export const updateActivityTime = (): void => {
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
export const isUserInactive = (): boolean => {
    const timeoutMs = DEFAULT_SESSION_TIMEOUT_MINUTES * 60 * 1000;

    return Date.now() - lastActivityTime > timeoutMs;
};

/**
 * Checks if a new day has started since session creation
 */
export const hasPassedMidnight = (sessionStartTime: number): boolean => {
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
export const generateSessionId = (): string => {
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
 * Initializes activity tracking
 */
export const initializeActivityTracking = (config: DotContentAnalyticsConfig): void => {
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
export const cleanupActivityTracking = (): void => {
    activityListeners.forEach((cleanup) => cleanup());
    activityListeners = [];

    if (inactivityTimer) {
        clearTimeout(inactivityTimer);
        inactivityTimer = null;
    }
};

/**
 * Gets the last activity time
 */
export const getLastActivityTime = (): number => {
    return lastActivityTime;
};

/**
 * Safe localStorage wrapper with error handling
 */
export const safeLocalStorage = {
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
 * Generates a cryptographically secure random ID using Web Crypto API
 * Format: prefix_timestamp_uuid
 * This ensures zero collision probability and proper entropy
 */
export const generateSecureId = (prefix: string): string => {
    const timestamp = Date.now().toString(36); // Base36 for shorter string

    try {
        // Use Web Crypto API for cryptographically secure random values
        const array = new Uint8Array(16); // 128 bits of entropy
        crypto.getRandomValues(array);

        // Convert to hexadecimal string
        const randomHex = Array.from(array)
            .map((b) => b.toString(16).padStart(2, '0'))
            .join('');

        return `${prefix}_${timestamp}_${randomHex}`;
    } catch (error) {
        // Fallback for environments without crypto API
        console.warn('DotAnalytics: Web Crypto API not available, using fallback ID generation');

        // Use performance.now() + Date.now() + multiple random sources for better entropy
        const highResTime = (performance?.now() || 0).toString(36);
        const random1 = Math.random().toString(36).substring(2);
        const random2 = Math.random().toString(36).substring(2);
        const random3 = Math.random().toString(36).substring(2);

        return `${prefix}_${timestamp}_${highResTime}_${random1}${random2}${random3}`;
    }
};

/**
 * Gets or generates a persistent user ID
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
 * Extracts UTM parameters from current location
 */
export const extractUTMParameters = (): Record<string, string> => {
    const urlParams = new URLSearchParams(window.location.search);
    const utmParams: Record<string, string> = {};

    EXPECTED_UTM_KEYS.forEach((key) => {
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
export const hasUTMChanged = (currentUTM: Record<string, string>): boolean => {
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
