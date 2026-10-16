import { DEFAULT_VIEW_TYPE, VIEW_TYPE_STORAGE_KEY_PREFIX } from '../shared/constants';
import { DOT_CONTENT_DRIVE_VIEW_TYPE, DotContentDriveViewType } from '../shared/models';

/** Used when the screen hosting the Drive cannot be told apart. */
const FALLBACK_PORTLET_ID = 'content-drive';

/**
 * Reads a view from any of its sources: the URL, browser storage or a custom tool's
 * `dataViewMode`. Letter case and surrounding spaces are ignored.
 *
 * @param value Whatever the source holds.
 * @returns `list` or `card`, or `null` when the value is not one of them.
 */
export function parseViewType(value: unknown): DotContentDriveViewType | null {
    if (typeof value !== 'string') {
        return null;
    }

    const normalized = value.trim().toLowerCase();

    return normalized === DOT_CONTENT_DRIVE_VIEW_TYPE.LIST ||
        normalized === DOT_CONTENT_DRIVE_VIEW_TYPE.CARD
        ? normalized
        : null;
}

/**
 * Decides which view the Drive opens in (#37930, FR-002a). The first value that reads as a view
 * wins, in this order:
 *
 * 1. `viewType` in the URL — an explicit choice, such as a link someone shared;
 * 2. the view the user last picked on this screen, remembered in the browser;
 * 3. the custom tool's default view (`defaultViewType`, #37931);
 * 4. the table.
 *
 * @param sources The raw value of each source; any of them may be missing or unreadable.
 */
export function resolveViewType(sources: {
    urlViewType: unknown;
    storedViewType: unknown;
    defaultViewType: unknown;
}): DotContentDriveViewType {
    return (
        parseViewType(sources.urlViewType) ??
        parseViewType(sources.storedViewType) ??
        parseViewType(sources.defaultViewType) ??
        DEFAULT_VIEW_TYPE
    );
}

/**
 * Storage key for the view remembered on one screen. Each screen that hosts the Drive — the Drive
 * itself, and each custom tool once #37931 lands — keeps its own.
 *
 * @param portletId The id of the screen's portlet (the first segment of its URL).
 */
export function viewTypeStorageKey(portletId: string): string {
    return `${VIEW_TYPE_STORAGE_KEY_PREFIX}${portletId || FALLBACK_PORTLET_ID}`;
}

/**
 * The browser's local storage, or `null` where even reaching it throws (some privacy settings).
 */
export function browserStorage(): Storage | null {
    try {
        return globalThis.localStorage ?? null;
    } catch {
        return null;
    }
}

/**
 * Reads the remembered view. Storage that is missing, blocked or holds something else reads as
 * nothing remembered.
 *
 * @param storage Where the view is kept.
 * @param key The screen's key, from {@link viewTypeStorageKey}.
 */
export function readStoredViewType(
    storage: Pick<Storage, 'getItem'> | null | undefined,
    key: string
): DotContentDriveViewType | null {
    try {
        return parseViewType(storage?.getItem(key));
    } catch {
        return null;
    }
}

/**
 * Remembers a view. A browser that refuses (private window, blocked or full storage) is not an
 * error: the view still changes for this visit, it is just not remembered (FR-024).
 *
 * @param storage Where the view is kept.
 * @param key The screen's key, from {@link viewTypeStorageKey}.
 * @param viewType The view to remember.
 */
export function writeStoredViewType(
    storage: Pick<Storage, 'setItem'> | null | undefined,
    key: string,
    viewType: DotContentDriveViewType
): void {
    try {
        storage?.setItem(key, viewType);
    } catch {
        // Nothing to do: the switch already happened, and there is nothing the user can act on.
    }
}
