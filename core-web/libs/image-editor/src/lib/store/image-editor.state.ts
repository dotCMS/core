import {
    AdjustState,
    CropState,
    FileInfoState,
    FocalPointState,
    ImageEditorAssetContext,
    ImageEditorState,
    TransformState,
    ZoomState
} from '../models/image-editor.models';

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
    error: null,
    history: [],
    historyIndex: -1,
    cacheBust: 0,
    isFullscreen: false
};
