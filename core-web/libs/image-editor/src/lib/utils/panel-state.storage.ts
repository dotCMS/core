/**
 * Persistence for the image-editor side-panel accordion's expanded sections.
 *
 * Mirrors the Edit Content sidebar pattern (`getStoredUIState` /
 * `saveStoreUIState` in `libs/edit-content/.../functions.util.ts`): a guarded
 * read that falls back to a sane default and a guarded write, both wrapped so a
 * blocked or corrupt storage entry can never break the editor. We use
 * `localStorage` (Edit Content uses `sessionStorage`) so the user's last layout
 * survives across browser sessions — they get the panels back the way they left
 * them, even after closing the tab.
 *
 * The value is the array of open `p-accordion-panel` values (e.g. `['adjust']`).
 * The default is an empty array: every section starts collapsed.
 */
export const IMAGE_EDITOR_PANEL_STATE_KEY = 'DOT_IMAGE_EDITOR_PANEL_STATE';

/** Every accordion section collapsed by default. */
const DEFAULT_PANEL_STATE: string[] = [];

/**
 * Reads the persisted set of open accordion sections, or returns the default
 * (all collapsed) when nothing is stored or the stored value is unusable.
 */
export const getStoredPanelState = (): string[] => {
    try {
        const stored = localStorage.getItem(IMAGE_EDITOR_PANEL_STATE_KEY);
        if (stored) {
            const parsed = JSON.parse(stored);
            if (Array.isArray(parsed) && parsed.every((value) => typeof value === 'string')) {
                return parsed;
            }
        }
    } catch (e) {
        console.warn('Error reading image editor panel state from localStorage:', e);
    }

    return [...DEFAULT_PANEL_STATE];
};

/** Persists the current set of open accordion sections. */
export const savePanelState = (state: string[]): void => {
    try {
        localStorage.setItem(IMAGE_EDITOR_PANEL_STATE_KEY, JSON.stringify(state));
    } catch (e) {
        console.warn('Error saving image editor panel state to localStorage:', e);
    }
};
