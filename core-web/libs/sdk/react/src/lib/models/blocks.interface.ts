import { ContentNode } from './content-node.interface';

export interface Block {
    content: ContentNode[];
}

export interface DotContentletProps {
    title: string;
    baseType: string;
    inode: string;
    archived: boolean;
    working: boolean;
    locked: boolean;
    contentType: string;
    live: boolean;
    identifier: string;
    image: string;
    imageContentAsset: string;
    urlTitle: string;
    url: string;
    titleImage: string;
    urlMap: string;
    hasLiveVersion: boolean;
    hasTitleImage: boolean;
    sortOrder: number;
    modUser: string;
    __icon__: string;
    contentTypeIcon: string;
    language: string;
    description: string;
    shortDescription: string;
    salePrice: string;
    retailPrice: string;
    mimeType: string;
    thumbnail?: string;
}

export interface BlockProps {
    children: React.ReactNode;
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
