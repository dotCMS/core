import { signalStoreFeature, type } from '@ngrx/signals';
import { on, withReducer } from '@ngrx/signals/events';

import {
    FocalPointState,
    ImageEditorState,
    TransformState
} from '../../models/image-editor.models';
import { imageEditorToolEvents } from '../image-editor.events';
import { initialFocalPointState } from '../image-editor.state';
import { coalesceHistory, editableSlicesOf, focalCenteredCrop } from '../image-editor.store-utils';

/**
 * Focal point feature: setting/clearing the focal anchor and the focal-centered
 * aspect crop. Setting the point does NOT reload the preview (it's a save-time
 * anchor); the aspect crop derives a crop centered on the point and, like a manual
 * crop, supersedes resize and returns to the move tool.
 */
export function withFocalPoint() {
    return signalStoreFeature(
        type<{ state: ImageEditorState }>(),
        withReducer(
            on(imageEditorToolEvents.focalPointSet, ({ payload }, state) => {
                // No preview reload: the focal point doesn't change the rendered image
                // on its own — it's a saved anchor consumed by the aspect crop and
                // persisted on save. Just record it (and a coalesced history entry).
                const focalPoint: FocalPointState = { x: payload.x, y: payload.y, active: true };
                const next: ImageEditorState = { ...state, focalPoint };

                return {
                    ...next,
                    ...coalesceHistory(
                        next,
                        'focal',
                        `Focal point ${payload.x.toFixed(2)}, ${payload.y.toFixed(2)}`,
                        editableSlicesOf(next)
                    )
                };
            }),
            on(imageEditorToolEvents.focalPointCleared, (_event, state) => ({
                ...state,
                focalPoint: initialFocalPointState
            })),
            on(imageEditorToolEvents.aspectCropApplied, ({ payload }, state) => {
                if (!state.assetContext.naturalWidth || !state.assetContext.naturalHeight) {
                    return state;
                }

                const crop = focalCenteredCrop(payload.aspect, state);
                // Cropping is mutually exclusive with resize, and returns to the move tool.
                const transform: TransformState = {
                    ...state.transform,
                    scale: 100,
                    outputWidth: null,
                    outputHeight: null
                };
                const next: ImageEditorState = {
                    ...state,
                    crop,
                    transform,
                    activeTool: 'move',
                    previewStatus: 'loading',
                    cacheBust: state.cacheBust + 1
                };

                return {
                    ...next,
                    ...coalesceHistory(
                        next,
                        'crop',
                        `Crop ${payload.label}`,
                        editableSlicesOf(next)
                    )
                };
            })
        )
    );
}
