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

    describe('Story Block media nodes', () => {
        // The Story Block opens the same picker as the fields, one mode per media node. Each is
        // narrowed to its own mimetype: a `dotVideo` node pointing at an mp3 is as broken as an
        // Image field returning a PDF.
        it.each([
            ['video', ['video/*']],
            ['audio', ['audio/*']]
        ] as const)('should restrict %s to its own mimetype', (mode, mimeTypes) => {
            expect(buildAssetPickerConfig({ mode, site: SITE }).mimeTypes).toEqual(mimeTypes);
        });

        it.each(['video', 'audio'] as const)(
            'should treat %s like the other media modes',
            (mode) => {
                const config = buildAssetPickerConfig({ mode, site: SITE, languageId: '1' });

                expect(config.baseTypes).toEqual(['DOTASSET', 'FILEASSET']);
                expect(config.allowedBaseTypes).toEqual(['DOTASSET', 'FILEASSET']);
                expect(config.languageId).toBe('1');
            }
        );

        it('should hand back a fresh mimetype array each call', () => {
            const first = buildAssetPickerConfig({ mode: 'video', site: SITE });
            first.mimeTypes?.push('image/*');

            expect(buildAssetPickerConfig({ mode: 'video', site: SITE }).mimeTypes).toEqual([
                'video/*'
            ]);
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

    /**
     * `browse` is `DotCustomFieldApi.openBrowserModal` — the only entry point that may ask for
     * folders, menu links or pages. Everything it adds is opt-in, so the four existing modes are
     * unaffected; the guards for that live in the "opt-in guarantee" block below.
     */
    describe('browse mode (openBrowserModal)', () => {
        it('should not apply any mimetype restriction of its own', () => {
            // Like `file`, and unlike the media modes: a custom field browsing for a page must not
            // be silently narrowed to images.
            const config = buildAssetPickerConfig({ mode: 'browse', site: SITE });

            expect(config.mimeTypes).toBeUndefined();
        });

        it('should offer only the asset base types when no kinds are requested', () => {
            // The default is today's behaviour, so a caller that asks for nothing new browses
            // assets exactly as every other entry point does.
            const config = buildAssetPickerConfig({ mode: 'browse', site: SITE });

            expect(config.allowedBaseTypes).toEqual(['DOTASSET', 'FILEASSET']);
        });

        it('should offer HTMLPAGE when the caller asks for pages', () => {
            const config = buildAssetPickerConfig({
                mode: 'browse',
                site: SITE,
                allowedBaseTypes: ['FILEASSET', 'HTMLPAGE']
            });

            expect(config.allowedBaseTypes).toEqual(['FILEASSET', 'HTMLPAGE']);
        });

        it('should carry the browse options through untouched', () => {
            const browse = {
                showFolders: true,
                showLinks: true,
                showWorking: false,
                showArchived: false,
                sortByDesc: true,
                extensions: ['jpg']
            };

            const config = buildAssetPickerConfig({ mode: 'browse', site: SITE, browse });

            expect(config.browse).toEqual(browse);
        });

        it('should use the caller-supplied title', () => {
            // The picker renders its own header, so the title travels in the config rather than in
            // `DynamicDialogConfig.header`.
            const config = buildAssetPickerConfig({
                mode: 'browse',
                site: SITE,
                title: 'Select a Page'
            });

            expect(config.title).toBe('Select a Page');
        });

        it('should honour an explicit starting path', () => {
            const config = buildAssetPickerConfig({
                mode: 'browse',
                site: SITE,
                initialAssetPath: '/application/'
            });

            expect(config.path).toBe('/application/');
        });
    });

    /**
     * The opt-in guarantee: absent `browse`, every existing entry point must be byte-identical to
     * what it was before folders, links and pages became expressible. These are the guards US3
     * relies on — if one fails, the defaults are wrong, not the guard.
     */
    describe('opt-in guarantee', () => {
        it.each(['file', 'image', 'video', 'audio'] as const)(
            'should not set browse options for the %s entry point',
            (mode) => {
                expect(buildAssetPickerConfig({ mode, site: SITE }).browse).toBeUndefined();
            }
        );

        it.each(['file', 'image', 'video', 'audio'] as const)(
            'should never offer HTMLPAGE to the %s entry point',
            (mode) => {
                // Widening the picker for openBrowserModal is exactly how pages would leak into a
                // field that cannot store one.
                expect(buildAssetPickerConfig({ mode, site: SITE }).allowedBaseTypes).toEqual([
                    'DOTASSET',
                    'FILEASSET'
                ]);
            }
        );
    });
});
