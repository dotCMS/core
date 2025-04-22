/**
 * Represents a Mark used by text content in the Block Editor
 *
 * @export
 * @interface Mark
 */
export interface Mark {
    type: string;
    attrs: Record<string, string>;
}

/**
 * Represents a Content Node used by the Block Editor
 *
 * @export
 * @interface ContentNode
 */
export interface ContentNode {
    /** The type of content node */
    type: string;
    /** Child content nodes */
    content?: ContentNode[];
    /** Optional attributes for the node */
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    attrs?: Record<string, any>;
    /** Optional marks applied to text content */
    marks?: Mark[];
    /** Optional text content */
    text?: string;
}

/**
 * Represents a Block in the Block Editor
 *
 * @export
 * @interface Block
 */
export interface Block {
    content?: ContentNode[];
    type: string;
}
