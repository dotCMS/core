import { JSONContent } from '@tiptap/core';

export const UNKNOWN_BLOCK_NODE_NAME = 'dotUnsupportedBlock';

type JSONLike = JSONContent | JSONContent[];
type JSONLikeOrUndefined = JSONLike | undefined;

function isJsonContent(value: unknown): value is JSONContent {
    return !!value && typeof value === 'object' && typeof (value as JSONContent).type === 'string';
}

function replaceUnknownNode(node: JSONContent, knownNodeNames: Set<string>): JSONContent {
    if (!knownNodeNames.has(node.type)) {
        return {
            type: UNKNOWN_BLOCK_NODE_NAME,
            attrs: {
                originalNode: node,
                originalType: node.type
            }
        };
    }

    return {
        ...node,
        content: node.content ? preserveUnknownBlockNodes(node.content, knownNodeNames) : node.content
    };
}

export function preserveUnknownBlockNodes<T extends JSONLike>(
    content: T,
    knownNodeNames: Set<string>
): T {
    if (!Array.isArray(content)) {
        return content;
    }

    return content.map((node) => replaceUnknownNode(node, knownNodeNames)) as T;
}

export function restoreUnknownBlockNodes<T extends JSONLikeOrUndefined>(content: T): T {
    if (!Array.isArray(content)) {
        return content;
    }

    return content.map((node) => {
        if (
            node.type === UNKNOWN_BLOCK_NODE_NAME &&
            isJsonContent(node.attrs?.['originalNode'])
        ) {
            return node.attrs['originalNode'];
        }

        return {
            ...node,
            content: restoreUnknownBlockNodes(node.content)
        };
    }) as T;
}
