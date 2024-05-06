import { DotCMSBaseTypesContentTypes } from '@dotcms/dotcms-models';
import { EMPTY_CONTENTLET } from '@dotcms/utils-testing';

import { getImageAssetUrl } from './dot-utils';

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
