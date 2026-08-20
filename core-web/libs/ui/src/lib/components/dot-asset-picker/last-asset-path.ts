import { readJson, removeKey, writeJson } from '@dotcms/data-access';

/**
 * The folder the AssetPicker reopens on.
 *
 * Deliberately **one key for the whole system**, not one per field: picking an image in one field
 * and then opening a different field should land you where you just were (epic #36702).
 */
export const LAST_ASSET_PATH_KEY = 'dotcms.asset-picker.lastPath';

/**
 * Where the picker last left off.
 *
 * The site is part of it, not just the path: the picker browses every site, so `/images/` on its own
 * is ambiguous and would send the next open to a folder of the same name on a different site — or to
 * one that does not exist there at all.
 */
export interface DotAssetPickerLocation {
    /** Site identifier — needed to scope the folder search that expands the branch. */
    siteId: string;
    hostname: string;
    /** Folder path within the site. Absent means the site root. */
    path?: string;
}

/** Payload written before the picker went multi-site: a bare folder path. */
type StoredLocation = DotAssetPickerLocation | string;

/**
 * Remembered location, or `undefined` when nothing has been picked yet.
 *
 * A legacy bare-path payload is read as "this path, site unknown" rather than discarded — the caller
 * then applies it to whichever site it is opening on, which is what it used to mean.
 */
export const readLastAssetLocation = (): DotAssetPickerLocation | undefined => {
    const stored = readJson<StoredLocation | null>(LAST_ASSET_PATH_KEY, null);

    if (!stored) {
        return undefined;
    }

    if (typeof stored === 'string') {
        return { siteId: '', hostname: '', path: stored };
    }

    return stored.siteId && stored.hostname ? stored : undefined;
};

/**
 * Remembers where an asset was picked from. Writing a location with no site clears the key rather
 * than storing something the next open could not act on.
 */
export const writeLastAssetLocation = (location: DotAssetPickerLocation | undefined): void => {
    if (!location?.siteId || !location.hostname) {
        removeKey(LAST_ASSET_PATH_KEY);

        return;
    }

    writeJson(LAST_ASSET_PATH_KEY, location);
};
