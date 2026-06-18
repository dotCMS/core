import {
    AdjustState,
    AppliedFilter,
    CompressionMode,
    CropState,
    FileInfoState,
    FocalPointState,
    ImageEditorAssetContext,
    TransformState
} from '../models/image-editor.models';

/** State slices required to build the server filter chain. */
interface FilterChainInput {
    adjust: AdjustState;
    transform: TransformState;
    crop: CropState;
    fileInfo: FileInfoState;
    focalPoint: FocalPointState;
}

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
 * compression filter is always applied last and exclusively.
 * @param input - The adjust/transform/crop/fileInfo/focalPoint state slices
 * @returns The applied filters in the exact order they must be concatenated
 */
export function buildFilterChain(input: FilterChainInput): AppliedFilter[] {
    const { adjust, transform, crop, fileInfo, focalPoint } = input;
    const filters: AppliedFilter[] = [];

    const isResizing =
        transform.outputWidth != null || transform.outputHeight != null || transform.scale !== 100;

    if (isResizing) {
        let args = '';
        if (transform.outputWidth != null) {
            args += `/resize_w/${Math.round(transform.outputWidth)}`;
        }
        if (transform.outputHeight != null) {
            args += `/resize_h/${Math.round(transform.outputHeight)}`;
        }
        if (args) {
            filters.push({ name: 'Resize', args });
        }
    } else if (crop.active && crop.w > 0 && crop.h > 0) {
        const args =
            `/crop_w/${Math.round(crop.w)}` +
            `/crop_h/${Math.round(crop.h)}` +
            `/crop_x/${Math.round(crop.x)}` +
            `/crop_y/${Math.round(crop.y)}`;
        filters.push({ name: 'Crop', args });
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

    if (focalPoint.active && !(focalPoint.x === 0.5 && focalPoint.y === 0.5)) {
        filters.push({ name: 'FocalPoint', args: `/fp/${focalPoint.x},${focalPoint.y}` });
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
