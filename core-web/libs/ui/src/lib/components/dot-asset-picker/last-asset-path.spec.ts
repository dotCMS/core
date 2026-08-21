import {
    LAST_ASSET_PATH_KEY,
    readLastAssetLocation,
    writeLastAssetLocation
} from './last-asset-path';

const LOCATION = { siteId: 'site-1', hostname: 'demo.dotcms.com', path: '/images/' };

describe('last asset location', () => {
    beforeEach(() => window.localStorage.clear());

    describe('readLastAssetLocation', () => {
        it('should be undefined before anything is stored', () => {
            expect(readLastAssetLocation()).toBeUndefined();
        });

        it('should read back what was written', () => {
            writeLastAssetLocation(LOCATION);

            expect(readLastAssetLocation()).toEqual(LOCATION);
        });

        it('should fall back to undefined on an unparseable payload', () => {
            window.localStorage.setItem(LAST_ASSET_PATH_KEY, '{not json');

            expect(readLastAssetLocation()).toBeUndefined();
        });

        it('should not corrupt a path that starts with digits', () => {
            // Guards the reason we use readJson over DotLocalstorageService, whose getItem runs a
            // parseInt that would turn this into the number 2024.
            writeLastAssetLocation({ ...LOCATION, path: '/2024-campaign/' });

            expect(readLastAssetLocation()?.path).toBe('/2024-campaign/');
        });

        it('should read a legacy bare-path payload as a site-less path', () => {
            // Written before the picker went multi-site. The caller applies it to whatever site it
            // is opening on, which is what it used to mean.
            window.localStorage.setItem(LAST_ASSET_PATH_KEY, '"/images/"');

            expect(readLastAssetLocation()).toEqual({ siteId: '', hostname: '', path: '/images/' });
        });

        it('should ignore a stored location that lost its site', () => {
            // `/images/` on its own is ambiguous once every site is browsable.
            window.localStorage.setItem(LAST_ASSET_PATH_KEY, JSON.stringify({ path: '/images/' }));

            expect(readLastAssetLocation()).toBeUndefined();
        });
    });

    describe('writeLastAssetLocation', () => {
        it('should store under the single global key', () => {
            writeLastAssetLocation(LOCATION);

            expect(window.localStorage.getItem(LAST_ASSET_PATH_KEY)).toBe(JSON.stringify(LOCATION));
        });

        it('should overwrite the previous value under the same key', () => {
            // The "global, not per-field" contract: a second pick replaces the first rather than
            // being filed somewhere else.
            writeLastAssetLocation(LOCATION);
            writeLastAssetLocation({
                siteId: 'site-2',
                hostname: 'blog.dotcms.com',
                path: '/docs/'
            });

            expect(readLastAssetLocation()).toEqual({
                siteId: 'site-2',
                hostname: 'blog.dotcms.com',
                path: '/docs/'
            });
        });

        it('should keep a site with no path — that means the site root', () => {
            writeLastAssetLocation({ siteId: 'site-1', hostname: 'demo.dotcms.com' });

            expect(readLastAssetLocation()).toEqual({
                siteId: 'site-1',
                hostname: 'demo.dotcms.com'
            });
        });

        it('should clear the key when the location has no site', () => {
            writeLastAssetLocation(LOCATION);

            writeLastAssetLocation({ siteId: '', hostname: '', path: '/images/' });

            expect(window.localStorage.getItem(LAST_ASSET_PATH_KEY)).toBeNull();
            expect(readLastAssetLocation()).toBeUndefined();
        });

        it('should clear the key when given undefined', () => {
            writeLastAssetLocation(LOCATION);

            writeLastAssetLocation(undefined);

            expect(readLastAssetLocation()).toBeUndefined();
        });
    });
});
