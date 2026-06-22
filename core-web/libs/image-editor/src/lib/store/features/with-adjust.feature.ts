import { signalStoreFeature, type } from '@ngrx/signals';
import { on, withReducer } from '@ngrx/signals/events';

import { RANGES } from '../../image-editor.constants';
import { AdjustState, ImageEditorState } from '../../models/image-editor.models';
import { clamp } from '../../utils/dimensions.util';
import { imageEditorAdjustEvents } from '../image-editor.events';
import { adjustPatch } from '../image-editor.store-utils';

/**
 * Adjust feature: color & light. Folds the brightness / hue / saturation sliders
 * and the grayscale toggle into the `adjust` slice (clamped to {@link RANGES}),
 * each producing a coalesced history entry via {@link adjustPatch}.
 */
export function withAdjust() {
    return signalStoreFeature(
        type<{ state: ImageEditorState }>(),
        withReducer(
            on(imageEditorAdjustEvents.brightnessChanged, ({ payload }, state) => {
                const value = clamp(payload, RANGES.brightness.min, RANGES.brightness.max);
                const adjust: AdjustState = { ...state.adjust, brightness: value };

                return adjustPatch(state, adjust, 'adjust', `Brightness ${value}`);
            }),
            on(imageEditorAdjustEvents.hueChanged, ({ payload }, state) => {
                const value = clamp(payload, RANGES.hue.min, RANGES.hue.max);
                const adjust: AdjustState = { ...state.adjust, hue: value };

                return adjustPatch(state, adjust, 'adjust', `Hue ${value}`);
            }),
            on(imageEditorAdjustEvents.saturationChanged, ({ payload }, state) => {
                const value = clamp(payload, RANGES.saturation.min, RANGES.saturation.max);
                const adjust: AdjustState = { ...state.adjust, saturation: value };

                return adjustPatch(state, adjust, 'adjust', `Saturation ${value}`);
            }),
            on(imageEditorAdjustEvents.grayscaleToggled, ({ payload }, state) => {
                const adjust: AdjustState = { ...state.adjust, grayscale: payload };

                return adjustPatch(
                    state,
                    adjust,
                    'grayscale',
                    `Grayscale ${payload ? 'on' : 'off'}`
                );
            })
        )
    );
}
