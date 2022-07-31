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
 *
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
