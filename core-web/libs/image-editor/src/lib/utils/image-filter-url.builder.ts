import { computeResizeParams } from './dimensions.util';

import {
    AppliedFilter,
    CompressionMode,
    FilterChainInput,
    ImageEditorAssetContext,
    NormalizedPoint
} from '../models/image-editor.models';

/**
 * Formats an HSB slider value (-100..100) into the legacy `-1..1` string the
 * dotCMS Hsb filter expects, always with two decimals (e.g. 50 -> "0.50").
 * @param v - Slider value in the range -100..100
 * @returns The value divided by 100, fixed to two decimals
 */
export function toHsb(v: number): string {
    return (v / 100).toFixed(2);
}

/** Clamps a quality value into the valid 0..100 range and rounds it. */
function clampQuality(quality: number): number {
    return Math.min(100, Math.max(0, Math.round(quality)));
}

function compressionFilter(mode: CompressionMode, quality: number): AppliedFilter | null {
    const q = clampQuality(quality);
    switch (mode) {
        case 'jpeg':
            return { name: 'Jpeg', args: `/jpeg_q/${q}` };
        case 'webp':
            return { name: 'WebP', args: `/webp_q/${q}` };
        // AVIF is a libvips-only filter (registered lowercase as `avif`); it
        // takes the same 0..100 quality as jpeg/webp via `avif_q`.
        case 'avif':
            return { name: 'avif', args: `/avif_q/${q}` };
        case 'auto':
            return { name: 'Quality', args: `/quality_q/${q}` };
        case 'none':
        default:
            return null;
    }
}

/**
 * Builds the ordered list of server filters from the current edit state,
 * mirroring the legacy ImageEditor rules: resizing removes crop, vertical flip
 * is expressed as a 180deg rotation plus flip-token cancellation, and the
 * compression filter is always applied last and exclusively. Crop is placed
 * relative to the rotate/flip transforms by {@link FilterChainInput.cropBeforeTransforms}:
 * a crop drawn before rotating must run first (on the un-rotated image it was drawn
 * against), while a crop drawn on an already-rotated preview runs after — so the
 * server applies it in the same coordinate space the user saw.
 * @param input - The adjust/transform/crop/fileInfo slices plus the natural dimensions
 * @returns The applied filters in the exact order they must be concatenated
 */
export function buildFilterChain(input: FilterChainInput): AppliedFilter[] {
    const { adjust, transform, crop, fileInfo, naturalWidth, naturalHeight } = input;
    const filters: AppliedFilter[] = [];

    // Resize derives from explicit output dimensions OR scale% × natural size, so a
    // scale change with no explicit W/H still produces resize pixels.
    const resize = computeResizeParams(transform, { width: naturalWidth, height: naturalHeight });
    const hasResize = !!(resize.width || resize.height);

    if (hasResize) {
        let args = '';
        if (resize.width) {
            args += `/resize_w/${resize.width}`;
        }
        if (resize.height) {
            args += `/resize_h/${resize.height}`;
        }
        filters.push({ name: 'Resize', args });
    }

    // Resize and crop stay mutually exclusive: resize wins.
    const cropFilter =
        !hasResize && crop.active && crop.w > 0 && crop.h > 0
            ? {
                  name: 'Crop' as const,
                  args:
                      `/crop_w/${Math.round(crop.w)}` +
                      `/crop_h/${Math.round(crop.h)}` +
                      `/crop_x/${Math.round(crop.x)}` +
                      `/crop_y/${Math.round(crop.y)}`
              }
            : null;

    // A crop drawn BEFORE any rotate/flip is in the original image's coordinates, so
    // it must run first — the server crops the un-rotated image, then rotates the
    // result (matching the order the user performed). Applying it after the rotation
    // (the crop-last branch) would address the rotated image with original-space
    // coordinates and cut the wrong region.
    if (cropFilter && input.cropBeforeTransforms) {
        filters.push(cropFilter);
    }

    // Vertical flip is achieved by rotating 180deg and toggling the flip token.
    let rotation = transform.rotateDeg;
    if (transform.flipV) {
        rotation = (rotation + 180) % 360;
    }
    if (rotation !== 0) {
        filters.push({ name: 'Rotate', args: `/rotate_a/${rotation}.0` });
    }

    if (transform.flipH !== transform.flipV) {
        filters.push({ name: 'Flip', args: '/flip_flip/1' });
    }

    // A crop drawn on the already-transformed preview lives in the flipped/rotated
    // preview's coordinates ("crop what you see", matching the legacy editor, which
    // appends Crop to the already-transformed chain). Without this, a horizontal flip
    // would mirror the crop region.
    if (cropFilter && !input.cropBeforeTransforms) {
        filters.push(cropFilter);
    }

    if (adjust.grayscale) {
        filters.push({ name: 'Grayscale', args: '/grayscale/1' });
    }

    if (adjust.brightness !== 0 || adjust.hue !== 0 || adjust.saturation !== 0) {
        const args =
            `/hsb_h/${toHsb(adjust.hue)}` +
            `/hsb_s/${toHsb(adjust.saturation)}` +
            `/hsb_b/${toHsb(adjust.brightness)}`;
        filters.push({ name: 'Hsb', args });
    }

    const compression = compressionFilter(fileInfo.compression, fileInfo.quality);
    if (compression) {
        filters.push(compression);
    }

    return filters;
}

