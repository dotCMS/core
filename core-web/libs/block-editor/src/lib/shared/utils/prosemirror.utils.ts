import { ResolvedPos } from 'prosemirror-model';
import { EditorView } from 'prosemirror-view';
import { SelectionRange } from 'prosemirror-state';
import { Editor } from '@tiptap/core';

import { NodeTypes, CustomNodeTypes } from '@dotcms/block-editor';

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
 *
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
 *
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
 *
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
 *
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
