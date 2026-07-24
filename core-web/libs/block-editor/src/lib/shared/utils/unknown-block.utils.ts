import { JSONContent } from '@tiptap/core';

export const UNKNOWN_BLOCK_NODE_NAME = 'dotUnsupportedBlock';

type JSONLike = JSONContent | JSONContent[];
type JSONLikeOrUndefined = JSONLike | undefined;

function isJsonContent(value: unknown): value is JSONContent {
    return (
        !!value &&
        !Array.isArray(value) &&
        typeof value === 'object' &&
        typeof (value as JSONContent).type === 'string' &&
        (value as JSONContent).type.length > 0
    );
}

function replaceUnknownNode(node: JSONContent, knownNodeNames: Set<string>): JSONContent {
    const nodeType = typeof node.type === 'string' ? node.type : null;

    if (!nodeType || !knownNodeNames.has(nodeType)) {
        return {
            type: UNKNOWN_BLOCK_NODE_NAME,
            attrs: {
                originalNode: node,
                originalType: nodeType
            }
        };
    }

    return {
        ...node,
        content: preserveUnknownBlockNodes(node.content, knownNodeNames)
    };
}

export function preserveUnknownBlockNodes<T extends JSONLikeOrUndefined>(
    content: T,
    knownNodeNames: Set<string>
): T {
    if (!content) {
        return content;
    }

    if (!Array.isArray(content)) {
        return replaceUnknownNode(content, knownNodeNames) as T;
    }

    return content.map((node) => replaceUnknownNode(node, knownNodeNames)) as T;
}

export function restoreUnknownBlockNodes<T extends JSONLikeOrUndefined>(content: T): T {
    if (!content) {
        return content;
    }

    if (!Array.isArray(content)) {
        if (
            content.type === UNKNOWN_BLOCK_NODE_NAME &&
            isJsonContent(content.attrs?.['originalNode'])
        ) {
            return content.attrs['originalNode'] as T;
        }

        return {
            ...content,
            content: restoreUnknownBlockNodes(content.content)
        } as T;
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
