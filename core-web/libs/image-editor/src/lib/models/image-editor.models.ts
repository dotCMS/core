import { Observable } from 'rxjs';

import { DotCMSTempFile } from '@dotcms/dotcms-models';

/**
 * Tool currently selected on the editing canvas.
 * - `move`: pan/zoom the image
 * - `crop`: draw a crop selection
 * - `focal`: place the focal point
 */
export type ActiveTool = 'move' | 'crop' | 'focal';

/** Output compression strategy applied as the last filter in the chain. */
export type CompressionMode = 'none' | 'auto' | 'jpeg' | 'webp';

/** Loading lifecycle of the preview image. */
export type PreviewStatus = 'idle' | 'loading' | 'loaded' | 'error';

/** Lifecycle of a save / save-as operation. */
export type SaveStatus = 'idle' | 'saving' | 'saved' | 'error';

/** Logical category an applied edit belongs to, used for grouping and labels. */
export type FilterCategory =
    | 'adjust'
    | 'crop'
    | 'rotate'
    | 'flip'
    | 'grayscale'
    | 'compression'
    | 'focal';

/** Server-side filter name as understood by the dotCMS image filter endpoint. */
export type FilterName =
    | 'Resize'
    | 'Crop'
    | 'Rotate'
    | 'Flip'
    | 'Grayscale'
    | 'Hsb'
    | 'FocalPoint'
    | 'Jpeg'
    | 'WebP'
    | 'Quality';

/**
 * Resolved information about the asset being edited, including the source
 * identifiers and the natural pixel dimensions of the original image.
 */
export interface ImageEditorAssetContext {
    /** Inode when editing an existing asset, otherwise the temp file id. */
    idOrTempId: string;
    /** Inode of the persisted asset, or `null` when editing a temp file. */
    inode: string | null;
    /** Temp file id when editing an uploaded file, otherwise `null`. */
    tempId: string | null;
    /** Content type variable that owns the binary field. */
    variable: string;
    /** Binary field name being edited. */
    fieldName: string;
    /** Original file name of the asset. */
    fileName: string;
    /** MIME type of the source asset. */
    mimeType: string;
    /** Whether the asset is backed by a temporary upload. */
    isTempFile: boolean;
    /** Whether the asset should be addressed by inode in built URLs. */
    byInode: boolean;
    /** Intrinsic width of the original image in pixels. */
    naturalWidth: number;
    /** Intrinsic height of the original image in pixels. */
    naturalHeight: number;
    /** Base URL of the unfiltered original asset. */
    originalUrl: string;
}

/** Color adjustment values. Hue, saturation and brightness range -100..100. */
export interface AdjustState {
    brightness: number;
    hue: number;
    saturation: number;
    grayscale: boolean;
}

/** Geometric transform values applied to the image. */
export interface TransformState {
    /** Zoom percentage where 100 means no scaling. */
    scale: number;
    /** Rotation in degrees. */
    rotateDeg: number;
    /** Horizontal flip toggle. */
    flipH: boolean;
    /** Vertical flip toggle. */
    flipV: boolean;
    /** Explicit output width in pixels, or `null` to derive it. */
    outputWidth: number | null;
    /** Explicit output height in pixels, or `null` to derive it. */
    outputHeight: number | null;
    /** Whether width/height stay proportional when one is changed. */
    lockAspectRatio: boolean;
}

/** Crop selection in source pixels. `active` gates whether it is applied. */
export interface CropState {
    x: number;
    y: number;
    w: number;
    h: number;
    active: boolean;
    /** Locked aspect ratio (width / height) or `null` for freeform. */
    aspect: number | null;
}

/** Focal point as normalized 0..1 coordinates. `active` gates application. */
export interface FocalPointState {
    x: number;
    y: number;
    active: boolean;
}

/** Compression configuration and the resulting/original file sizes in bytes. */
export interface FileInfoState {
    compression: CompressionMode;
    /** Compression quality 0..100. */
    quality: number;
    /** Current preview size in bytes, or `null` when unknown. */
    currentBytes: number | null;
    /** Original asset size in bytes, or `null` when unknown. */
    originalBytes: number | null;
}

/** Canvas zoom state. */
export interface ZoomState {
    /** Zoom level as a multiplier where 1 is 100%. */
    level: number;
    /** Whether the image is auto-fitted to the viewport. */
    fitToScreen: boolean;
}

/** A single server filter and its argument string, ready to be concatenated. */
export interface AppliedFilter {
    name: FilterName;
    args: string;
}

/** A user-facing entry in the applied-edits list. */
export interface AppliedEditEntry {
    id: string;
    category: FilterCategory;
    label: string;
}

/**
 * Snapshot of the editable state at a point in history, used to power
 * undo/redo and coalescing of rapid consecutive edits in the same category.
 */
export interface ImageEditorHistoryEntry {
    id: string;
    category: FilterCategory;
    label: string;
    /** State slices captured when this entry was created. */
    snapshot: {
        adjust: AdjustState;
        transform: TransformState;
        crop: CropState;
        focalPoint: FocalPointState;
        fileInfo: FileInfoState;
    };
}

/** Parameters used to open the editor for a given asset. */
export interface ImageEditorOpenParams {
    inode?: string;
    tempId?: string;
    variable: string;
    fieldName: string;
    byInode?: boolean;
    fileName?: string;
    mimeType?: string;
}

/**
 * Public contract for the service that launches the image editor.
 * Implementations decide availability (e.g. behind a feature flag) and how
 * the editor surfaces its result.
 */
export interface DotImageEditorLauncher {
    /** Whether the editor can be launched in the current environment. */
    isAvailable(): boolean;
    /**
     * Opens the editor for the given asset.
     * @param params - Identifiers and metadata of the asset to edit
     * @returns Emits the saved temp file, or `null` if the user cancelled
     */
    open(params: ImageEditorOpenParams): Observable<DotCMSTempFile | null>;
}
