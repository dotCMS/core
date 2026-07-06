import { DotFileMetadata } from '@dotcms/dotcms-models';

import { focalPointFromMetadata, parseFocalPoint } from './focal-point.util';

describe('focalPointFromMetadata', () => {
    it('reads the clean focalPoint key (Binary field metadata)', () => {
        expect(focalPointFromMetadata({ focalPoint: '0.4,0.6' } as DotFileMetadata)).toBe(
            '0.4,0.6'
        );
    });

    it('falls back to the namespaced dot:focalPoint key (referenced dotAsset assetMetaData)', () => {
        expect(
            focalPointFromMetadata({ 'dot:focalPoint': '0.14,0.29' } as unknown as DotFileMetadata)
        ).toBe('0.14,0.29');
    });

    it('prefers the clean key when both are present', () => {
        expect(
            focalPointFromMetadata({
                focalPoint: '0.4,0.6',
                'dot:focalPoint': '0.1,0.2'
            } as unknown as DotFileMetadata)
        ).toBe('0.4,0.6');
    });

    it('returns undefined for null/undefined or when neither key is present', () => {
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
