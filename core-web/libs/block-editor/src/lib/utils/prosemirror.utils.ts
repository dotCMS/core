import { ResolvedPos } from 'prosemirror-model';
import { NodeTypes } from '@dotcms/block-editor';
import { EditorView } from 'prosemirror-view';

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
