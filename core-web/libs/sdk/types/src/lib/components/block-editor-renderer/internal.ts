/**
 * Enum representing the different types of blocks available in the Block Editor
 *
 * @export
 * @enum {string}
 */
export enum BlockEditorDefaultBlocks {
    /** Represents a paragraph block */
    PARAGRAPH = 'paragraph',
    /** Represents a heading block */
    HEADING = 'heading',
    /** Represents a text block */
    TEXT = 'text',
    /** Represents a bullet/unordered list block */
    BULLET_LIST = 'bulletList',
    /** Represents an ordered/numbered list block */
    ORDERED_LIST = 'orderedList',
    /** Represents a list item within a list block */
    LIST_ITEM = 'listItem',
    /** Represents a blockquote block */
    BLOCK_QUOTE = 'blockquote',
    /** Represents a code block */
    CODE_BLOCK = 'codeBlock',
    /** Represents a hard break (line break) */
    HARDBREAK = 'hardBreak',
    /** Represents a horizontal rule/divider */
    HORIZONTAL_RULE = 'horizontalRule',
    /** Represents a DotCMS image block */
    DOT_IMAGE = 'dotImage',
    /** Represents a DotCMS video block */
    DOT_VIDEO = 'dotVideo',
    /** Represents a table block */
    TABLE = 'table',
    /** Represents a DotCMS content block */
    DOT_CONTENT = 'dotContent'
}

/**
 * Represents the validation state of a Block Editor instance
 *
 * @interface BlockEditorState
 * @property {boolean} isValid - Whether the blocks structure is valid
 * @property {string | null} error - Error message if blocks are invalid, null otherwise
 */
export interface BlockEditorState {
    error: string | null;
}
