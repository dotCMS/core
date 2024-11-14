import { initInlineEditing } from '../sdk-editor';

/**
 * Listen for block editor inline event.
 */
export const listenBlockEditorInlineEvent = (): void => {
    if (document.readyState === 'complete') {
        // The page is fully loaded or interactive
        listenBlockEditorClick();

        return;
    }

    window.addEventListener('load', () => listenBlockEditorClick());
};

const listenBlockEditorClick = (): void => {
    const editBlockEditorNodes: NodeListOf<HTMLElement> = document.querySelectorAll(
        '[data-block-editor-content]'
    );

    if (!editBlockEditorNodes.length) {
        return;
    }

    editBlockEditorNodes.forEach((node: HTMLElement) => {
        const { inode, languageId, contentType, fieldName, blockEditorContent } = node.dataset;
        const content = JSON.parse(blockEditorContent || '');

        if (!inode || !languageId || !contentType || !fieldName) {
            console.error('Missing data attributes for block editor inline editing.');
            console.warn('inode, languageId, contentType and fieldName are required.');

            return;
        }

        node.classList.add('dotcms__inline-edit-field');
        node.addEventListener('click', () => {
            initInlineEditing('BLOCK_EDITOR', {
                inode,
                content,
                fieldName,
                languageId,
                contentType
            });
        });
    });
};
