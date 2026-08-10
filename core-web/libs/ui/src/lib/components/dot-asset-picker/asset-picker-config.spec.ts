import { DotSite } from '@dotcms/dotcms-models';

import { buildAssetPickerConfig } from './asset-picker-config';
import { LAST_ASSET_PATH_KEY, writeLastAssetLocation } from './last-asset-path';

const SITE: DotSite = {
    identifier: 'site-1',
    hostname: 'dotcms.com',
    aliases: null,
    archived: false
};

/** Somewhere other than `SITE`, to prove the remembered site travels with the remembered path. */
const OTHER_SITE = { siteId: 'site-2', hostname: 'blog.dotcms.com', path: '/images/' };

describe('buildAssetPickerConfig', () => {
    beforeEach(() => window.localStorage.clear());

    describe('File field', () => {
        it('should pre-select the contentlet locale', () => {
            const config = buildAssetPickerConfig({ mode: 'file', site: SITE, languageId: '1' });

            expect(config.languageId).toBe('1');
        });

        it('should not pre-select any base type', () => {
            const config = buildAssetPickerConfig({ mode: 'file', site: SITE, languageId: '1' });

            expect(config.baseTypes).toBeUndefined();
        });

        it('should still only offer the asset-bearing base types', () => {
            // AC (#36836): the selector offers dotAsset + File Asset in BOTH modes. "Nothing
            // pre-selected" must not degrade into "everything offered", or the File field lists
            // Widget and Content.
            const config = buildAssetPickerConfig({ mode: 'file', site: SITE, languageId: '1' });

            expect(config.allowedBaseTypes).toEqual(['DOTASSET', 'FILEASSET']);
        });

        it('should not apply a mimetype restriction', () => {
            const config = buildAssetPickerConfig({ mode: 'file', site: SITE, languageId: '1' });

            expect(config.mimeTypes).toBeUndefined();
        });
    });

    describe('Image field', () => {
        it('should pre-select the contentlet locale', () => {
            const config = buildAssetPickerConfig({ mode: 'image', site: SITE, languageId: '1' });

            expect(config.languageId).toBe('1');
        });

        it('should pre-select the dotAsset and File Asset base types', () => {
            const config = buildAssetPickerConfig({ mode: 'image', site: SITE });

            expect(config.baseTypes).toEqual(['DOTASSET', 'FILEASSET']);
        });

        it('should offer only the asset-bearing base types', () => {
            const config = buildAssetPickerConfig({ mode: 'image', site: SITE });

            expect(config.allowedBaseTypes).toEqual(['DOTASSET', 'FILEASSET']);
        });

        it('should apply the image mimetype restriction', () => {
            const config = buildAssetPickerConfig({ mode: 'image', site: SITE });

            expect(config.mimeTypes).toEqual(['image/*']);
        });

        it('should keep the mimetype restriction out of anything filter-shaped', () => {
            // FR: the mime filter is transparent. It lives on the config, never in the filter bag,
            // so no chip can ever render it.
            const config = buildAssetPickerConfig({ mode: 'image', site: SITE });

            expect(config.baseTypes).not.toContain('image/*');
            expect(config.languageId).not.toBe('image/*');
        });

        it('should hand back a fresh array each call', () => {
            // Callers must not be able to mutate the shared constant through the config.
            const first = buildAssetPickerConfig({ mode: 'image', site: SITE });
            first.baseTypes?.push('WIDGET');
            first.allowedBaseTypes?.push('WIDGET');

            const second = buildAssetPickerConfig({ mode: 'image', site: SITE });

            expect(second.baseTypes).toEqual(['DOTASSET', 'FILEASSET']);
            expect(second.allowedBaseTypes).toEqual(['DOTASSET', 'FILEASSET']);
        });
    });

    describe('starting location', () => {
        it('should be undefined when nothing is remembered and none is given', () => {
            const config = buildAssetPickerConfig({ mode: 'file', site: SITE });

            expect(config.path).toBeUndefined();
            expect(config.browseSite).toBeUndefined();
        });

        it('should fall back to the remembered global location', () => {
            writeLastAssetLocation(OTHER_SITE);

            const config = buildAssetPickerConfig({ mode: 'file', site: SITE });

            expect(config.path).toBe('/images/');
        });

        it('should reopen on the remembered site, not the site being edited', () => {
            // The picker browses every site, so a remembered `/images/` belongs to the site it was
            // picked from — applying it to the editor's site would open a folder that may not exist.
            writeLastAssetLocation(OTHER_SITE);

            const config = buildAssetPickerConfig({ mode: 'file', site: SITE });

            expect(config.browseSite).toEqual({
                identifier: 'site-2',
                hostname: 'blog.dotcms.com'
            });
            // The entry site still travels through — it is the upload fallback.
            expect(config.site).toBe(SITE);
        });

        it('should apply a legacy site-less path to the site being edited', () => {
            window.localStorage.setItem(LAST_ASSET_PATH_KEY, '"/images/"');

            const config = buildAssetPickerConfig({ mode: 'file', site: SITE });

            expect(config.path).toBe('/images/');
            expect(config.browseSite).toBeUndefined();
        });

        it('should prefer an explicit path over the remembered one', () => {
            writeLastAssetLocation(OTHER_SITE);

            const config = buildAssetPickerConfig({
                mode: 'file',
                site: SITE,
                initialAssetPath: '/docs/'
            });

            expect(config.path).toBe('/docs/');
            // An explicit path is about the entry site, so the remembered site must not tag along.
            expect(config.browseSite).toBeUndefined();
        });

        it('should share the remembered location across modes', () => {
            // The value is global, not per field: a location stored from an Image field is what a
            // File field opens on next.
            writeLastAssetLocation(OTHER_SITE);

            expect(buildAssetPickerConfig({ mode: 'image', site: SITE }).path).toBe('/images/');
            expect(buildAssetPickerConfig({ mode: 'file', site: SITE }).path).toBe('/images/');
        });
    });

    it('should always carry the site through', () => {
        expect(buildAssetPickerConfig({ mode: 'file', site: SITE }).site).toBe(SITE);
    });
});
