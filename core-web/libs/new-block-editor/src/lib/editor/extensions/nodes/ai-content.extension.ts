import { Node } from '@tiptap/core';

/** TipTap node name for AI-generated text blocks. Same as legacy block-editor for storage compat. */
export const AI_CONTENT_NODE_NAME = 'aiContent' as const;

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        aiContent: {
            /** Insert a new aiContent node with the given HTML, or replace the existing one's content. */
            insertAINode: (content?: string) => ReturnType;
            /** Toggle the loading state on the existing aiContent node. */
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
                ({ commands, editor, tr }) => {
                    // If an aiContent node already exists in the doc, replace its content in-place.
                    let foundPos: number | null = null;
                    editor.state.doc.descendants((node, pos) => {
                        if (foundPos === null && node.type.name === AI_CONTENT_NODE_NAME) {
                            foundPos = pos;
                            return false;
                        }
                        return true;
                    });

                    if (foundPos !== null) {
                        tr.setNodeMarkup(foundPos, undefined, {
                            content: content ?? '',
                            loading: false
                        });
                        commands.setNodeSelection(foundPos);
                        return true;
                    }

                    return commands.insertContent({
                        type: AI_CONTENT_NODE_NAME,
                        attrs: { content: content ?? '', loading: false }
                    });
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
