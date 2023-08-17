import { ResolvedPos } from 'prosemirror-model';
import { EditorView } from 'prosemirror-view';
import { SelectionRange, TextSelection } from 'prosemirror-state';
import { Editor } from '@tiptap/core';

import { NodeTypes, CustomNodeTypes } from '@dotcms/block-editor';

const aTagRex = new RegExp(/<a(|\s+[^>]*)>(\s|\n|<img[^>]*src="[^"]*"[^>]*>)*?<\/a>/gm);
const imgTagRex = new RegExp(/<img[^>]*src="[^"]*"[^>]*>/gm);

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
