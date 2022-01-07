import { Editor, isTextSelection } from '@tiptap/core';
import { EditorView } from 'prosemirror-view';
import { EditorState } from 'prosemirror-state';

interface ShouldShowProps {
    editor: Editor;
    view: EditorView<any>;
    state: EditorState<any>;
    oldState?: EditorState<any>;
    from: number;
    to: number;
}

export const shouldShowBubbleMenu = ({ editor, state, from, to }: ShouldShowProps) => {

    const { doc, selection } = state
    const { empty } = selection

    // Current selected node
    const node = editor.state.doc.nodeAt(editor.state.selection.from);

    // Sometime check for `empty` is not enough.
    // Doubleclick an empty paragraph returns a node size of 2.
    // So we check also for an empty text size.
    const isEmptyTextBlock = !doc.textBetween(from, to).length
    && isTextSelection(state.selection)


    // If it's empty or the current node is type dotContent, it will not open.
    if (empty || isEmptyTextBlock || node.type.name == 'dotContent') {
        return false
    }

    return true;

};
