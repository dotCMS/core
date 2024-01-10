import { EditorState } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';

import { ComponentRef, EventEmitter } from '@angular/core';

import { Editor } from '@tiptap/core';
import { BubbleMenuPluginProps, BubbleMenuViewProps } from '@tiptap/extension-bubble-menu';

import { SuggestionsComponent } from '../../../shared';

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

export interface ShouldShowProps {
    editor: Editor;
    view: EditorView;
    state: EditorState;
    oldState?: EditorState;
    from: number;
    to: number;
}

export interface BubbleMenuItem {
    icon?: string;
    text?: string;
    markAction: string;
    active: boolean;
    divider?: boolean;
}

export interface BubbleMenuComponentProps {
    command: EventEmitter<BubbleMenuItem>;
    items: BubbleMenuItem[];
    selected: string;
    toggleChangeTo: EventEmitter<void>;
}

export declare type DotBubbleMenuPluginProps = BubbleMenuPluginProps & {
    component: ComponentRef<BubbleMenuComponentProps>;
    changeToComponent: ComponentRef<SuggestionsComponent>;
    changeToElement: HTMLElement;
};

export declare type DotBubbleMenuViewProps = BubbleMenuViewProps & {
    component: ComponentRef<BubbleMenuComponentProps>;
    changeToComponent: ComponentRef<SuggestionsComponent>;
    changeToElement: HTMLElement;
};

/**
 * If you need to to hide the bubble menu on another extension,
 * add the extension name here first
 */
export interface HideBubbleMenuExtensions {
    tableCell: boolean;
    table: boolean;
    dotVideo: boolean;
    youtube: boolean;
    aiContent: boolean;
    loader: boolean;
}
