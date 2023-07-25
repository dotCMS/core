import { Node } from '@tiptap/core';

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        CustomNode: {
            addHelloWorld: () => ReturnType;
        };
    }
}

export const CustomNode = Node.create({
    name: 'customNode',

    storage: {
        icon: 'customNode'
    },

    addAttributes() {
        return {};
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
        return this.options.inline;
    },

    group() {
        return 'block';
    },

    addCommands() {
        return {
            // Custom command to insert a custom node
            addHelloWorld:
                () =>
                ({ commands }) => {
                    return commands.insertContent({ type: this.name });
                }
        };
    },

    addNodeView() {
        return () => {
            const dom = document.createElement('div');
            dom.contentEditable = 'false';
            const label = document.createElement('label');

            label.innerHTML = 'Hello World';
            label.contentEditable = 'false';

            // Styles
            label.style.fontWeight = 'bold';
            label.style.fontSize = '25px';
            label.style.paddingBottom = '10px';

            // Styles
            dom.style.padding = '4px';
            dom.style.background = '#f9dc5c';
            dom.style.borderRadius = '5px';
            dom.style.border = '2px solid #333';
            dom.style.marginBottom = '10px';

            dom.append(label);

            return { dom };
        };
    }
});
