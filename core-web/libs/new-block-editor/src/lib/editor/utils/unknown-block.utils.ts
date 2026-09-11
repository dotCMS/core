import type { JSONContent } from '@tiptap/core';

import { preserveUnknownBlockMarks, preserveUnknownBlockNodes } from '@dotcms/dotcms-models';

export {
    createUnknownBlockMarkAttrs,
    createUnknownBlockNodeAttrs,
    parseUnknownBlockOriginalMark,
    parseUnknownBlockOriginalNode,
    preserveUnknownBlockMarks,
    preserveUnknownBlockNodes,
    renderUnknownBlockOriginalMark,
    renderUnknownBlockOriginalNode,
    restoreUnknownBlockNodes,
    UNKNOWN_BLOCK_MARK_NAME,
    UNKNOWN_BLOCK_NODE_NAME
} from '@dotcms/dotcms-models';

/**
 * Replaces unknown nodes with the `dotUnsupportedBlock` placeholder and unknown marks with
 * the `dotUnsupportedMark` placeholder, for either shape a Block Editor value can arrive in:
 * a `{ type: 'doc', content: [...] }` document, or a bare array of nodes — which is what some
 * hosts pass, notably the UVE side panel.
 *
 * Nodes are processed before marks on purpose: an unknown node is swallowed whole into the
 * placeholder's `originalNode` payload, which must stay byte-for-byte as stored, so the mark
 * pass only ever walks what is left of the real tree.
 *
 * The array branch is not cosmetic. Spreading an array into an object yields
 * `{ 0: node, 1: node, ..., content: undefined }`, dropping the document `type` and making
 * TipTap throw `RangeError: Unknown node type: undefined`, so the field renders blank for
 * ANY content (#37145). The legacy editor branches the same way — see
 * `dot-block-editor.component.ts:773`.
 */
export function preserveUnknownNodesInDocument(
    parsed: JSONContent | JSONContent[],
    knownNodeNames: Set<string>,
    knownMarkNames: Set<string>
): JSONContent | JSONContent[] {
    if (Array.isArray(parsed)) {
        return preserveUnknownBlockMarks(
            preserveUnknownBlockNodes(parsed, knownNodeNames),
            knownMarkNames
        );
    }

    return {
        ...parsed,
        content: preserveUnknownBlockMarks(
            preserveUnknownBlockNodes(parsed.content, knownNodeNames),
            knownMarkNames
        )
    };
}
