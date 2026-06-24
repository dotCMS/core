import { signalStoreFeature, type } from '@ngrx/signals';
import { on, withReducer } from '@ngrx/signals/events';

import { ImageEditorState } from '../../models/image-editor.models';
import { imageEditorToolEvents, imageEditorViewEvents } from '../image-editor.events';

/**
 * View feature: the editor's transient view/UI state, as opposed to the edit
 * slices that feed the preview URL and history. It owns which canvas tool is
 * active (move / crop) and whether the dialog is full-screen. The crop
 * interaction itself lives in {@link withCrop}; resizing the dialog to
 * full-screen is the root component's job — this feature only owns the
 * `isFullscreen` flag it reads.
 */
export function withView() {
    return signalStoreFeature(
        type<{ state: ImageEditorState }>(),
        withReducer(
            on(imageEditorToolEvents.toolSelected, ({ payload }, state) => ({
                ...state,
                activeTool: payload
            })),
            on(imageEditorViewEvents.fullscreenToggled, (_event, state) => ({
                ...state,
                isFullscreen: !state.isFullscreen
            }))
        )
    );
}
