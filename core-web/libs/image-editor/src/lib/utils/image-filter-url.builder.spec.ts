import { buildFilterChain, buildPreviewUrl, cleanUrl, toHsb } from './image-filter-url.builder';

import {
    AdjustState,
    CompressionMode,
    CropState,
    FileInfoState,
    ImageEditorAssetContext,
    TransformState
} from '../models/image-editor.models';

const baseAdjust: AdjustState = {
    brightness: 0,
    hue: 0,
    saturation: 0,
    grayscale: false
};

const baseTransform: TransformState = {
    scale: 100,
    rotateDeg: 0,
    flipH: false,
    flipV: false,
    outputWidth: null,
    outputHeight: null,
    lockAspectRatio: true
};

const baseCrop: CropState = {
    x: 0,
    y: 0,
    w: 0,
    h: 0,
    active: false,
    aspect: null
};

const baseFileInfo: FileInfoState = {
    compression: 'none',
    quality: 65,
    currentBytes: null,
    originalBytes: null
};

const baseContext: ImageEditorAssetContext = {
    idOrTempId: 'abc123',
    inode: 'abc123',
    tempId: null,
    variable: 'fileAsset',
    fieldName: 'fileAsset',
    fileName: 'image.png',
    mimeType: 'image/png',
    isTempFile: false,
    byInode: false,
    naturalWidth: 1000,
    naturalHeight: 800,
    originalUrl: '/contentAsset/image/abc123/fileAsset'
};

function chain(
    overrides: Partial<{
        adjust: Partial<AdjustState>;
        transform: Partial<TransformState>;
        crop: Partial<CropState>;
        fileInfo: Partial<FileInfoState>;
        naturalWidth: number;
        naturalHeight: number;
    }> = {}
) {
    return buildFilterChain({
        adjust: { ...baseAdjust, ...overrides.adjust },
        transform: { ...baseTransform, ...overrides.transform },
        crop: { ...baseCrop, ...overrides.crop },
        fileInfo: { ...baseFileInfo, ...overrides.fileInfo },
        naturalWidth: overrides.naturalWidth ?? 1000,
        naturalHeight: overrides.naturalHeight ?? 800
    });
}

