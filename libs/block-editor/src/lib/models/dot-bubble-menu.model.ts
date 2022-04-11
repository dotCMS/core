import { Editor } from '@tiptap/core';
import { EditorView } from 'prosemirror-view';
import { EditorState } from 'prosemirror-state';
import { BubbleMenuPluginProps, BubbleMenuViewProps } from '@tiptap/extension-bubble-menu';
import { ComponentRef, EventEmitter } from '@angular/core';
import { SuggestionsComponent } from '../extensions/components/suggestions/suggestions.component';

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
