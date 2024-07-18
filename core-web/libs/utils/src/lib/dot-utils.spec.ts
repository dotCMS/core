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

    it('should return the original text when it is shorter than the limit', () => {
        const text = 'Short text';
        expect(ellipsizeText(text, 20)).toEqual(text);
    });

    it('should truncate the text with ellipsis when it exceeds the limit', () => {
        const text = 'This is a longer text that needs truncation';
        const truncated = 'This is a longer...';
        expect(ellipsizeText(text, 20)).toEqual(truncated);
    });

    it('should truncate the text at the nearest word boundary', () => {
        const text = 'This is a longer text that needs truncation';
        const truncated = 'This is a...';
        expect(ellipsizeText(text, 15)).toEqual(truncated);
    });

    it('should return the truncated text with ellipsis even if there is no space before the limit', () => {
        const text = 'Thisisaverylongwordthatneedstruncation';
        const truncated = 'Thisisaverylongw...';
        expect(ellipsizeText(text, 17)).toEqual(truncated);
    });

    it('should handle edge cases where limit is 0', () => {
        const text = 'Any text';
        const truncated = '...';
        expect(ellipsizeText(text, 0)).toEqual(truncated);
    });
});
