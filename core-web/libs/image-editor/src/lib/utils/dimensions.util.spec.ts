import { clamp, computeOutputDimensions, computeResizeParams } from './dimensions.util';

import { CropState, TransformState } from '../models/image-editor.models';
import {
    initialCropState,
    initialImageEditorState,
    initialTransformState
} from '../store/image-editor.state';

/** A transform slice with the given overrides on top of the defaults. */
const transform = (overrides: Partial<TransformState> = {}): TransformState => ({
    ...initialTransformState,
    ...overrides
});

/** A crop slice with the given overrides on top of the defaults. */
const crop = (overrides: Partial<CropState> = {}): CropState => ({
    ...initialCropState,
    ...overrides
});

/** An asset context seeded with the given natural dimensions. */
const ctx = (naturalWidth: number, naturalHeight: number) => ({
    ...initialImageEditorState.assetContext,
    naturalWidth,
    naturalHeight
});

describe('dimensions.util', () => {
    describe('clamp', () => {
        it('returns the value when within range', () => {
            expect(clamp(5, 0, 10)).toBe(5);
        });

        it('clamps to the lower bound', () => {
            expect(clamp(-5, 0, 10)).toBe(0);
        });

        it('clamps to the upper bound', () => {
            expect(clamp(15, 0, 10)).toBe(10);
        });

        it('treats both bounds as inclusive', () => {
            expect(clamp(0, 0, 10)).toBe(0);
            expect(clamp(10, 0, 10)).toBe(10);
        });
    });

    describe('computeResizeParams', () => {
        const natural = { width: 1000, height: 800 };

        it('returns null/null when no resize is constrained (scale 100, no output)', () => {
            expect(computeResizeParams(transform(), natural)).toEqual({
                width: null,
                height: null
            });
        });

        it('uses explicit output width and height when both are set', () => {
            expect(
                computeResizeParams(transform({ outputWidth: 320.4, outputHeight: 240.6 }), natural)
            ).toEqual({ width: 320, height: 241 });
        });

        it('returns only the width when just the width is set', () => {
            expect(computeResizeParams(transform({ outputWidth: 500 }), natural)).toEqual({
                width: 500,
                height: null
            });
        });

        it('returns only the height when just the height is set', () => {
            expect(computeResizeParams(transform({ outputHeight: 250 }), natural)).toEqual({
                width: null,
                height: 250
            });
        });

        it('scales the natural size by the scale percentage when no output is set', () => {
            expect(computeResizeParams(transform({ scale: 50 }), natural)).toEqual({
                width: 500,
                height: 400
            });
        });
    });

    describe('computeOutputDimensions', () => {
        it('returns the natural size with no edits', () => {
            expect(computeOutputDimensions(ctx(1000, 800), transform(), crop())).toEqual({
                width: 1000,
                height: 800
            });
        });

        it('uses the crop size when an active crop is present and not resizing', () => {
            expect(
                computeOutputDimensions(
                    ctx(1000, 800),
                    transform(),
                    crop({ active: true, w: 200.4, h: 150.6 })
                )
            ).toEqual({ width: 200, height: 151 });
        });

        it('ignores the crop when resizing (resize supersedes crop)', () => {
            expect(
                computeOutputDimensions(
                    ctx(1000, 800),
                    transform({ scale: 50 }),
                    crop({ active: true, w: 200, h: 150 })
                )
            ).toEqual({ width: 500, height: 400 });
        });

        it('uses explicit output width and height when both are set', () => {
            expect(
                computeOutputDimensions(
                    ctx(1000, 800),
                    transform({ outputWidth: 300, outputHeight: 200 }),
                    crop()
                )
            ).toEqual({ width: 300, height: 200 });
        });

        it('derives the height from the aspect ratio when only width is set', () => {
            // aspect 1000/800 = 1.25 → height = round(500 / 1.25) = 400
            expect(
                computeOutputDimensions(ctx(1000, 800), transform({ outputWidth: 500 }), crop())
            ).toEqual({ width: 500, height: 400 });
        });

        it('derives the width from the aspect ratio when only height is set', () => {
            // height 400 → width = round(400 * 1.25) = 500
            expect(
                computeOutputDimensions(ctx(1000, 800), transform({ outputHeight: 400 }), crop())
            ).toEqual({ width: 500, height: 400 });
        });

        it('falls back to aspect 1 when the natural height is zero', () => {
            // height 0 → aspect defaults to 1 → height = round(500 / 1) = 500
            expect(
                computeOutputDimensions(ctx(1000, 0), transform({ outputWidth: 500 }), crop())
            ).toEqual({ width: 500, height: 500 });
        });

        it('scales by the scale percentage when no explicit output is set', () => {
            expect(
                computeOutputDimensions(ctx(1000, 800), transform({ scale: 25 }), crop())
            ).toEqual({ width: 250, height: 200 });
        });
    });
});
