import { Node, mergeAttributes } from '@tiptap/core';

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        gridBlock: {
            insertGrid: () => ReturnType;
        };
    }
}

export const GridColumn = Node.create({
    name: 'gridColumn',
    group: 'gridColumnGroup',
    content: 'block+',
    isolating: true,

    addAttributes() {
        return {
            span: { default: 6 }
        };
    },

    parseHTML() {
        return [{ tag: 'div[data-type="gridColumn"]' }];
    },

    renderHTML({ HTMLAttributes }) {
        return ['div', mergeAttributes({ 'data-type': 'gridColumn' }, HTMLAttributes), 0];
    },

    addNodeView() {
        return ({ node }) => {
            const dom = document.createElement('div');
            dom.setAttribute('data-type', 'gridColumn');
            dom.style.gridColumn = `span ${node.attrs['span'] ?? 6}`;
            dom.classList.add('grid-block__column');

            const contentDOM = document.createElement('div');
            contentDOM.classList.add('grid-block__column-content');
            dom.appendChild(contentDOM);

            return {
                dom,
                contentDOM,
                update(updatedNode) {
                    if (updatedNode.type.name !== 'gridColumn') return false;
                    dom.style.gridColumn = `span ${updatedNode.attrs['span'] ?? 6}`;
                    return true;
                }
            };
        };
    }
});

export const GridBlock = Node.create({
    name: 'gridBlock',
    group: 'block',
    content: 'gridColumn{2}',
    defining: true,
    draggable: true,

    addAttributes() {
        return {
            columns: { default: [6, 6] }
        };
    },

    parseHTML() {
        return [{ tag: 'div[data-type="gridBlock"]' }];
    },

    renderHTML({ HTMLAttributes }) {
        return ['div', mergeAttributes({ 'data-type': 'gridBlock' }, HTMLAttributes), 0];
    },

    addNodeView() {
        return () => {
            const dom = document.createElement('div');
            dom.setAttribute('data-type', 'gridBlock');
            dom.classList.add('grid-block');

            const contentDOM = document.createElement('div');
            contentDOM.classList.add('grid-block__grid');
            dom.appendChild(contentDOM);

            return { dom, contentDOM };
        };
    },

    addCommands() {
        return {
            insertGrid:
                () =>
                ({ commands }) =>
                    commands.insertContent({
                        type: 'gridBlock',
                        attrs: { columns: [6, 6] },
                        content: [
                            {
                                type: 'gridColumn',
                                attrs: { span: 6 },
                                content: [{ type: 'paragraph' }]
                            },
                            {
                                type: 'gridColumn',
                                attrs: { span: 6 },
                                content: [{ type: 'paragraph' }]
                            }
                        ]
                    })
        };
    }
});
