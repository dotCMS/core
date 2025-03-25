import { DotCMSContainerBound } from '@dotcms/uve/internal';
import { DotCMSUVEAction } from '@dotcms/uve/types';

import { sendMessageToEditor } from './public';

/**
 * Sets the bounds of the containers in the editor.
 * Retrieves the containers from the DOM and sends their position data to the editor.
 * @private
 * @memberof DotCMSPageEditor
 */
export function setBounds(bounds: DotCMSContainerBound[]): void {
    sendMessageToEditor({
        action: DotCMSUVEAction.SET_BOUNDS,
        payload: bounds
    });
}
