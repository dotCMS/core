import { DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';
import { EMPTY_CONTENTLET } from '@dotcms/utils-testing';

import { getImageAssetUrl, ellipsizeText } from './dot-utils';

describe('Dot Utils', () => {
    describe('getImageAssetUrl', () => {
        it('should return fileAssetVersion when baseType is FILEASSET and fileAssetVersion is defined', () => {
            const contentlet = {
                ...EMPTY_CONTENTLET,
                baseType: DotCMSBaseTypesContentTypes.FILEASSET,
                fileAssetVersion: 'fileAssetVersion',
                fileAsset: 'fileAsset'
            };

            expect(getImageAssetUrl(contentlet)).toEqual('fileAssetVersion');
        });

        it('should return fileAsset when baseType is FILEASSET and fileAssetVersion is not defined', () => {
            const contentlet = {
                ...EMPTY_CONTENTLET,
                baseType: DotCMSBaseTypesContentTypes.FILEASSET,
                fileAsset: 'fileAsset'
            };

            expect(getImageAssetUrl(contentlet)).toEqual('fileAsset');
        });

        it('should return assetVersion when baseType is DOTASSET and assetVersion is defined', () => {
            const contentlet = {
                ...EMPTY_CONTENTLET,
                baseType: DotCMSBaseTypesContentTypes.DOTASSET,
                assetVersion: 'assetVersion',
                asset: 'asset'
            };

            expect(getImageAssetUrl(contentlet)).toEqual('assetVersion');
        });

        it('should return asset when baseType is DOTASSET and assetVersion is not defined', () => {
            const contentlet = {
                ...EMPTY_CONTENTLET,
                baseType: DotCMSBaseTypesContentTypes.DOTASSET,
                asset: 'asset'
            };

            expect(getImageAssetUrl(contentlet)).toEqual('asset');
        });

        it('should return asset when baseType is not FILEASSET or DOTASSET', () => {
            const contentlet = {
                ...EMPTY_CONTENTLET,
                baseType: 'OTHER',
                asset: 'asset'
            };

            expect(getImageAssetUrl(contentlet)).toEqual('asset');
        });

        it('should return empty string when asset is not defined and baseType is not FILEASSET or DOTASSET', () => {
            const contentlet = {
                ...EMPTY_CONTENTLET,
                baseType: 'OTHER'
            };

            expect(getImageAssetUrl(contentlet)).toEqual('');
        });
    });
});

describe('Ellipsize Text Utility', () => {
    it('should return empty string when text is undefined', () => {
        expect(ellipsizeText(undefined, 10)).toEqual('');
    });

    it('should return empty string when text is empty', () => {
        expect(ellipsizeText('', 10)).toEqual('');
    });

    it('should return empty string when limit is 0', () => {
        expect(ellipsizeText('Any text', 0)).toEqual('');
    });

    it('should return empty string when limit is negative', () => {
        expect(ellipsizeText('Any text', -5)).toEqual('');
    });

    it('should return the original text when it is shorter than the limit', () => {
        const text = 'Short text';
        expect(ellipsizeText(text, 20)).toEqual(text);
    });

    it('should truncate the text with ellipsis when it exceeds the limit', () => {
        const text = 'This is a longer text that needs truncation';
        const truncated = 'This is a longer...';
        expect(ellipsizeText(text, 18)).toEqual(truncated);
    });

    it('should handle no spaces correctly and truncate at the limit', () => {
        const text = 'Thisisaverylongwordthatneedstruncation';
        const truncated = 'Thisisaverylongwo...';
        expect(ellipsizeText(text, 18)).toEqual(truncated);
    });
    it('should return empty string when a non-string value is passed', () => {
        expect(ellipsizeText(12345 as any, 10)).toEqual('');
        expect(ellipsizeText({} as any, 10)).toEqual('');
        expect(ellipsizeText([] as any, 10)).toEqual('');
        expect(ellipsizeText(null, 10)).toEqual('');
    });
    it('should return empty string when limit is not a number', () => {
        const text = 'Any text';
        expect(ellipsizeText(text, NaN)).toEqual('');
        expect(ellipsizeText(text, 'abc' as any)).toEqual('');
        expect(ellipsizeText(text, {} as any)).toEqual('');
        expect(ellipsizeText(text, [] as any)).toEqual('');
        expect(ellipsizeText(text, null)).toEqual('');
    });

    it('should truncate text at word boundary if there is a space before the limit', () => {
        const text = 'This is a longer text that needs truncation';
        const truncated = 'This is a...';
        expect(ellipsizeText(text, 15)).toEqual(truncated);
    });
});
