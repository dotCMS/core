import { ResolvedPos, Node } from 'prosemirror-model';
import { SelectionRange, TextSelection } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { Editor } from '@tiptap/core';

import { CustomNodeTypes, NodeTypes } from './constants.utils';

import { toJSONFn } from '../../NodeViewRenderer';

const aTagRex = new RegExp(/<a(|\s+[^>]*)>(\s|\n|<img[^>]*src="[^"]*"[^>]*>)*?<\/a>/gm);
const imgTagRex = new RegExp(/<img[^>]*src="[^"]*"[^>]*>/gm);

export interface DotTiptapNodeInformation {
    node: Node;
    from: number;
    to: number;
}

/**
 * Set Custom JSON for this type of Node
 * For this JSON we are going to only add the `contentlet` identifier to the backend
 *
 * @param {*} this
 * @return {*}
 */
export const contentletToJSON: toJSONFn = function () {
    const { attrs, type } = this?.node || {}; // Add null check for this.node
    const { data } = attrs;

    const formattedData = data
        ? {
              identifier: data?.identifier,
              languageId: data?.languageId
          }
        : {};

    const customAttrs = {
        ...attrs,
        data: formattedData
    };

    return {
        type: type.name,
        attrs: customAttrs
    };
};

/**
 * Get the parent node of the ResolvedPos sent
 * @param selectionStart ResolvedPos
 * @param NodesTypesToFind NodeTypes
 */
export const findParentNode = (
    selectionStart: ResolvedPos,
    NodesTypesToFind?: Array<NodeTypes>
) => {
    let depth = selectionStart.depth;
    let parent;
    do {
        parent = selectionStart.node(depth);
        if (parent) {
            if (Array.isArray(NodesTypesToFind) && NodesTypesToFind.includes(parent.type.name)) {
                break;
            }

            depth--;
        }
    } while (depth > 0 && parent);

    return parent;
};

/**
 * This logic was made byt the creator of ProseMirror.
 * See the full comment here: https://github.com/ProseMirror/prosemirror/issues/505#issuecomment-266131409
 *
 * @param {ResolvedPos} selectionStart
 * @param {number} pos
 * @return {*}
 */
export const textNodeRange = (selectionStart: ResolvedPos, pos: number) => {
    const to = pos - selectionStart?.textOffset;
    const from =
        selectionStart.index() < selectionStart?.parent.childCount
            ? to + selectionStart?.parent.child(selectionStart.index()).nodeSize
            : to;

    return { to, from };
};

/**
 *
 * Get the block editor click position based on mouse event.
 * @param {EditorView} view
 * @param {MouseEvent} event
 * @return {*}
 */
export const getPosAtDocCoords = (view: EditorView, event: MouseEvent) => {
    const { clientX: left, clientY: top } = event;
    const { pos } = view.posAtCoords({ left, top });

    return pos;
};

/**
 * Resolve the correct method to remove a node.
 *
 * @param {*} { editor, nodeType, selectionRange }
 */
export const deleteByNode = ({ editor, nodeType, selectionRange }) => {
    if (CustomNodeTypes.includes(nodeType)) {
        deleteSelectedCustomNodeType(editor, selectionRange);
    } else {
        deleteSelectionNode(editor, selectionRange);
    }
};

/**
 * Delete a node given a selection range.
 *
 * @param {Editor} editor
 * @param {SelectionRange} selectionRange
 */
export const deleteByRange = (editor: Editor, selectionRange: SelectionRange) => {
    const from = selectionRange.$from.pos;
    const to = selectionRange.$to.pos + 1;
    editor.chain().deleteRange({ from, to }).blur().run();
};

/**
 *  Delete Custom Node.
 *
 * @param {Editor} editor
 * @param {SelectionRange} selectionRange
 */
export const deleteSelectedCustomNodeType = (editor: Editor, selectionRange: SelectionRange) => {
    const from = selectionRange.$from.pos;
    const to = from + 1;

    // TODO: Try to make the `deleteNode` command works with custom nodes.
    editor.chain().deleteRange({ from, to }).blur().run();
};

/**
 * Delete node given a selection. This method also works with nested lists.
 *
 * @param {Editor} editor
 * @param {SelectionRange} selectionRange
 */
export const deleteSelectionNode = (editor: Editor, selectionRange: SelectionRange) => {
    const selectionParentNode = findParentNode(selectionRange.$from);
    const nodeSelectionNodeType: NodeTypes = selectionParentNode.type.name;

    const closestOrderedOrBulletNode = findParentNode(selectionRange.$from, [
        NodeTypes.ORDERED_LIST,
        NodeTypes.BULLET_LIST
    ]);

    const { childCount } = closestOrderedOrBulletNode;

    switch (nodeSelectionNodeType) {
        case NodeTypes.ORDERED_LIST:

        // eslint-disable-next-line no-fallthrough
        case NodeTypes.BULLET_LIST:
            if (childCount > 1) {
                //delete only the list item selected
                editor.chain().deleteNode(NodeTypes.LIST_ITEM).blur().run();
            } else {
                // delete the order/bullet node
                editor.chain().deleteNode(closestOrderedOrBulletNode.type).blur().run();
            }

            break;

        default:
            editor.chain().deleteNode(selectionParentNode.type).blur().run();
            break;
    }
};

export const formatHTML = (html: string) => {
    const pRex = new RegExp(/<p(|\s+[^>]*)>(.|\n)*?<\/p>/gm);
    const hRex = new RegExp(/<h\d+(|\s+[^>]*)>(.|\n)*?<\/h\d+>/gm);
    const liRex = new RegExp(/<li(|\s+[^>]*)>(.|\n)*?<\/li>/gm);
    const listRex = new RegExp(/<(ul|ol)(|\s+[^>]*)>(.|\n)*?<\/(ul|ol)>/gm);

    return html
        .replace(aTagRex, (content) => replaceInlineLinkImage(content))
        .replace(pRex, (content) => replaceInlineContent(content))
        .replace(hRex, (content) => replaceInlineContent(content))
        .replace(liRex, (content) => replaceInlineContent(content))
        .replace(listRex, (content) => replaceInlineContent(content));
};

export const replaceInlineContent = (content) => {
    // Get Images inside <a> Tag
    const images = content.match(imgTagRex) || [];

    // Check that after removing the images, it's not empty
    const text = new DOMParser().parseFromString(content, 'text/html').documentElement.textContent;

    // Move the <img/> tag to the end of the <p> tag.
    return text.trim().length > 0
        ? content.replace(imgTagRex, '') + images.join('')
        : images.join('');
};

export const replaceInlineLinkImage = (content) => {
    const container = document.createElement('div');
    container.innerHTML = content;
    const href = container.querySelector('a').getAttribute('href');
    const title = container.querySelector('a').getAttribute('href');
    const alt = container.querySelector('a').getAttribute('alt');

    return content
        .replace(/<a(|\s+[^>]*)>/gm, '')
        .replace(/<a\/>/gm, '')
        .replace(imgTagRex, (content) => {
            return content.replace(/img/gm, `img href="${href}" title="${title}" alt="${alt}"`);
        });
};

/**
 * This method is used to deselect current node.
 * Placing the cursor at the end of the node.
 *
 * @param {EditorView} view
 */
export const deselectCurrentNode = (view: EditorView) => {
    const { state } = view;
    const { doc } = state.tr;
    const resolvedEnd = state.selection.to;
    const selection = TextSelection.create(doc, resolvedEnd, resolvedEnd);
    view.dispatch(state.tr.setSelection(selection));
};

/**
 * Get position from cursor current position when pasting an image.
 *
 * @param {EditorView} view
 * @return {*}  {{ from: number; to: number }}
 */
export const getCursorPosition = (view: EditorView): { from: number; to: number } => {
    const { state } = view;
    const { selection } = state;
    const { ranges } = selection;
    const from = Math.min(...ranges.map((range) => range.$from.pos));
    const to = Math.max(...ranges.map((range) => range.$to.pos));

    return { from, to };
};

/**
 * Replace a node in Tiptap editor with new content.
 *
 * @param {Editor} editor - The Tiptap editor instance.
 * @param {DotTiptapNodeInformation} tiptapNodeInfo - Information about the Tiptap node to be replaced,
 * including the node itself, and its start and end positions.
 * @param {string} content - The new content to replace the existing node.
 * @returns {void}
 */
export const replaceNodeWithContent = (
    editor: Editor,
    tiptapNodeInfo: DotTiptapNodeInformation,
    content: string
): void => {
    const { node, from, to } = tiptapNodeInfo;

    // If the node is found, replace it with the new content
    if (node) {
        editor.chain().deleteRange({ from, to }).insertContentAt(from, content).run();
    }
};

/**
 * Find all occurrences of nodes of a specific type in the TipTap editor and determine their position ranges.
 *
 * @param {Editor} editor - The TipTap editor instance.
 * @param {NodeTypes} nodeType - The type of the node to search for (e.g., 'paragraph').
 * @returns {DotTiptapNodeInformation[] | null} An array containing information about the found nodes:
 *          - `node`: The found node.
 *          - `from`: The starting position of the found node in the document.
 *          - `to`: The ending position (exclusive) of the found node in the document.
 *          Returns `null` if no nodes of the specified type are found.
 */
export const findNodeByType = (
    editor: Editor, // The TipTap editor instance
    nodeType: NodeTypes // The type of the node to search for
): DotTiptapNodeInformation[] | null => {
    const nodes = [];

    // Traverse the document's descendants
    editor.state.doc.descendants((node, currentPosition) => {
        if (node.type.name === nodeType) {
            nodes.push({
                node,
                from: currentPosition,
                to: currentPosition + node.nodeSize
            });
        }
    });

    return nodes.length ? nodes : null;
};

/**
 * Check if the text is an image URL.
 *
 * @param {string} text
 * @return {*}  {boolean}
 */
export const isImageURL = (url: string): boolean => {
    return /^https?:\/\/.+\.(jpg|jpeg|png|webp|avif|gif|svg)$/.test(url);
};
