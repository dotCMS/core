import { Node } from 'prosemirror-model';

import { Editor } from '@tiptap/core';

/**
 * Gets the current node type at the cursor position in the editor.
 *
 * This function analyzes the current selection in the editor and determines
 * what type of node the cursor is positioned on. It handles various scenarios:
 * - Block-level nodes (paragraphs, headings, etc.)
 * - Text nodes within block elements
 * - List items and their parent list types
 * - Headings with their level information
 *
 * @param editor - The TipTap editor instance
 * @returns The node type as a string. For headings, returns format "heading-{level}" (e.g., "heading-1", "heading-2")
 *
 * @example
 * ```typescript
 * const nodeType = getCurrentNodeType(editor);
 * // Returns: "paragraph", "heading-1", "bulletList", "orderedList", etc.
 * ```
 */
export const getCurrentNodeType = (editor: Editor) => {
    const state = editor.view.state;
    const from = state.selection.from;
    const pos = state.doc.resolve(from);

    // First, try to get the node at the current position
    const nodeAtPos = state.doc.nodeAt(from);
    if (nodeAtPos) {
        const nodeType = nodeAtPos.type.name;

        // If it's a heading, return with level
        if (nodeType === 'heading') {
            return getNodeTypeWithLevel(nodeAtPos);
        }

        // If it's a block-level node, return it directly
        if (nodeType !== 'text' && nodeType !== 'doc') {
            return nodeType;
        }
    }

    // Fallback to the original logic for text nodes
    const currentNode = pos.node(pos.depth);
    const parentNode = pos.node(pos.depth - 1);
    const parentType = parentNode?.type?.name;
    const currentNodeType = currentNode.type.name;

    if (parentType === 'listItem') {
        const listType = pos.node(pos.depth - 2);

        return listType.type.name;
    }

    if (currentNodeType === 'heading') {
        return getNodeTypeWithLevel(currentNode);
    }

    return parentType === 'doc' ? currentNodeType : parentType;
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
