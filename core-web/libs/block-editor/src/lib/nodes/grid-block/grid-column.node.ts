import { Node } from '@tiptap/core';

export const GridColumn = Node.create({
    name: 'gridColumn',
    group: 'gridColumn',
    // gridBlock is intentionally excluded to prevent nested grids.
    // Nested grid support requires a more robust resize implementation.
    content:
        '(paragraph | heading | bulletList | orderedList | codeBlock | blockquote | table | horizontalRule | dotImage | dotVideo | dotContent)+',
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
