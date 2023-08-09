import { Node } from '@tiptap/core';

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        AIContentNode: {
            showGeneratedContent: (content?: string) => ReturnType;
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
                tag: 'div'
            }
        ];
    },

    renderHTML() {
        return ['div'];
    },

    addOptions() {
        return {
            inline: false
        };
    },

    inline() {
        return false;
    },

    group() {
        return 'block';
    },

    addCommands() {
        return {
            showGeneratedContent:
                (content?: string) =>
                ({ commands }) => {
                    return commands.insertContent({
                        type: this.name,
                        attrs: { content: content }
                    });
                }
        };
    },

    addNodeView() {
        return ({ node }) => {
            const dom = document.createElement('div');
            const div = document.createElement('div');

            div.contentEditable = 'false';
            div.innerHTML = node.attrs.content || '';
            dom.append(div);

            return { dom };
        };
    }
});
