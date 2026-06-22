import { signalStoreFeature, type } from '@ngrx/signals';
import { on, withReducer } from '@ngrx/signals/events';

import { ImageEditorState } from '../../models/image-editor.models';
import { imageEditorToolEvents } from '../image-editor.events';

/**
 * Tools feature: tracks which canvas tool (move / crop / focal) is active. The
 * crop and focal interactions themselves live in {@link withCrop} and
 * {@link withFocalPoint}; this feature only owns the tool selection.
 */
export function withTools() {
    return signalStoreFeature(
        type<{ state: ImageEditorState }>(),
        withReducer(
            on(imageEditorToolEvents.toolSelected, ({ payload }, state) => ({
                ...state,
                activeTool: payload
            }))
        )
    );
}
