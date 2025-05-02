/**
 * Represents a Mark used by text content in the Block Editor
 *
 * @export
 * @interface Mark
 */
export interface BlockEditorMark {
    type: string;
    attrs: Record<string, string>;
}

/**
 * Represents a Content Node used by the Block Editor
 *
 * @export
 * @interface BlockEditorNode
 */
export interface BlockEditorNode {
    /** The type of content node */
    type: string;
    /** Child content nodes */
    content?: BlockEditorNode[];
    /** Optional attributes for the node */
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    attrs?: Record<string, any>;
    /** Optional marks applied to text content */
    marks?: BlockEditorMark[];
    /** Optional text content */
    text?: string;
}

/**
 * Represents a Block in the Block Editor
 *
 * @export
 * @interface BlockEditorContent
 */
export interface BlockEditorContent {
    content?: BlockEditorNode[];
    type: string;
}
