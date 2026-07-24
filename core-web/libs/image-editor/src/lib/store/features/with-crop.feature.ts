import { signalStoreFeature, type } from '@ngrx/signals';
import { on, withReducer } from '@ngrx/signals/events';

import { CropState, ImageEditorState } from '../../models/image-editor.models';
import { imageEditorToolEvents } from '../image-editor.events';
import { initialCropState } from '../image-editor.state';
import { coalesceHistory, editableSlicesOf } from '../image-editor.store-utils';

/**
 * Crop feature: applies or cancels a manual crop selection. Applying a crop
 * returns to the move tool and records a coalesced history entry; cancelling
 * clears the pending selection.
 *
 * Crop / resize interplay mirrors the legacy editor, which is intentionally
 * ASYMMETRIC: applying a resize clears any crop (handled in `withTransform`),
 * but applying a crop KEEPS the resize — the crop box is drawn on the already
 * scaled preview, so its pixels are in the scaled image's space and the server
 * runs `Resize` then `Crop` (see `buildFilterChain`). Clearing the scale here
 * would drop the resize and re-apply the crop against the full-resolution
 * original, cutting the wrong region.
 */
export function withCrop() {
    return signalStoreFeature(
        type<{ state: ImageEditorState }>(),
        withReducer(
            on(imageEditorToolEvents.cropApplied, ({ payload }, state) => {
                const crop: CropState = { ...payload, active: true };
                const next: ImageEditorState = {
                    ...state,
                    crop,
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