describe('image-filter-url.builder', () => {
    describe('toHsb', () => {
        it('formats positive midpoint with two decimals', () => {
            expect(toHsb(50)).toBe('0.50');
        });

        it('formats negative bounds with two decimals', () => {
            expect(toHsb(-100)).toBe('-1.00');
        });

        it('formats zero', () => {
            expect(toHsb(0)).toBe('0.00');
        });
    });

    describe('buildFilterChain - individual controls', () => {
        it('produces no filters for the default state', () => {
            expect(chain()).toEqual([]);
        });

        it('builds a Resize filter from explicit output dimensions', () => {
            const result = chain({ transform: { outputWidth: 500, outputHeight: 250 } });
            expect(result).toEqual([{ name: 'Resize', args: '/resize_w/500/resize_h/250' }]);
        });

        it('builds a Resize filter when only width is set', () => {
            const result = chain({ transform: { outputWidth: 500 } });
            expect(result).toEqual([{ name: 'Resize', args: '/resize_w/500' }]);
        });

        it('builds a Crop filter with rounded params', () => {
            const result = chain({
                crop: { active: true, x: 10.4, y: 20.6, w: 100.5, h: 50.2 }
            });
            expect(result).toEqual([
                { name: 'Crop', args: '/crop_w/101/crop_h/50/crop_x/10/crop_y/21' }
            ]);
        });

        it('does not crop when inactive', () => {
            const result = chain({ crop: { active: false, x: 0, y: 0, w: 100, h: 50 } });
            expect(result).toEqual([]);
        });

        it('applies Crop after Flip/Rotate so it crops the image as displayed', () => {
            const result = chain({
                transform: { flipH: true, rotateDeg: 90 },
                crop: { active: true, x: 10, y: 20, w: 100, h: 50 }
            });
            const names = result.map((f) => f.name);
            const rotateIdx = names.indexOf('Rotate');
            const flipIdx = names.indexOf('Flip');
            const cropIdx = names.indexOf('Crop');

            expect(cropIdx).toBeGreaterThan(rotateIdx);
            expect(cropIdx).toBeGreaterThan(flipIdx);
        });

        it('builds a Rotate filter', () => {
            const result = chain({ transform: { rotateDeg: 90 } });
            expect(result).toEqual([{ name: 'Rotate', args: '/rotate_a/90.0' }]);
        });

        it('builds a Grayscale filter', () => {
            const result = chain({ adjust: { grayscale: true } });
            expect(result).toEqual([{ name: 'Grayscale', args: '/grayscale/1' }]);
        });

        it('builds an Hsb filter in h/s/b order', () => {
            const result = chain({
                adjust: { hue: 50, saturation: -100, brightness: 25 }
            });
            expect(result).toEqual([{ name: 'Hsb', args: '/hsb_h/0.50/hsb_s/-1.00/hsb_b/0.25' }]);
        });

        it('builds an Hsb filter when only one channel is non-zero', () => {
            const result = chain({ adjust: { brightness: 10 } });
            expect(result).toEqual([{ name: 'Hsb', args: '/hsb_h/0.00/hsb_s/0.00/hsb_b/0.10' }]);
        });
    });

    describe('buildFilterChain - resize removes crop', () => {
        it('drops the Crop filter when resizing', () => {
            const result = chain({
                transform: { outputWidth: 600 },
                crop: { active: true, x: 0, y: 0, w: 100, h: 100 }
            });
            expect(result).toEqual([{ name: 'Resize', args: '/resize_w/600' }]);
            expect(result.some((f) => f.name === 'Crop')).toBe(false);
        });

        it('treats a non-100 scale as resizing and drops crop', () => {
            const result = chain({
                transform: { scale: 50 },
                crop: { active: true, x: 0, y: 0, w: 100, h: 100 }
            });
            expect(result.some((f) => f.name === 'Crop')).toBe(false);
        });

        it('builds a Resize filter from scale% × the natural size', () => {
            // 50% of the 1000×800 natural size.
            const result = chain({ transform: { scale: 50 } });
            expect(result).toEqual([{ name: 'Resize', args: '/resize_w/500/resize_h/400' }]);
        });
    });

    describe('buildFilterChain - flip rules', () => {
        it('expresses vertical flip as flip token plus 180deg rotation', () => {
            const result = chain({ transform: { flipV: true } });
            expect(result).toEqual([
                { name: 'Rotate', args: '/rotate_a/180.0' },
                { name: 'Flip', args: '/flip_flip/1' }
            ]);
        });

        it('combines an existing rotation with the vertical-flip rotation', () => {
            const result = chain({ transform: { rotateDeg: 90, flipV: true } });
            expect(result).toContainEqual({ name: 'Rotate', args: '/rotate_a/270.0' });
        });

        it('emits a single Flip token for a horizontal flip', () => {
            const result = chain({ transform: { flipH: true } });
            expect(result).toEqual([{ name: 'Flip', args: '/flip_flip/1' }]);
        });

        it('cancels the flip token when both H and V flips are active', () => {
            const result = chain({ transform: { flipH: true, flipV: true } });
            expect(result.some((f) => f.name === 'Flip')).toBe(false);
            expect(result).toContainEqual({ name: 'Rotate', args: '/rotate_a/180.0' });
        });
    });

    describe('buildFilterChain - compression', () => {
        const cases: Array<[CompressionMode, string, string]> = [
            ['jpeg', '/jpeg_q/80', 'Jpeg'],
            ['webp', '/webp_q/80', 'WebP'],
            ['avif', '/avif_q/80', 'avif'],
            ['auto', '/quality_q/80', 'Quality']
        ];

        it.each(cases)('appends %s compression last', (mode, args, name) => {
            const result = chain({
                adjust: { grayscale: true },
                fileInfo: { compression: mode, quality: 80 }
            });
            expect(result[result.length - 1]).toEqual({ name, args });
        });

        it('adds no compression filter for mode none', () => {
            const result = chain({ fileInfo: { compression: 'none', quality: 80 } });
            expect(result).toEqual([]);
        });

        it('applies only one compression filter (mutual exclusion)', () => {
            const result = chain({ fileInfo: { compression: 'webp', quality: 50 } });
            const compressionFilters = result.filter((f) =>
                ['Jpeg', 'WebP', 'avif', 'Quality'].includes(f.name)
            );
            expect(compressionFilters).toHaveLength(1);
        });

        it('clamps the quality value into 0..100', () => {
            const high = chain({ fileInfo: { compression: 'jpeg', quality: 150 } });
            const low = chain({ fileInfo: { compression: 'jpeg', quality: -20 } });
            expect(high[0]).toEqual({ name: 'Jpeg', args: '/jpeg_q/100' });
            expect(low[0]).toEqual({ name: 'Jpeg', args: '/jpeg_q/0' });
        });
    });

    describe('cleanUrl', () => {
        it('collapses repeated slashes in the path', () => {
            expect(cleanUrl('/contentAsset//image///abc')).toBe('/contentAsset/image/abc');
        });

        it('preserves the protocol separator', () => {
            expect(cleanUrl('https://host//a//b')).toBe('https://host/a/b');
        });
    });

    describe('buildPreviewUrl', () => {
        it('returns the base url with a cache-buster for an empty chain', () => {
            const url = buildPreviewUrl(baseContext, [], 12345);
            expect(url).toBe('/contentAsset/image/abc123/fileAsset?test=12345');
        });

        it('appends the filter segment for a non-empty chain', () => {
            const filters = chain({ adjust: { grayscale: true } });
            const url = buildPreviewUrl(baseContext, filters, 999);
            expect(url).toBe(
                '/contentAsset/image/abc123/fileAsset/filter/Grayscale/grayscale/1?test=999'
            );
        });

        it('joins multiple filter names with commas and concatenates args', () => {
            const filters = chain({
                transform: { rotateDeg: 90 },
                adjust: { grayscale: true }
            });
            const url = buildPreviewUrl(baseContext, filters, 1);
            expect(url).toContain('/filter/Rotate,Grayscale/rotate_a/90.0/grayscale/1');
        });

        it('uses & for the cache-buster when the url already has a query', () => {
            const ctx = { ...baseContext, originalUrl: '/contentAsset/image/abc123/fileAsset?x=1' };
            const url = buildPreviewUrl(ctx, [], 7);
            expect(url).toBe('/contentAsset/image/abc123/fileAsset?x=1&test=7');
        });

        it('appends &byInode=true when the context requires it', () => {
            const ctx = { ...baseContext, byInode: true };
            const url = buildPreviewUrl(ctx, [], 42);
            expect(url).toBe('/contentAsset/image/abc123/fileAsset?test=42&byInode=true');
        });

        it('collapses redundant slashes from the original url', () => {
            const ctx = { ...baseContext, originalUrl: '/contentAsset//image//abc123/fileAsset' };
            const url = buildPreviewUrl(ctx, [], 5);
            expect(url).toBe('/contentAsset/image/abc123/fileAsset?test=5');
        });
    });
});
