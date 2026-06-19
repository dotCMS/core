import { DotCMSTempFile } from '@dotcms/dotcms-models';

import {
    ActiveTool,
    AdjustState,
    CropState,
    FileInfoState,
    FocalPointState,
    ImageEditorAssetContext,
    ImageEditorHistoryEntry,
    PreviewStatus,
    SaveStatus,
    TransformState,
    ZoomState
} from '../models/image-editor.models';

/**
 * The complete, flat state of the image editor. Each slice owns a domain of the
 * editing experience (color adjustment, geometric transform, crop, focal point,
 * file/compression info, zoom) plus the editor lifecycle bookkeeping (active
 * tool, preview/save status, history and a cache-busting counter).
 */
export interface ImageEditorState {
    /** Resolved identifiers and natural dimensions of the asset being edited. */
    assetContext: ImageEditorAssetContext;
    /** Color adjustment slice (brightness/hue/saturation/grayscale). */
    adjust: AdjustState;
    /** Geometric transform slice (scale/rotate/flip/output dimensions). */
    transform: TransformState;
    /** Crop selection slice. */
    crop: CropState;
    /** Compression configuration and resulting/original file sizes. */
    fileInfo: FileInfoState;
    /** Normalized focal point slice. */
    focalPoint: FocalPointState;
    /** Canvas zoom slice. */
    zoom: ZoomState;
    /** Tool currently selected on the canvas. */
    activeTool: ActiveTool;
    /** Loading lifecycle of the preview image. */
    previewStatus: PreviewStatus;
    /** Consecutive silent retries of the current failing preview (reset on success). */
    previewRetries: number;
    /** Lifecycle of the current save / save-as operation. */
    saveStatus: SaveStatus;
    /** Temp file produced by the last successful save, or `null`. */
    savedTempFile: DotCMSTempFile | null;
    /** Last error message surfaced to the user, or `null`. */
    error: string | null;
    /** Ordered undo/redo history of applied edits. */
    history: ImageEditorHistoryEntry[];
    /** Index of the current head entry in {@link history}, or `-1` when empty. */
    historyIndex: number;
    /** Monotonic counter appended to preview URLs to bust the browser cache. */
    cacheBust: number;
}

/** Inclusive value ranges enforced by the reducer when clamping panel edits. */
export const RANGES = {
    brightness: { min: -100, max: 100 },
    hue: { min: -100, max: 100 },
    saturation: { min: -100, max: 100 },
    scale: { min: 1, max: 400 },
    rotate: { min: -180, max: 180 },
    quality: { min: 0, max: 100 }
} as const;

/** Default empty asset context used before an asset is requested. */
const initialAssetContext: ImageEditorAssetContext = {
    idOrTempId: '',
    inode: null,
    tempId: null,
    variable: '',
    fieldName: '',
    fileName: '',
    mimeType: '',
    isTempFile: false,
    byInode: false,
    naturalWidth: 0,
    naturalHeight: 0,
    originalUrl: ''
};

/** Default color adjustment slice: no adjustments, no grayscale. */
export const initialAdjustState: AdjustState = {
    brightness: 0,
    hue: 0,
    saturation: 0,
    grayscale: false
};

/** Default transform slice: no scaling, rotation, flip or output override. */
export const initialTransformState: TransformState = {
    scale: 100,
    rotateDeg: 0,
    flipH: false,
    flipV: false,
    outputWidth: null,
    outputHeight: null,
    lockAspectRatio: true
};

/** Default crop slice: inactive, freeform. */
export const initialCropState: CropState = {
    x: 0,
    y: 0,
    w: 0,
    h: 0,
    active: false,
    aspect: null
};

/** Default file info slice: no compression, quality 85, sizes unknown. */
export const initialFileInfoState: FileInfoState = {
    compression: 'none',
    quality: 85,
    currentBytes: null,
    originalBytes: null
};

/** Default focal point slice: centered and inactive. */
export const initialFocalPointState: FocalPointState = {
    x: 0.5,
    y: 0.5,
    active: false
};

/** Default zoom slice: 100% and fitted to screen. */
export const initialZoomState: ZoomState = {
    level: 100,
    fitToScreen: true
};

/** The pristine state of the editor before any asset is loaded. */
export const initialImageEditorState: ImageEditorState = {
    assetContext: initialAssetContext,
    adjust: initialAdjustState,
    transform: initialTransformState,
    crop: initialCropState,
    fileInfo: initialFileInfoState,
    focalPoint: initialFocalPointState,
    zoom: initialZoomState,
    activeTool: 'move',
    previewStatus: 'idle',
    previewRetries: 0,
    saveStatus: 'idle',
    savedTempFile: null,
    error: null,
    history: [],
    historyIndex: -1,
    cacheBust: 0
};
