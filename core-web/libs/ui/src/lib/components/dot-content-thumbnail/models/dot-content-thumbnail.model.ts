/**
 * Media type resolved for a thumbnail. `pdf` and `image` render the same way
 * (an `<img>` of a server-rendered raster) but are kept distinct so consumers
 * and tests stay expressive. `svg` resolves to the raw vector URL (never a
 * rasterized crop) but renders `object-cover` like every other image type.
 */
export type DotContentThumbnailType = 'image' | 'svg' | 'pdf' | 'video' | 'icon';

/**
 * Loading lifecycle of the thumbnail media. `icon` thumbnails have no async
 * load and start as `loaded`. On `error` the viewer falls back to the icon
 * renderer.
 */
export type DotContentThumbnailState = 'loading' | 'loaded' | 'error';

/**
 * Resolved, render-ready thumbnail model consumed by `dot-content-thumbnail`.
 * Build it with `contentletToThumbnailModel` / `tempFileToThumbnailModel`, or
 * let the component resolve it by passing the `contentlet` input instead.
 */
export interface DotContentThumbnail {
    type: DotContentThumbnailType;
    /**
     * Resolved asset URL. Empty string for `icon`. For non-playable videos it
     * already carries the `#t=0.1` media fragment used to paint the first frame.
     */
    src: string;
    /**
     * Material Symbols glyph name (e.g. `insert_drive_file`). Always populated,
     * on every type, so the viewer can fall back to it when the media errors.
     */
    icon: string;
    alt: string;
    /** Video only: render with controls (true) vs a static first-frame preview. */
    playable?: boolean;
}

/**
 * Options for `contentletToThumbnailModel`. Defaults mirror the legacy Stencil
 * `dot-contentlet-thumbnail` element as used by most call sites.
 */
export interface DotContentletThumbnailOptions {
    /** Binary/image field variable used to build field-scoped `/dA` URLs. */
    fieldVariable?: string;
    /** Render videos as playable (`<video controls>`). Default `false` (first-frame preview). */
    playableVideo?: boolean;
    /** When `false`, videos resolve to the icon fallback instead of a thumbnail. Default `true`. */
    showVideoThumbnail?: boolean;
}
