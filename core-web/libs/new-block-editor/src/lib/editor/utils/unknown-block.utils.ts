import type { JSONContent } from '@tiptap/core';

import { preserveUnknownBlockNodes } from '@dotcms/dotcms-models';

export {
    createUnknownBlockNodeAttrs,
    parseUnknownBlockOriginalNode,
    preserveUnknownBlockNodes,
    renderUnknownBlockOriginalNode,
    restoreUnknownBlockNodes,
    UNKNOWN_BLOCK_NODE_NAME
} from '@dotcms/dotcms-models';

/**
 * Replaces unknown nodes with the `dotUnsupportedBlock` placeholder, for either shape a
 * Block Editor value can arrive in: a `{ type: 'doc', content: [...] }` document, or a bare
 * array of nodes — which is what some hosts pass, notably the UVE side panel.
 *
 * The array branch is not cosmetic. Spreading an array into an object yields
 * `{ 0: node, 1: node, ..., content: undefined }`, dropping the document `type` and making
 * TipTap throw `RangeError: Unknown node type: undefined`, so the field renders blank for
 * ANY content (#37145). The legacy editor branches the same way — see
 * `dot-block-editor.component.ts:773`.
 */
export function preserveUnknownNodesInDocument(
    parsed: JSONContent | JSONContent[],
    knownNodeNames: Set<string>
): JSONContent | JSONContent[] {
    if (Array.isArray(parsed)) {
        return preserveUnknownBlockNodes(parsed, knownNodeNames);
    }

    return {
        ...parsed,
        content: preserveUnknownBlockNodes(parsed.content, knownNodeNames)
    };
}
