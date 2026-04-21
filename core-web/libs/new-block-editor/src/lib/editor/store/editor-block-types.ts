export const DOT_BLOCK_TYPES = {
    aiContent: 'aiContent',
    aiImage: 'aiImage',
    blockquote: 'blockquote',
    codeBlock: 'codeBlock',
    dotContent: 'dotContent',
    gridBlock: 'gridBlock',
    heading1: 'heading1',
    heading2: 'heading2',
    heading3: 'heading3',
    heading4: 'heading4',
    heading5: 'heading5',
    heading6: 'heading6',
    horizontalRule: 'horizontalRule',
    image: 'image',
    orderedList: 'orderedList',
    bulletList: 'bulletList',
    table: 'table',
    video: 'video'
} as const;

export type DotBlockType = (typeof DOT_BLOCK_TYPES)[keyof typeof DOT_BLOCK_TYPES];

export const ALL_BLOCK_TYPES: DotBlockType[] = Object.values(DOT_BLOCK_TYPES);
