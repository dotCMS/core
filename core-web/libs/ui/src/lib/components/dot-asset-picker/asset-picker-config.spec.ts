import { DotSite } from '@dotcms/dotcms-models';

import { buildAssetPickerConfig } from './asset-picker-config';
import { writeLastAssetPath } from './last-asset-path';

const SITE: DotSite = {
    identifier: 'site-1',
    hostname: 'dotcms.com',
    aliases: null,
    archived: false
};

describe('buildAssetPickerConfig', () => {
    beforeEach(() => window.localStorage.clear());

    describe('File field', () => {
        it('should pre-select the contentlet locale', () => {
            const config = buildAssetPickerConfig({ mode: 'file', site: SITE, languageId: '1' });

            expect(config.languageId).toBe('1');
        });

        it('should not restrict base types', () => {
            const config = buildAssetPickerConfig({ mode: 'file', site: SITE, languageId: '1' });

            expect(config.baseTypes).toBeUndefined();
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

            const second = buildAssetPickerConfig({ mode: 'image', site: SITE });

            expect(second.baseTypes).toEqual(['DOTASSET', 'FILEASSET']);
        });
    });

    describe('starting folder', () => {
        it('should be undefined when nothing is remembered and none is given', () => {
            const config = buildAssetPickerConfig({ mode: 'file', site: SITE });

            expect(config.path).toBeUndefined();
        });

        it('should fall back to the remembered global path', () => {
            writeLastAssetPath('/images/');

            const config = buildAssetPickerConfig({ mode: 'file', site: SITE });

            expect(config.path).toBe('/images/');
        });

        it('should prefer an explicit path over the remembered one', () => {
            writeLastAssetPath('/images/');

            const config = buildAssetPickerConfig({
                mode: 'file',
                site: SITE,
                initialAssetPath: '/docs/'
            });

            expect(config.path).toBe('/docs/');
        });

        it('should share the remembered path across modes', () => {
            // The value is global, not per field: a path stored from an Image field is what a File
            // field opens on next.
            writeLastAssetPath('/images/');

            expect(buildAssetPickerConfig({ mode: 'image', site: SITE }).path).toBe('/images/');
            expect(buildAssetPickerConfig({ mode: 'file', site: SITE }).path).toBe('/images/');
        });
    });

    it('should always carry the site through', () => {
        expect(buildAssetPickerConfig({ mode: 'file', site: SITE }).site).toBe(SITE);
    });
});
