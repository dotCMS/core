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
        const { inode, language = '1', contentType, fieldName, blockEditorContent } = node.dataset;
        const content = JSON.parse(blockEditorContent || '');

        if (!inode || !language || !contentType || !fieldName) {
            console.error('Missing data attributes for block editor inline editing.');
            console.warn('inode, language, contentType and fieldName are required.');

            return;
        }

        node.classList.add('dotcms__inline-edit-field');
        node.addEventListener('click', () => {
            initInlineEditing('BLOCK_EDITOR', {
                inode,
                content,
                language: parseInt(language),
                fieldName,
                contentType
            });
        });
    });
};
