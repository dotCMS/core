import { Node } from '@tiptap/core';

export const GridColumn = Node.create({
    name: 'gridColumn',
    group: 'gridColumn',
    content: 'block+',
    isolating: true,

    parseHTML() {
        return [{ tag: 'div[data-type="gridColumn"]' }];
    },

    renderHTML({ HTMLAttributes }) {
        return [
            'div',
            { ...HTMLAttributes, 'data-type': 'gridColumn', class: 'grid-block__column' },
            0
        ];
    }
});
