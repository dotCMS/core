import { Editor } from '@tiptap/core';

export function getContentletDataFromSelection(editor: Editor) {
    const selection = editor?.state?.selection;

    // Get the slice of the selection
    const slice = selection?.content?.();

    // Get the fragment of the slice
    const fragment = slice?.content;

    // Get the first node of the fragment
    const selectionNode = fragment?.content?.[0];

    if (!selectionNode) {
        console.warn('Selection node is undefined');

        return {};
    }

    // Extract content type information from the selected node
    return selectionNode?.attrs?.data || {};
}
