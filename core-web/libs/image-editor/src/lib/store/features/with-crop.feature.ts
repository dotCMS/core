import { signalStoreFeature, type } from '@ngrx/signals';
import { on, withReducer } from '@ngrx/signals/events';

import { CropState, ImageEditorState, TransformState } from '../../models/image-editor.models';
import { imageEditorToolEvents } from '../image-editor.events';
import { initialCropState } from '../image-editor.state';
import { coalesceHistory, editableSlicesOf } from '../image-editor.store-utils';

/**
 * Crop feature: applies or cancels a manual crop selection. Applying a crop is
 * mutually exclusive with resize (it clears scale/output dims), returns to the
 * move tool and records a coalesced history entry; cancelling clears the pending
 * selection.
 */
export function withCrop() {
    return signalStoreFeature(
        type<{ state: ImageEditorState }>(),
        withReducer(
            on(imageEditorToolEvents.cropApplied, ({ payload }, state) => {
                const crop: CropState = { ...payload, active: true };
                // Crop and resize are mutually exclusive: applying a crop clears resize.
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
                    ...coalesceHistory(next, 'crop', 'Crop', editableSlicesOf(next))
                };
            }),
            on(imageEditorToolEvents.cropCancelled, (_event, state) => ({
                ...state,
                crop: initialCropState,
                activeTool: 'move' as const
            }))
        )
    );
}
