import { IMAGE_EDITOR_PANEL_STATE_KEY } from '../image-editor.constants';

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
 * The stored value (under {@link IMAGE_EDITOR_PANEL_STATE_KEY}) is the array of
 * open `p-accordion-panel` values (e.g. `['adjust']`); the default opens every
 * section so a first-time user sees all the controls at once.
 */

/** Every accordion section open by default (first use / no stored layout). */
const DEFAULT_PANEL_STATE: string[] = ['adjust', 'transform', 'fileinfo', 'history'];

/**
 * Reads the persisted set of open accordion sections, or returns the default
 * (all sections open) when nothing is stored or the stored value is unusable.
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
    } catch {
        // Reading the layout is non-critical UI persistence; on any storage error
        // (blocked/corrupt) fall through to the default rather than surface it.
    }

    return [...DEFAULT_PANEL_STATE];
};

/** Persists the current set of open accordion sections. */
export const savePanelState = (state: string[]): void => {
    try {
        localStorage.setItem(IMAGE_EDITOR_PANEL_STATE_KEY, JSON.stringify(state));
    } catch {
        // Persisting the layout is best-effort; ignore storage errors (quota/blocked).
    }
};
