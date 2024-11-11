import { CLIENT_ACTIONS } from '../models/client.model';

/**
 * Listen for block editor inline event.
 *
 */
export const listenBlockEditorInlineEvent = () => {
    const listenBlockEditorClick = () => {
        const editBlockEditorNodes = document.querySelectorAll('[data-block-editor-content]');
        if (!editBlockEditorNodes.length) {
            return;
        }

        editBlockEditorNodes.forEach((node) => {
            node.classList.add('dotcms__inline-edit-field');
            node.addEventListener('click', () => {
                const payload = { ...(node as HTMLElement).dataset };
                window.parent.postMessage(
                    {
                        payload,
                        action: CLIENT_ACTIONS.INIT_BLOCK_EDITOR_INLINE_EDITING
                    },
                    '*'
                );
            });
        });
    };

    if (document.readyState === 'complete') {
        // The page is fully loaded
        listenBlockEditorClick();
    } else {
        window.addEventListener('load', () => listenBlockEditorClick());
    }
};
