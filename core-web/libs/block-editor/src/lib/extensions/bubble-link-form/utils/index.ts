import { textNodeRange } from '@dotcms/block-editor';

export const openFormLinkOnclik = ({ editor, view, pos }) => {
    const selectionStart = view.state.doc?.resolve(pos);
    const range = textNodeRange(selectionStart, pos);
    const { from, to } = range;

    if (from === to) {
        return;
    }

    editor.chain().setTextSelection(range).openLinkForm({ openOnClick: true }).run();
};
