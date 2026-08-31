import { JSONContent, Mark, Node } from '@tiptap/core';

export const UNKNOWN_BLOCK_NODE_NAME = 'dotUnsupportedBlock';
export const UNKNOWN_BLOCK_MARK_NAME = 'dotUnsupportedMark';

type JSONLike = JSONContent | JSONContent[];
type JSONLikeOrUndefined = JSONLike | undefined;

/** The JSON shape TipTap uses for an entry of `JSONContent.marks`. */
type JSONMark = NonNullable<JSONContent['marks']>[number];

export interface UnknownBlockNodeAttrs {
    originalType: string | null;
    originalNode: JSONContent | null;
    originalNodeRaw: string | null;
}

export interface UnknownBlockMarkAttrs {
    originalType: string | null;
    originalMark: JSONMark | null;
    originalMarkRaw: string | null;
}

function isJsonContent(value: unknown): value is JSONContent {
    const type = (value as JSONContent | null | undefined)?.type;

    return (
        !!value &&
        !Array.isArray(value) &&
        typeof value === 'object' &&
        typeof type === 'string' &&
        type.length > 0
    );
}

/**
 * Builds the placeholder attrs stored on `dotUnsupportedBlock`.
 *
 * @param node The original unsupported TipTap node JSON.
 * @param nodeType The original TipTap node name, or `null` when the source node
 * type was missing or invalid.
 * @returns Stable attrs used by both editors to preserve the original node payload.
 */
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

/**
 * Builds the placeholder attrs stored on `dotUnsupportedMark` — the mark-side
 * counterpart of {@link createUnknownBlockNodeAttrs}.
 */
export function createUnknownBlockMarkAttrs(
    mark: JSONMark,
    markType: string | null
): UnknownBlockMarkAttrs {
    return {
        originalType: markType,
        originalMark: mark,
        originalMarkRaw: null
    };
}

/**
 * Parses the serialized `data-original-node` HTML attribute back into JSON when
 * possible, and preserves the raw string separately when parsing fails so the
 * original content can still round-trip through the placeholder unchanged.
 */
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

/** Mark-side counterpart of {@link parseUnknownBlockOriginalNode}. */
export function parseUnknownBlockOriginalMark(
    value: string | null
): Pick<UnknownBlockMarkAttrs, 'originalMark' | 'originalMarkRaw'> {
    if (!value) {
        return {
            originalMark: null,
            originalMarkRaw: null
        };
    }

    try {
        return {
            originalMark: JSON.parse(value),
            originalMarkRaw: null
        };
    } catch (error) {
        console.warn('[unsupported-mark] failed to parse originalMark', error);

        return {
            originalMark: null,
            originalMarkRaw: value
        };
    }
}

/**
 * Re-renders the preserved original node payload back onto the placeholder DOM
 * node so unsupported blocks keep their recoverable serialized representation.
 */
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

/** Mark-side counterpart of {@link renderUnknownBlockOriginalNode}. */
export function renderUnknownBlockOriginalMark(
    attributes: Partial<UnknownBlockMarkAttrs>
): Record<string, string> {
    if (isJsonContent(attributes.originalMark)) {
        return { 'data-original-mark': JSON.stringify(attributes.originalMark) };
    }

    return typeof attributes.originalMarkRaw === 'string' && attributes.originalMarkRaw.length > 0
        ? { 'data-original-mark': attributes.originalMarkRaw }
        : {};
}

export function createUnsupportedBlockNode() {
    return Node.create({
        name: UNKNOWN_BLOCK_NODE_NAME,
        group: 'block',
        atom: true,
        selectable: true,
        draggable: true,

        addAttributes() {
            return {
                originalType: {
                    default: null,
                    parseHTML: (element) => element.getAttribute('data-original-type'),
                    renderHTML: (attributes) =>
                        attributes['originalType']
                            ? { 'data-original-type': attributes['originalType'] }
                            : {}
                },
                originalNode: {
                    default: null,
                    parseHTML: (element) =>
                        parseUnknownBlockOriginalNode(element.getAttribute('data-original-node'))
                            .originalNode,
                    renderHTML: (attributes) => renderUnknownBlockOriginalNode(attributes)
                },
                originalNodeRaw: {
                    default: null,
                    parseHTML: (element) =>
                        parseUnknownBlockOriginalNode(element.getAttribute('data-original-node'))
                            .originalNodeRaw,
                    renderHTML: () => ({})
                }
            };
        },

        parseHTML() {
            return [{ tag: `div[data-node-view-wrapper="${UNKNOWN_BLOCK_NODE_NAME}"]` }];
        },

        renderHTML({ HTMLAttributes, node }) {
            const originalType = node.attrs['originalType'] || 'unknown';

            return [
                'div',
                {
                    ...HTMLAttributes,
                    'data-node-view-wrapper': UNKNOWN_BLOCK_NODE_NAME,
                    contenteditable: 'false',
                    class: 'dot-unsupported-block'
                },
                `Unsupported block (${originalType})`
            ];
        }
    });
}

/**
 * Placeholder for a mark the current schema does not know, and the reason unknown marks
 * no longer blank the whole field (#37175).
 *
 * Unlike an unknown *node*, an unknown *mark* has nothing to show: the text it decorates
 * is ordinary text that must keep rendering and stay editable. So this mark is visually
 * neutral — it only carries the original mark payload so it round-trips back on save
 * instead of being dropped.
 *
 * `inclusive: false` keeps the mark from swallowing text typed at its boundary, so an
 * author extending that sentence does not silently widen a mark this editor cannot render.
 */
