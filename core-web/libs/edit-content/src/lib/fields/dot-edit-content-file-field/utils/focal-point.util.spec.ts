import { parseFocalPoint } from './focal-point.util';

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
