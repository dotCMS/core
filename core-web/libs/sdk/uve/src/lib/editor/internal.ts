import { DotCMSUVEAction, BlockEditorNode } from '@dotcms/types';
import { BlockEditorState, DotCMSContainerBound } from '@dotcms/types/internal';

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
 * Validates the structure of a Block Editor node.
 *
 * This function performs validation checks on a BlockEditorNode object to ensure:
 * - The node exists and is a valid object
 * - The node has a 'doc' type
 * - The node has a valid content array containing at least one block
 * - Each block in the content array:
 *   - Has a valid string type property
 *   - Has valid object attributes (if present)
 *   - Has valid nested content (if present)
 *
 * @param {BlockEditorNode} blocks - The BlockEditorNode structure to validate
 * @returns {BlockEditorState} The validation result
 * @property {string | null} BlockEditorState.error - Error message if validation fails, null if valid
 */
export const isValidBlocks = (blocks: BlockEditorNode): BlockEditorState => {
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
            error: `Error: Invalid block type. Expected 'doc' but received: '${blocks.type}'`
        };
    }

    if (!blocks.content) {
        return {
            error: 'Error: Blocks content is missing'
        };
    }

    if (!Array.isArray(blocks.content)) {
        return {
            error: `Error: Blocks content must be an array, but received: ${typeof blocks.content}`
        };
    }

    if (blocks.content.length === 0) {
        return {
            error: 'Error: Blocks content is empty. At least one block is required.'
        };
    }

    // Validate each block in the content array
    for (let i = 0; i < blocks.content.length; i++) {
        const block = blocks.content[i];
        if (!block.type) {
            return {
                error: `Error: Block at index ${i} is missing required 'type' property`
            };
        }

        if (typeof block.type !== 'string') {
            return {
                error: `Error: Block type at index ${i} must be a string, but received: ${typeof block.type}`
            };
        }

        // Validate block attributes if present
        if (block.attrs && typeof block.attrs !== 'object') {
            return {
                error: `Error: Block attributes at index ${i} must be an object, but received: ${typeof block.attrs}`
            };
        }

        // Validate nested content if present
        if (block.content) {
            if (!Array.isArray(block.content)) {
                return {
                    error: `Error: Block content at index ${i} must be an array, but received: ${typeof block.content}`
                };
            }

            // Recursively validate nested blocks
            const nestedValidation = isValidBlocks({
                type: 'doc',
                content: block.content
            });
            if (nestedValidation.error) {
                return {
                    error: `Error in nested block at index ${i}: ${nestedValidation.error}`
                };
            }
        }
    }

    return {
        error: null
    };
};
