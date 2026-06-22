import { signalStoreFeature, type, withComputed } from '@ngrx/signals';
import { on, withReducer } from '@ngrx/signals/events';

import { computed } from '@angular/core';

import { RANGES } from '../../image-editor.constants';
import { ImageEditorState, TransformState } from '../../models/image-editor.models';
import { clamp, computeOutputDimensions } from '../../utils/dimensions.util';
import { imageEditorTransformEvents } from '../image-editor.events';
import { initialCropState } from '../image-editor.state';
import { transformPatch } from '../image-editor.store-utils';

/**
 * Transform feature: scale, rotate, flip and explicit output size. Folds the
 * transform controls into the `transform` slice (resizing supersedes crop,
 * mirroring the filter-chain rule) and exposes the effective `outputDimensions`
 * derived from the transform + crop + natural size.
 */
export function withTransform() {
    return signalStoreFeature(
        type<{ state: ImageEditorState }>(),
        withReducer(
            on(imageEditorTransformEvents.scaleChanged, ({ payload }, state) => {
                const value = clamp(payload, RANGES.scale.min, RANGES.scale.max);
                // Resizing supersedes crop, mirroring the filter-chain rule.
                const transform: TransformState = { ...state.transform, scale: value };
                const crop = value !== 100 ? initialCropState : state.crop;

                return transformPatch(state, transform, crop, 'adjust', `Scale ${value}%`);
            }),
            on(imageEditorTransformEvents.rotateChanged, ({ payload }, state) => {
                const value = clamp(payload, RANGES.rotate.min, RANGES.rotate.max);
                const transform: TransformState = { ...state.transform, rotateDeg: value };

                return transformPatch(state, transform, state.crop, 'rotate', `Rotate ${value}°`);
            }),
            on(imageEditorTransformEvents.flipHToggled, (_event, state) => {
                const transform: TransformState = {
                    ...state.transform,
                    flipH: !state.transform.flipH
                };

                return transformPatch(state, transform, state.crop, 'flip', 'Flip horizontal');
            }),
            on(imageEditorTransformEvents.flipVToggled, (_event, state) => {
                const transform: TransformState = {
                    ...state.transform,
                    flipV: !state.transform.flipV
                };

                return transformPatch(state, transform, state.crop, 'flip', 'Flip vertical');
            }),
            on(imageEditorTransformEvents.outputDimsChanged, ({ payload }, state) => {
                const transform: TransformState = {
                    ...state.transform,
                    outputWidth: payload.width,
                    outputHeight: payload.height
                };
                const isResizing = payload.width != null || payload.height != null;
                const crop = isResizing ? initialCropState : state.crop;

                return transformPatch(state, transform, crop, 'adjust', 'Resize');
            })
        ),
        withComputed((store) => ({
            /** The effective output dimensions of the edited image. */
            outputDimensions: computed(() =>
                computeOutputDimensions(store.assetContext(), store.transform(), store.crop())
            )
        }))
    );
}
