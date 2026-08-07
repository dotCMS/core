import { LAST_ASSET_PATH_KEY, readLastAssetPath, writeLastAssetPath } from './last-asset-path';

describe('last asset path', () => {
    beforeEach(() => window.localStorage.clear());

    describe('readLastAssetPath', () => {
        it('should be undefined before anything is stored', () => {
            expect(readLastAssetPath()).toBeUndefined();
        });

        it('should read back what was written', () => {
            writeLastAssetPath('/images/');

            expect(readLastAssetPath()).toBe('/images/');
        });

        it('should fall back to undefined on an unparseable payload', () => {
            window.localStorage.setItem(LAST_ASSET_PATH_KEY, '{not json');

            expect(readLastAssetPath()).toBeUndefined();
        });

        it('should not corrupt a path that starts with digits', () => {
            // Guards the reason we use readJson over DotLocalstorageService, whose getItem runs a
            // parseInt that would turn this into the number 2024.
            writeLastAssetPath('/2024-campaign/');

            expect(readLastAssetPath()).toBe('/2024-campaign/');
        });
    });

    describe('writeLastAssetPath', () => {
        it('should store under the single global key', () => {
            writeLastAssetPath('/images/');

            expect(window.localStorage.getItem(LAST_ASSET_PATH_KEY)).toBe('"/images/"');
        });

        it('should overwrite the previous value under the same key', () => {
            // The "global, not per-field" contract: a second pick replaces the first rather than
            // being filed somewhere else.
            writeLastAssetPath('/images/');
            writeLastAssetPath('/docs/');

            expect(readLastAssetPath()).toBe('/docs/');
            expect(window.localStorage.getItem(LAST_ASSET_PATH_KEY)).toBe('"/docs/"');
        });

        it('should clear the key when given an empty path', () => {
            writeLastAssetPath('/images/');

            writeLastAssetPath('');

            expect(window.localStorage.getItem(LAST_ASSET_PATH_KEY)).toBeNull();
            expect(readLastAssetPath()).toBeUndefined();
        });

        it('should clear the key when given undefined', () => {
            writeLastAssetPath('/images/');

            writeLastAssetPath(undefined);

            expect(readLastAssetPath()).toBeUndefined();
        });
    });
});
