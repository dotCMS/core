/**
 * Shared localStorage helpers for the dev-tool portlets
 * (`dot-query-tool`, `dot-es-search`, `dot-velocity-playground`).
 *
 * These are pure functions rather than a service so they can be called from
 * SignalStore features (`withHooks` / `effect`) without depending on Angular DI
 * at the storage layer. They also guard against SSR (`typeof window`) and
 * silently swallow `localStorage` errors (quota, private mode, disabled
 * storage) — persistence is best-effort UX polish, never a hard requirement.
 */

/** Prefix for every persisted-query storage key. */
export const PERSISTED_QUERY_KEY_PREFIX = 'dotcms.devtools';

/** Build the full localStorage key for a given portlet's last query. */
export const buildPersistedQueryKey = (portletKey: string): string =>
    `${PERSISTED_QUERY_KEY_PREFIX}.${portletKey}.lastQuery`;

/**
 * Read a JSON value from localStorage with a typed fallback. Returns the
 * fallback in non-browser environments, when the key is missing, or when the
 * payload can't be parsed.
 */
export const readJson = <T>(key: string, fallback: T): T => {
    if (typeof window === 'undefined') return fallback;
    try {
        const raw = window.localStorage.getItem(key);
        if (raw == null) return fallback;

        return JSON.parse(raw) as T;
    } catch {
        return fallback;
    }
};

/** Persist a JSON-serializable value to localStorage; silently noop on quota / private mode. */
export const writeJson = (key: string, value: unknown): void => {
    if (typeof window === 'undefined') return;
    try {
        window.localStorage.setItem(key, JSON.stringify(value));
    } catch {
        // Storage may be unavailable (quota, private mode) — ignore
    }
};

/** Remove a localStorage entry; silently noop when storage is unavailable. */
export const removeKey = (key: string): void => {
    if (typeof window === 'undefined') return;
    try {
        window.localStorage.removeItem(key);
    } catch {
        // ignore
    }
};
