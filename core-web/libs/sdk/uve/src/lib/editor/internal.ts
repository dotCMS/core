import { Block, BlockEditorState, DotCMSContainerBound } from '@dotcms/uve/internal';
import { DotCMSUVEAction } from '@dotcms/uve/types';

import { sendMessageToUVE } from './public';

/**
 * Sets the bounds of the containers in the editor.
 * Retrieves the containers from the DOM and sends their position data to the editor.
 * @private
 * @memberof DotCMSPageEditor
 */
export function setBounds(bounds: DotCMSContainerBound[]): void {
    sendMessageToUVE({
        action: DotCMSUVEAction.SET_BOUNDS,
        payload: bounds
    });
}

/**
 * Validates the structure of a Block Editor block.
 *
 * This function checks that:
 * 1. The blocks parameter is a valid object
 * 2. The block has a 'doc' type
 * 3. The block has a valid content array that is not empty
 *
 * @param {Block} blocks - The blocks structure to validate
 * @returns {BlockEditorState} Object containing validation state and any error message
 * @property {boolean} BlockEditorState.isValid - Whether the blocks structure is valid
 * @property {string | null} BlockEditorState.error - Error message if invalid, null if valid
 */
export const isValidBlocks = (blocks: Block): BlockEditorState => {
    if (!blocks) {
        return {
            error: `Error: Blocks object is not defined`
        };
    }

    if (typeof blocks !== 'object') {
        return {
            error: `Error: Blocks must be an object, but received: ${typeof blocks}`
        };
    }

    if (blocks.type !== 'doc') {
        return {
            error: 'Error: Blocks must have a doc type'
        };
    }

    if (!blocks.content || !Array.isArray(blocks.content)) {
        return {
            error: 'Error: Blocks must have a valid content array'
        };
    }

    if (blocks.content.length === 0) {
        return {
            error: 'Error: Blocks content is empty'
        };
    }

    return {
        error: null
    };
};
