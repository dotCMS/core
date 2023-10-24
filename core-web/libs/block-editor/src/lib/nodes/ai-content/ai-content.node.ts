import { Node } from '@tiptap/core';

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        AIContentNode: {
            insertAINode: (content?: string) => ReturnType;
        };
    }
}

export const AIContentNode = Node.create({
    name: 'aiContent',

    addAttributes() {
        return {
            content: {
                default: ''
            }
        };
    },

    parseHTML() {
        return [
            {
                tag: 'div[ai-content]'
            }
        ];
    },

    addOptions() {
        return {
            inline: false
        };
    },

    inline() {
        return this.options.inline;
    },

    group() {
        return 'block';
    },

    addCommands() {
        return {
            ...this.parent?.(),
            insertAINode:
                (content?: string) =>
                ({ commands }) => {
                    return commands.insertContent({
                        type: this.name,
                        attrs: { content: content }
                    });
                }
        };
    },

    renderHTML() {
        return ['div[ai-content]'];
    },

    addNodeView() {
        return ({ node }) => {
            const dom = document.createElement('div');
            const div = document.createElement('div');

            div.innerHTML = node.attrs.content || '';

            dom.contentEditable = 'true';
            dom.classList.add('ai-content-container');
            dom.append(div);

            return { dom };
        };
    }
});
