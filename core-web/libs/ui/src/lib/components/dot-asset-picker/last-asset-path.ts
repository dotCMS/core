import { readJson, removeKey, writeJson } from '@dotcms/data-access';

/**
 * The folder the AssetPicker reopens on.
 *
 * Deliberately **one key for the whole system**, not one per field: picking an image in one field
 * and then opening a different field should land you where you just were (epic #36702).
 */
export const LAST_ASSET_PATH_KEY = 'dotcms.asset-picker.lastPath';

/** Remembered folder, or `undefined` when nothing has been picked yet. */
export const readLastAssetPath = (): string | undefined =>
    readJson<string | null>(LAST_ASSET_PATH_KEY, null) ?? undefined;

/**
 * Remembers the folder an asset was picked from. Writing an empty path clears the key rather than
 * storing `""`, so the next open falls back to the site root instead of a meaningless value.
 */
export const writeLastAssetPath = (path: string | undefined): void => {
    if (!path) {
        removeKey(LAST_ASSET_PATH_KEY);

        return;
    }

    writeJson(LAST_ASSET_PATH_KEY, path);
};
