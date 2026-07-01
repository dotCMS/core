import { signalStore, withState } from '@ngrx/signals';

import {
    withAdjust,
    withAsset,
    withCrop,
    withDownload,
    withFileInfo,
    withFocalPoint,
    withHistory,
    withPreview,
    withTransform,
    withView
} from './features';
import { initialImageEditorState } from './image-editor.state';

/**
 * NgRx SignalStore for the image editor, composed from one vertical feature per
 * area of functionality. Each feature
 * bundles its own reducers, derived selectors and effects, so a domain lives in a
 * single place (`features/with-*.feature.ts`).
 *
 * Order matters only where features consume each other's selectors: `withPreview`
 * derives `previewUrl`, which `withDownload` reads — so download composes after
 * preview. The store is NOT provided in root; the editor dialog supplies it so
 * each editor instance is isolated.
 */
export const ImageEditorStore = signalStore(
    withState(initialImageEditorState),
    withAdjust(),
    withTransform(),
    withCrop(),
    withFileInfo(),
    withFocalPoint(),
    withView(),
    withHistory(),
    withAsset(),
    withPreview(),
    withDownload()
);
