import { DotCMSContentlet, DotFileMetadata } from '@dotcms/dotcms-models';

import {
    focalPointFromContentlet,
    focalPointFromMetadata,
    parseFocalPoint
} from './focal-point.util';

describe('focalPointFromContentlet', () => {
    it('reads a dotAsset focal from assetMetaData (titleImage=asset)', () => {
        const file = {
            titleImage: 'asset',
            assetMetaData: { focalPoint: '0.4,0.6' }
        } as unknown as DotCMSContentlet;
        expect(focalPointFromContentlet(file)).toBe('0.4,0.6');
    });

    it('reads a legacy FileAsset focal from fileAssetMetaData (titleImage=fileAsset)', () => {
        const file = {
            titleImage: 'fileAsset',
            metaData: { name: 'a.png' },
            fileAssetMetaData: { focalPoint: '0.76,0.13' }
        } as unknown as DotCMSContentlet;
        expect(focalPointFromContentlet(file)).toBe('0.76,0.13');
    });

    it('defaults the field variable to asset when titleImage is absent', () => {
        const file = { assetMetaData: { focalPoint: '0.1,0.2' } } as unknown as DotCMSContentlet;
        expect(focalPointFromContentlet(file)).toBe('0.1,0.2');
    });

    it('returns undefined when the field metadata carries no focal', () => {
        expect(focalPointFromContentlet(null)).toBeUndefined();
        expect(
            focalPointFromContentlet({
                titleImage: 'fileAsset',
                fileAssetMetaData: { name: 'a.png' }
            } as unknown as DotCMSContentlet)
        ).toBeUndefined();
    });
});

describe('focalPointFromMetadata', () => {
    it('reads the focalPoint key', () => {
        expect(focalPointFromMetadata({ focalPoint: '0.4,0.6' } as DotFileMetadata)).toBe(
            '0.4,0.6'
        );
    });

    it('returns undefined for null/undefined or when the key is absent', () => {
        expect(focalPointFromMetadata(null)).toBeUndefined();
        expect(focalPointFromMetadata(undefined)).toBeUndefined();
        expect(focalPointFromMetadata({} as DotFileMetadata)).toBeUndefined();
    });
});

describe('parseFocalPoint', () => {
    it('parses an "x,y" focal point string into a point', () => {
        expect(parseFocalPoint('0.88,0.31')).toEqual({ x: 0.88, y: 0.31 });
    });

    it('returns undefined for an unset/empty value', () => {
        expect(parseFocalPoint(undefined)).toBeUndefined();
        expect(parseFocalPoint(null)).toBeUndefined();
        expect(parseFocalPoint('')).toBeUndefined();
    });

    it('treats the (0,0) "no focal point" sentinel as undefined', () => {
        // The backend uses (0,0) to mean "no focal point" -> editor opens centred.
        expect(parseFocalPoint('0,0')).toBeUndefined();
    });

    it('returns undefined for a malformed or single-value string', () => {
        // "0.0" has no comma -> one token -> the y axis parses to NaN.
        expect(parseFocalPoint('0.0')).toBeUndefined();
        expect(parseFocalPoint('not-a-point')).toBeUndefined();
        expect(parseFocalPoint('abc,xyz')).toBeUndefined();
    });
});
