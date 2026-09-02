import { signalStoreFeature, type } from '@ngrx/signals';
import { on, withReducer } from '@ngrx/signals/events';

import { ImageEditorState } from '../../models/image-editor.models';
import { clamp } from '../../utils/dimensions.util';
import { imageEditorToolEvents } from '../image-editor.events';

/**
 * Focal point feature: tracks the normalized 0..1 focal point as editor state.
 *
 * It is intentionally NOT persisted on its own. In dotCMS the focal point is part
 * of the edit→save pipeline — a write goes to a temp staging slot and is only
 * committed to the content during check-in/save (see `FocalPointImageFilter` +
 * `BinaryExporterServlet.copyMetadata`). So the marker just records the point here;
 * the (separate) Save flow will persist it alongside the other edits. The focal
 * point never enters the preview filter chain nor the edit history.
 */
export function withFocalPoint() {
    return signalStoreFeature(
        type<{ state: ImageEditorState }>(),
        withReducer(
            on(imageEditorToolEvents.focalPointSet, ({ payload }, state) => ({
                ...state,
                focalPoint: { x: clamp(payload.x, 0, 1), y: clamp(payload.y, 0, 1) }
            }))
        )
    );
}
