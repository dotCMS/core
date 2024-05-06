import { Node } from '@tiptap/core';

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        LoaderNode: {
            insertLoaderNode: (isLoading?: boolean, position?: number) => ReturnType;
        };
    }
}

export const LoaderNode = Node.create({
    name: 'loader',

    addAttributes() {
        return {
            isLoading: {
                default: true
            }
        };
    },

    parseHTML() {
        return [
            {
                tag: 'div.p-d-flex.p-jc-center'
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
            insertLoaderNode:
                (isLoading?: boolean, position?: number) =>
                ({ chain, state }) => {
                    const { selection } = state;
                    const { head } = selection;
                    const node = {
                        type: this.name,
                        attrs: { isLoading: isLoading }
                    };

                    return chain()
                        .insertContentAt(position ?? head, node)
                        .run();
                }
        };
    },

    renderHTML() {
        return ['div', { class: 'p-d-flex p-jc-center' }];
    },

    addNodeView() {
        return ({ node }) => {
            const dom = document.createElement('div');

            dom.classList.add('loader-style');

            if (node.attrs.isLoading) {
                const spinner = document.createElement('div');
                spinner.classList.add('p-progress-spinner');
                dom.append(spinner);
            }

            return { dom };
        };
    }
});
