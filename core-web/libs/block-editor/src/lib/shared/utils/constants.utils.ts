import { InjectionToken } from '@angular/core';

export const AI_PLUGIN_INSTALLED_TOKEN = new InjectionToken<boolean>('is AI Plugin installed');

export const AI_PLUGIN_KEY = {
    NOT_SET: 'NOT SET'
};

export const DEFAULT_LANG_ID = 1;

export type DotConfigKeys = 'lang';

export interface ContentletFilters {
    contentType: string;
    filter: string;
    currentLanguage: number;
    contentletIdentifier: string;
}

export enum NodeTypes {
    DOT_IMAGE = 'dotImage',
    LIST_ITEM = 'listItem',
    BULLET_LIST = 'bulletList',
    ORDERED_LIST = 'orderedList',
    BLOCKQUOTE = 'blockquote',
    CODE_BLOCK = 'codeBlock',
    DOC = 'doc',
    DOT_CONTENT = 'dotContent',
    PARAGRAPH = 'paragraph',
    HARD_BREAK = 'hardBreak',
    HEADING = 'heading',
    HORIZONTAL_RULE = 'horizontalRule',
    TEXT = 'text',
    TABLE_CELL = 'tableCell',
    AI_CONTENT = 'aiContent',
    LOADER = 'loader'
}

export const CustomNodeTypes: Array<NodeTypes> = [NodeTypes.DOT_IMAGE, NodeTypes.DOT_CONTENT];

export const getNodeCoords = (node: HTMLElement, type: string): DOMRect => {
    if (type === NodeTypes.DOT_IMAGE && node?.firstElementChild) {
        return node.firstElementChild.getBoundingClientRect();
    }

    return node.getBoundingClientRect();
};

export const popperModifiers = [
    {
        name: 'offset',
        options: {
            offset: [0, 5]
        }
    },
    {
        name: 'flip',
        options: {
            fallbackPlacements: ['bottom-start', 'top-start']
        }
    },
    {
        name: 'preventOverflow',
        options: {
            altAxis: true,
            tether: true
        }
    }
];

export const getNodePosition = (node: HTMLElement, type: string): DOMRect => {
    if (type === NodeTypes.DOT_IMAGE) {
        const img = node.getElementsByTagName('img')[0];

        // If is a image Node, get the image position
        return img?.getBoundingClientRect() || node.getBoundingClientRect();
    }

    return node.getBoundingClientRect();
};
