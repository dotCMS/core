import { Editor } from '@tiptap/core';
import { EditorView } from 'prosemirror-view';
import { EditorState } from 'prosemirror-state';
import { BubbleMenuPluginProps, BubbleMenuViewProps } from '@tiptap/extension-bubble-menu';
import { ComponentRef, EventEmitter } from '@angular/core';
import { BubbleChangeDropdownComponent } from '../extensions/components/bubble-change-dropdown/bubble-change-dropdown.component';

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
    items: BubbleMenuItem[];
    command: EventEmitter<BubbleMenuItem>;
    dropdown: BubbleChangeDropdownComponent;
}

export declare type DotBubbleMenuPluginProps = BubbleMenuPluginProps & {
    component: ComponentRef<BubbleMenuComponentProps>;
};

export declare type DotBubbleMenuViewProps = BubbleMenuViewProps & {
    component: ComponentRef<BubbleMenuComponentProps>;
};