/**
 * Collapses repeated path slashes while preserving the protocol separator
 * (e.g. `http://host//a//b` -> `http://host/a/b`).
 * @param x - The URL to normalize
 * @returns The URL with redundant slashes removed
 */
export function cleanUrl(x: string): string {
    return x.replace(/([^:]\/)\/+/g, '$1');
}

/**
 * Builds the preview URL for the given asset and filter chain, appending a
 * cache-busting `test` parameter and the `byInode` flag when applicable.
 * @param ctx - Resolved asset context (provides the base URL and byInode flag)
 * @param chain - The ordered filters produced by {@link buildFilterChain}
 * @param cacheBust - A value used to bust the browser cache (e.g. `Date.now()`)
 * @returns The fully-qualified preview URL
 */
export function buildPreviewUrl(
    ctx: ImageEditorAssetContext,
    chain: AppliedFilter[],
    cacheBust: number
): string {
    let url = ctx.originalUrl;

    if (chain.length > 0) {
        const names = chain.map((filter) => filter.name).join(',');
        const args = chain.map((filter) => filter.args).join('');
        url = `${url}/filter/${names}${args}`;
    }

    url = cleanUrl(url);
    url += url.includes('?') ? `&test=${cacheBust}` : `?test=${cacheBust}`;

    if (ctx.byInode) {
        url += '&byInode=true';
    }

    return url;
}

/**
 * Compares two normalized focal points within a small tolerance. Focal coordinates are
 * floats (pointer math and 0.01 keyboard nudges that never round), so an exact `===` would
 * report a point nudged back to its origin as still-moved — leaving `isDirty` stuck true and
 * the discard guard armed, and the Save URL emitting a focal entry for a visually-unchanged point.
 * @param a - First focal point
 * @param b - Second focal point
 * @param epsilon - Maximum per-axis difference treated as equal (default 1e-4)
 * @returns Whether the two points are equal within the tolerance
 */
export function focalPointsEqual(a: NormalizedPoint, b: NormalizedPoint, epsilon = 1e-4): boolean {
    return Math.abs(a.x - b.x) < epsilon && Math.abs(a.y - b.y) < epsilon;
}

/** The neutral, "no focal point set" position the editor opens with by default. */
const CENTER_FOCAL_POINT: NormalizedPoint = { x: 0.5, y: 0.5 };

/**
 * Builds the Save URL: the same `/filter/<chain>` segment the preview uses, but
 * without the cache-bust and with the `_imageToolSaveFile` flag so the servlet
 * stages the filtered render into a temp file (returned as a {@link DotCMSTempFile}).
 *
 * The focal point is folded in as a trailing `FocalPoint` chain entry (the registry
 * resolves the name case-insensitively) and forces `overwrite=true` — which the
 * `FocalPointImageFilter` requires to stage the focal metadata onto the result — whenever
 * it holds a real (non-centre) value OR differs from the point the asset opened with (so
 * recentring a previously-set focal point still overwrites it, and an existing focal point
 * is re-staged onto the new temp rather than dropped). It is intentionally NOT part of the
 * preview chain, so it only ever reaches the server on Save.
 * @param ctx - Resolved asset context (provides the base URL)
 * @param chain - The ordered preview filters produced by {@link buildFilterChain}
 * @param focalPoint - The current normalized 0..1 focal point
 * @param seededFocalPoint - The focal point the asset was opened with (the dirty baseline)
 * @param binaryFieldId - The binary field variable (sent for legacy parity)
 * @returns The fully-qualified Save URL
 */
export function buildSaveUrl(
    ctx: ImageEditorAssetContext,
    chain: AppliedFilter[],
    focalPoint: NormalizedPoint,
    seededFocalPoint: NormalizedPoint,
    binaryFieldId: string
): string {
    // Persist the focal point (and force the server to commit it via `overwrite`) when it
    // holds a real, non-centre value OR the user moved it from what the asset opened with
    // (the latter also covers recentring a previously-set focal point back to the middle).
    const focalApplied =
        !focalPointsEqual(focalPoint, seededFocalPoint) ||
        !focalPointsEqual(focalPoint, CENTER_FOCAL_POINT);
    const saveChain: AppliedFilter[] = focalApplied
        ? [...chain, { name: 'FocalPoint', args: `/fp/${focalPoint.x},${focalPoint.y}` }]
        : chain;

    let url = ctx.originalUrl;

    if (saveChain.length > 0) {
        const names = saveChain.map((filter) => filter.name).join(',');
        const args = saveChain.map((filter) => filter.args).join('');
        url = `${url}/filter/${names}${args}`;
    }

    url = cleanUrl(url);
    url += url.includes('?') ? '&' : '?';
    // Encode the field variable defensively: dotCMS field variables are constrained to
    // identifier-safe characters today, but encoding guarantees a stray `&`/`=`/`?` could
    // never break out of the value and inject extra query parameters.
    url += `binaryFieldId=${encodeURIComponent(binaryFieldId)}&_imageToolSaveFile=true`;

    if (focalApplied) {
        url += '&overwrite=true';
    }

    return url;
}
