import { CompressionMode, HandlePosition } from './models/image-editor.models';

/**
 * Library-wide constants for the image editor: control ranges, zoom/crop
 * interaction steps, formatting and behavior tuning. Kept in one place so the
 * components and store share a single source of truth instead of redeclaring
 * magic numbers per file.
 */

/** Inclusive value ranges enforced when clamping panel edits (sliders + inputs). */
export const RANGES = {
    brightness: { min: -100, max: 100 },
    hue: { min: -100, max: 100 },
    saturation: { min: -100, max: 100 },
    scale: { min: 1, max: 400 },
    rotate: { min: -180, max: 180 },
    quality: { min: 0, max: 100 }
} as const;

/** Canvas zoom bounds, step and default (percentages). */
export const ZOOM_MIN = 10;
export const ZOOM_MAX = 400;
export const ZOOM_STEP = 25;
export const ZOOM_DEFAULT = 100;

/** Crop box keyboard nudge in CSS px (Shift uses the larger step). */
export const CROP_NUDGE_STEP = 1;
export const CROP_NUDGE_STEP_LARGE = 10;

/** Smallest allowed crop dimension in CSS px to keep the box usable. */
export const MIN_CROP_SIZE = 16;

/** The eight resize handles rendered around the crop box, in render order. */
export const CROP_HANDLES: readonly HandlePosition[] = [
    'tl',
    't',
    'tr',
    'r',
    'br',
    'b',
    'bl',
    'l'
] as const;

/** One kibibyte, used to format byte counts for display. */
export const BYTES_PER_KB = 1024;

/**
 * Times a failed preview is silently retried before the error UI is shown. The
 * server renders filter chains on the fly, so the first request for a fresh URL
 * can race that generation and return an incomplete response; a couple of silent
 * re-attempts (mirroring a manual refresh) almost always lands the finished image.
 */
export const AUTO_PREVIEW_RETRY_LIMIT = 3;

/** Human-readable labels per compression mode, used in history entries. */
export const COMPRESSION_LABELS: Record<CompressionMode, string> = {
    none: 'None',
    auto: 'Auto',
    jpeg: 'JPEG',
    webp: 'WebP',
    avif: 'AVIF'
};

/** The editable slices, in snapshot order (used to diff/replay history). */
export const SLICE_KEYS = ['adjust', 'transform', 'crop', 'fileInfo'] as const;

/** localStorage key persisting which editor side panels are expanded. */
export const IMAGE_EDITOR_PANEL_STATE_KEY = 'DOT_IMAGE_EDITOR_PANEL_STATE';

/**
 * Inline `.p-dialog` style props applied when the editor goes full-screen and
 * restored on exit. Overrides PrimeNG's `DynamicDialog` size (set inline via
 * `[ngStyle]`), so it must be applied as inline styles to win.
 */
export const FULLSCREEN_DIALOG_STYLE: Record<string, string> = {
    width: '100vw',
    height: '100vh',
    maxWidth: '100vw',
    maxHeight: '100vh',
    borderRadius: '0'
};

/** Eased transition so the dialog grows/shrinks smoothly instead of snapping. */
export const DIALOG_SIZE_TRANSITION =
    'width 250ms ease, height 250ms ease, border-radius 250ms ease';
