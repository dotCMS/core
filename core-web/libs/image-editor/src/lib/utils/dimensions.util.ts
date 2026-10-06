import {
    CropState,
    Dimensions,
    ImageEditorAssetContext,
    TransformState
} from '../models/image-editor.models';

/**
 * Constrains a number to the inclusive `[min, max]` range.
 * @param v - The value to clamp
 * @param min - The lower bound
 * @param max - The upper bound
 * @returns `v` clamped to `[min, max]`
 */
export function clamp(v: number, min: number, max: number): number {
    return Math.min(max, Math.max(min, v));
}

/**
 * Resolves the resize width/height to request from the server based on the
 * transform state, falling back to scaling the natural dimensions when no
 * explicit output size is set.
 * @param transform - The current transform state
 * @param natural - The intrinsic dimensions of the source image
 * @returns The width and height to resize to, each `null` when not constrained
 */
export function computeResizeParams(
    transform: TransformState,
    natural: Dimensions
): { width: number | null; height: number | null } {
    if (transform.outputWidth != null || transform.outputHeight != null) {
        return {
            width: transform.outputWidth != null ? Math.round(transform.outputWidth) : null,
            height: transform.outputHeight != null ? Math.round(transform.outputHeight) : null
        };
    }

    if (transform.scale !== 100) {
        const factor = transform.scale / 100;
        return {
            width: Math.round(natural.width * factor),
            height: Math.round(natural.height * factor)
        };
    }

    return { width: null, height: null };
}

/**
 * Computes the effective output dimensions of the edited image, accounting for
 * an active crop and any resize/scale in the transform state.
 * @param ctx - The asset context (provides the natural dimensions)
 * @param transform - The current transform state
 * @param crop - The current crop state
 * @returns The resulting width and height in pixels
 */
export function computeOutputDimensions(
    ctx: ImageEditorAssetContext,
    transform: TransformState,
    crop: CropState
): Dimensions {
    let width = ctx.naturalWidth;
    let height = ctx.naturalHeight;

    // A crop runs last in the chain (after any resize), so when one is active it
    // dictates the final size — its w/h are already in the scaled image's space.
    if (crop.active && crop.w > 0 && crop.h > 0) {
        return { width: Math.round(crop.w), height: Math.round(crop.h) };
    }

    if (transform.outputWidth != null || transform.outputHeight != null) {
        const aspect = height === 0 ? 1 : width / height;
        if (transform.outputWidth != null && transform.outputHeight != null) {
            width = Math.round(transform.outputWidth);
            height = Math.round(transform.outputHeight);
        } else if (transform.outputWidth != null) {
            width = Math.round(transform.outputWidth);
            height = Math.round(width / aspect);
        } else if (transform.outputHeight != null) {
            height = Math.round(transform.outputHeight);
            width = Math.round(height * aspect);
        }
    } else if (transform.scale !== 100) {
        const factor = transform.scale / 100;
        width = Math.round(width * factor);
        height = Math.round(height * factor);
    }

    return { width, height };
}
