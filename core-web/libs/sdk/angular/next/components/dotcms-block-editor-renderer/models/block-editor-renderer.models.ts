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

export interface ContentNode {
    type: string;
    content: ContentNode[];
    attrs?: Record<string, string>;
    marks?: Mark[];
    text?: string;
}

export interface Block {
    content: ContentNode[];
    type: string;
}

export enum Blocks {
    PARAGRAPH = 'paragraph',
    HEADING = 'heading',
    TEXT = 'text',
    BULLET_LIST = 'bulletList',
    ORDERED_LIST = 'orderedList',
    LIST_ITEM = 'listItem',
    BLOCK_QUOTE = 'blockquote',
    CODE_BLOCK = 'codeBlock',
    HARDBREAK = 'hardBreak',
    HORIZONTAL_RULE = 'horizontalRule',
    DOT_IMAGE = 'dotImage',
    DOT_VIDEO = 'dotVideo',
    TABLE = 'table',
    DOT_CONTENT = 'dotContent'
}