import { Node } from 'prosemirror-model';

import { Editor } from '@tiptap/core';

/**
 * Gets the deepest block-level node at the cursor position in the editor.
 *
 * This function finds the deepest block-level container node that contains the cursor.
 * If the cursor is in text, it returns the block that contains that text (e.g., paragraph).
 * If the cursor is on a block-level leaf node (e.g., image), it returns that node.
 *
 * @param editor - The TipTap editor instance
 * @returns The node type as a string. For block nodes, returns the node type name.
 *
 * @example
 * ```typescript
 * const blockType = getCurrentLeafBlock(editor);
 * // In text: returns "paragraph", "tableCell", "listItem", etc.
 * // On image: returns "dotImage", "image", etc.
 * ```
 */
export const getCurrentLeafBlock = (editor: Editor) => {
    const state = editor.view.state;
    const { selection } = state;
    const { anchor } = selection;

    let name = '';
    let parentNode = null;

    state.doc.descendants((node, pos, parent) => {
        const hasAnchor = anchor >= pos && anchor <= pos + node.nodeSize;

        if (hasAnchor && node.isLeaf) {
            // If the leaf node is text, get the parent block node instead
            if (node.type.name === 'text' && parent) {
                name = parent.type.name;
                parentNode = parent;
            } else {
                name = node.type.name;
            }

            return false;
        }

        return true;
    });

    if (parentNode?.type.name === 'heading') {
        return getNodeTypeWithLevel(parentNode);
    }

    return name;
};

/**
 * Formats a heading node type with its level attribute.
 *
 * This helper function takes a heading node and returns a string that includes
 * the heading level. If the node has a level attribute, it appends the level
 * to the base node type name.
 *
 * @param node - The ProseMirror node (should be a heading node)
 * @returns The formatted node type string. For headings with level, returns "heading-{level}",
 *          otherwise returns the base node type name.
 *
 * @example
 * ```typescript
 * const headingNode = // ... a heading node with level: 2
 * const formattedType = getNodeTypeWithLevel(headingNode);
 * // Returns: "heading-2"
 * ```
 */
const getNodeTypeWithLevel = (node: Node): string => {
    const hasLevelAttribute = node.attrs.level;
    const baseNodeType = node.type.name;

    return hasLevelAttribute ? `${baseNodeType}-${node.attrs.level}` : baseNodeType;
};
