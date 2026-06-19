import { type } from '@ngrx/signals';
import { eventGroup } from '@ngrx/signals/events';

import { DotCMSTempFile } from '@dotcms/dotcms-models';

import {
    ActiveTool,
    CompressionMode,
    CropState,
    FocalPointState,
    ImageEditorOpenParams
} from '../models/image-editor.models';

/** Events emitted by the Adjust panel (color & light). */
export const imageEditorAdjustEvents = eventGroup({
    source: 'Image Editor Adjust',
    events: {
        brightnessChanged: type<number>(),
        hueChanged: type<number>(),
        saturationChanged: type<number>(),
        grayscaleToggled: type<boolean>()
    }
});

/** Events emitted by the Transform panel (scale, rotate, flip, output size). */
export const imageEditorTransformEvents = eventGroup({
    source: 'Image Editor Transform',
    events: {
        scaleChanged: type<number>(),
        rotateChanged: type<number>(),
        flipHToggled: type<void>(),
        flipVToggled: type<void>(),
        outputDimsChanged: type<{ width: number | null; height: number | null }>()
    }
});

/** Events emitted by the File info panel (compression & quality). */
export const imageEditorFileInfoEvents = eventGroup({
    source: 'Image Editor File Info',
    events: {
        compressionChanged: type<CompressionMode>(),
        qualityChanged: type<number>()
    }
});

/** Events emitted by the canvas tools (move/crop/focal). */
export const imageEditorToolEvents = eventGroup({
    source: 'Image Editor Tool',
    events: {
        toolSelected: type<ActiveTool>(),
        cropApplied: type<CropState>(),
        cropCancelled: type<void>(),
        focalPointSet: type<{ x: number; y: number }>(),
        focalPointCleared: type<void>(),
        // A crop to the given aspect ratio, centered on the current focal point.
        aspectCropApplied: type<{ aspect: number; label: string }>()
    }
});

/** Events emitted by the applied-edits / undo-redo history panel. */
export const imageEditorHistoryEvents = eventGroup({
    source: 'Image Editor History',
    events: {
        editRemoved: type<{ id: string }>(),
        undoRequested: type<void>(),
        redoRequested: type<void>(),
        resetRequested: type<void>()
    }
});

/** Events covering the editor lifecycle: load, preview, download and save. */
export const imageEditorLifecycleEvents = eventGroup({
    source: 'Image Editor Lifecycle',
    events: {
        assetRequested: type<ImageEditorOpenParams>(),
        previewLoaded: type<void>(),
        previewErrored: type<void>(),
        retryRequested: type<void>(),
        downloadRequested: type<void>(),
        saveRequested: type<void>(),
        saveAsRequested: type<{ fileName: string }>(),
        assetLoaded: type<{
            naturalWidth: number;
            naturalHeight: number;
            originalBytes: number | null;
            focalPoint?: FocalPointState;
        }>(),
        assetLoadFailed: type<unknown>(),
        previewSizeResolved: type<number>(),
        saveSucceeded: type<DotCMSTempFile>(),
        saveFailed: type<unknown>()
    }
});