export function createUnsupportedBlockMark() {
    return Mark.create({
        name: UNKNOWN_BLOCK_MARK_NAME,
        inclusive: false,

        addAttributes() {
            return {
                originalType: {
                    default: null,
                    parseHTML: (element) => element.getAttribute('data-original-type'),
                    renderHTML: (attributes) =>
                        attributes['originalType']
                            ? { 'data-original-type': attributes['originalType'] }
                            : {}
                },
                originalMark: {
                    default: null,
                    parseHTML: (element) =>
                        parseUnknownBlockOriginalMark(element.getAttribute('data-original-mark'))
                            .originalMark,
                    renderHTML: (attributes) => renderUnknownBlockOriginalMark(attributes)
                },
                originalMarkRaw: {
                    default: null,
                    parseHTML: (element) =>
                        parseUnknownBlockOriginalMark(element.getAttribute('data-original-mark'))
                            .originalMarkRaw,
                    renderHTML: () => ({})
                }
            };
        },

        parseHTML() {
            return [{ tag: `span[data-mark-type="${UNKNOWN_BLOCK_MARK_NAME}"]` }];
        },

        renderHTML({ HTMLAttributes }) {
            return ['span', { ...HTMLAttributes, 'data-mark-type': UNKNOWN_BLOCK_MARK_NAME }, 0];
        }
    });
}

/**
 * Replaces any node whose type is unknown to the current schema with the shared
 * unsupported-block placeholder while recursively preserving unknown descendants
 * inside otherwise-known parent nodes.
 */
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

/** Swaps a single mark for the placeholder when the schema does not declare it. */
function replaceUnknownMark(mark: JSONMark, knownMarkNames: Set<string>): JSONMark {
    const markType = typeof mark?.type === 'string' ? mark.type : null;

    if (markType && knownMarkNames.has(markType)) {
        return mark;
    }

    return {
        type: UNKNOWN_BLOCK_MARK_NAME,
        attrs: createUnknownBlockMarkAttrs(mark, markType)
    };
}

function replaceUnknownMarksInNode(node: JSONContent, knownMarkNames: Set<string>): JSONContent {
    // A `dotUnsupportedBlock` placeholder already holds its whole original payload —
    // marks included — in `attrs.originalNode`. That payload is inert data rather than
    // part of the document tree, so it has to be left byte-for-byte as stored.
    if (node.type === UNKNOWN_BLOCK_NODE_NAME) {
        return node;
    }

    return {
        ...node,
        marks: Array.isArray(node.marks)
            ? node.marks.map((mark) => replaceUnknownMark(mark, knownMarkNames))
            : node.marks,
        content: preserveUnknownBlockMarks(node.content, knownMarkNames)
    };
}

/**
 * Replaces every mark the schema does not declare with the `dotUnsupportedMark`
 * placeholder, so `Node.fromJSON` never meets a mark type it cannot resolve.
 *
 * This is the mark-side half of {@link preserveUnknownBlockNodes} and must run *after*
 * it: nodes first, so content already swallowed into a `dotUnsupportedBlock` payload is
 * not rewritten, then marks over what remains of the real tree.
 *
 * Without this an unknown mark threw `RangeError: There is no mark type X in this schema`
 * mid-recursion, aborting `Node.fromJSON` for the ENTIRE document. TipTap catches that,
 * warns, and boots an empty document — so the field looked emptied while the stored JSON
 * was intact, and the next save made the loss real (#37175).
 */
export function preserveUnknownBlockMarks<T extends JSONLikeOrUndefined>(
    content: T,
    knownMarkNames: Set<string>
): T {
    if (!content) {
        return content;
    }

    if (!Array.isArray(content)) {
        return replaceUnknownMarksInNode(content, knownMarkNames) as T;
    }

    return content.map((node) => replaceUnknownMarksInNode(node, knownMarkNames)) as T;
}

/**
 * Restores a placeholder mark back to the mark it stood in for, keeping the placeholder
 * when the payload is no longer valid so the recoverable raw string still round-trips.
 */
function restoreUnknownMark(mark: JSONMark): JSONMark {
    if (mark?.type === UNKNOWN_BLOCK_MARK_NAME && isJsonContent(mark.attrs?.['originalMark'])) {
        return mark.attrs['originalMark'] as JSONMark;
    }

    return mark;
}

/**
 * Restores a placeholder node back to its original JSON only when the preserved
 * payload is still a valid TipTap node; otherwise the placeholder is kept so the
 * recoverable raw payload can continue round-tripping.
 */
function restoreUnknownBlockNode(node: JSONContent): JSONContent {
    if (node.type === UNKNOWN_BLOCK_NODE_NAME && isJsonContent(node.attrs?.['originalNode'])) {
        return node.attrs['originalNode'];
    }

    return {
        ...node,
        marks: Array.isArray(node.marks) ? node.marks.map(restoreUnknownMark) : node.marks,
        content: restoreUnknownBlockNodes(node.content)
    };
}

/**
 * Inverse of {@link preserveUnknownBlockNodes} + {@link preserveUnknownBlockMarks}: turns
 * both placeholders back into the payloads they preserved, so what gets saved is the
 * content that was loaded plus the author's edits — never the placeholders themselves.
 */
export function restoreUnknownBlockNodes<T extends JSONLikeOrUndefined>(content: T): T {
    if (!content) {
        return content;
    }

    if (!Array.isArray(content)) {
        return restoreUnknownBlockNode(content) as T;
    }

    return content.map((node) => restoreUnknownBlockNode(node)) as T;
}
