import { Editor } from '@tiptap/core';
import { EditorView } from 'prosemirror-view';
import { EditorState } from 'prosemirror-state';
import { BubbleMenuPluginProps, BubbleMenuViewProps } from '@tiptap/extension-bubble-menu';
import { ComponentRef, EventEmitter } from '@angular/core';
import { SuggestionsComponent } from '../extensions/components/suggestions/suggestions.component';

export const DEFAULT_LANG_ID = 1;
export type DotConfigKeys = 'lang';

export interface ContentletFilters {
    contentType: string;
    filter: string;
    currentLanguage: number;
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
    TEXT = 'text'
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
    icon: string;
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
