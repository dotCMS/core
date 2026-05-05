import { Node } from '@tiptap/core';

/** TipTap node name for AI-generated text blocks. Same as legacy block-editor for storage compat. */
export const AI_CONTENT_NODE_NAME = 'aiContent' as const;

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        aiContent: {
            /**
             * Insert AI-generated HTML at the current selection. Parses the HTML against
             * the editor schema so each block (paragraph, heading, list, etc.) becomes a
             * normal, fully editable node — NOT wrapped in an `aiContent` block.
             */
            insertAINode: (content?: string) => ReturnType;
            /** Toggle the loading state on an existing aiContent placeholder (legacy). */
            setLoadingAIContentNode: (loading: boolean) => ReturnType;
        };
    }
}

const LOADING_CLASS = 'is-loading';

export const AIContent = Node.create({
    name: AI_CONTENT_NODE_NAME,
    group: 'block',
    inline: false,

    addAttributes() {
        return {
            content: { default: '' },
            loading: { default: false }
        };
    },

    parseHTML() {
        return [{ tag: 'div[data-ai-content]' }];
    },

    renderHTML() {
        return ['div', { 'data-ai-content': 'true' }];
    },

    addCommands() {
        return {
            insertAINode:
                (content?: string) =>
                ({ commands }) => {
                    // Parse the AI HTML against the editor schema so each block becomes a
                    // normal node — not wrapped in an `aiContent` atom that the user can't
                    // navigate into or edit.
                    return commands.insertContent(content ?? '');
                },

            setLoadingAIContentNode:
                (loading: boolean) =>
                ({ tr, editor }) => {
                    let touched = false;
                    editor.state.doc.descendants((node, pos) => {
                        if (!touched && node.type.name === AI_CONTENT_NODE_NAME) {
                            tr.setNodeMarkup(pos, undefined, { ...node.attrs, loading });
                            touched = true;
                            return false;
                        }
                        return true;
                    });
                    return touched;
                }
        };
    },

    addNodeView() {
        return ({ node }) => {
            const dom = document.createElement('div');
            dom.setAttribute('data-ai-content', 'true');
            dom.classList.add('ai-content-container');
            dom.contentEditable = 'true';

            const inner = document.createElement('div');
            if (node.attrs['loading']) {
                dom.classList.add(LOADING_CLASS);
                inner.innerHTML =
                    '<span class="material-symbols-outlined animate-spin">progress_activity</span>';
            } else {
                inner.innerHTML = String(node.attrs['content'] ?? '');
            }
            dom.appendChild(inner);

            return { dom };
        };
    }
});
