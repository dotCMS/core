import { JSONContent } from '@tiptap/core';

export const UNKNOWN_BLOCK_NODE_NAME = 'dotUnsupportedBlock';

type JSONLike = JSONContent | JSONContent[];
type JSONLikeOrUndefined = JSONLike | undefined;

export interface UnknownBlockNodeAttrs {
    originalType: string | null;
    originalNode: JSONContent | null;
    originalNodeRaw: string | null;
}

function isJsonContent(value: unknown): value is JSONContent {
    return (
        !!value &&
        !Array.isArray(value) &&
        typeof value === 'object' &&
        typeof (value as JSONContent).type === 'string' &&
        (value as JSONContent).type.length > 0
    );
}

export function createUnknownBlockNodeAttrs(
    node: JSONContent,
    nodeType: string | null
): UnknownBlockNodeAttrs {
    return {
        originalType: nodeType,
        originalNode: node,
        originalNodeRaw: null
    };
}

export function parseUnknownBlockOriginalNode(
    value: string | null
): Pick<UnknownBlockNodeAttrs, 'originalNode' | 'originalNodeRaw'> {
    if (!value) {
        return {
            originalNode: null,
            originalNodeRaw: null
        };
    }

    try {
        return {
            originalNode: JSON.parse(value),
            originalNodeRaw: null
        };
    } catch (error) {
        console.warn('[unsupported-block] failed to parse originalNode', error);

        return {
            originalNode: null,
            originalNodeRaw: value
        };
    }
}

export function renderUnknownBlockOriginalNode(
    attributes: Partial<UnknownBlockNodeAttrs>
): Record<string, string> {
    if (isJsonContent(attributes.originalNode)) {
        return { 'data-original-node': JSON.stringify(attributes.originalNode) };
    }

    return typeof attributes.originalNodeRaw === 'string' && attributes.originalNodeRaw.length > 0
        ? { 'data-original-node': attributes.originalNodeRaw }
        : {};
}

function replaceUnknownNode(node: JSONContent, knownNodeNames: Set<string>): JSONContent {
    const nodeType = typeof node.type === 'string' ? node.type : null;

    if (!nodeType || !knownNodeNames.has(nodeType)) {
        return {
            type: UNKNOWN_BLOCK_NODE_NAME,
            attrs: createUnknownBlockNodeAttrs(node, nodeType)
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

function restoreUnknownBlockNode(node: JSONContent): JSONContent {
    if (node.type === UNKNOWN_BLOCK_NODE_NAME && isJsonContent(node.attrs?.['originalNode'])) {
        return node.attrs['originalNode'];
    }

    return {
        ...node,
        content: restoreUnknownBlockNodes(node.content)
    };
}

export function restoreUnknownBlockNodes<T extends JSONLikeOrUndefined>(content: T): T {
    if (!content) {
        return content;
    }

    if (!Array.isArray(content)) {
        return restoreUnknownBlockNode(content) as T;
    }

    return content.map((node) => restoreUnknownBlockNode(node)) as T;
}
