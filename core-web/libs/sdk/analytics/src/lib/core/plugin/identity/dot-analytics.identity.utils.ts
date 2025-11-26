import { DEFAULT_SESSION_TIMEOUT_MINUTES, SESSION_UTM_KEY } from '../../shared/constants';
import { safeSessionStorage } from '../../shared/dot-content-analytics.utils';
import { DotCMSEventUtmData } from '../../shared/models';

// Activity tracking state
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
 * Gets the last activity time
 */
export const getLastActivityTime = (): number => {
    return lastActivityTime;
};

/**
 * Compares UTM parameters to detect campaign changes.
 * Only checks significant parameters: source, medium, and campaign.
 * @internal This function is for internal use only.
 * @param currentUTM - Current UTM parameters in DotCMS format
 * @returns True if UTM parameters have changed, false otherwise
 */
export const hasUTMChanged = (currentUTM: DotCMSEventUtmData): boolean => {
    try {
        const storedUTM = safeSessionStorage.getItem(SESSION_UTM_KEY);
        if (!storedUTM) {
            safeSessionStorage.setItem(SESSION_UTM_KEY, JSON.stringify(currentUTM));

            return false;
        }

        const previousUTM = JSON.parse(storedUTM);
        const significantParams: (keyof DotCMSEventUtmData)[] = ['source', 'medium', 'campaign'];

        for (const param of significantParams) {
            if (currentUTM[param] !== previousUTM[param]) {
                safeSessionStorage.setItem(SESSION_UTM_KEY, JSON.stringify(currentUTM));

                return true;
            }
        }

        return false;
    } catch {
        return false;
    }
};
