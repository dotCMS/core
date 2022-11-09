import { TableHeader } from '@tiptap/extension-table-header';
import { mergeAttributes } from '@tiptap/core';
import { DotTableCellPlugin } from './dot-table-cell/dot-table-cell.plugin';

export function DotTableHeaderExtension() {
    return TableHeader.extend({
        content: 'block',

        renderHTML({ HTMLAttributes }) {
            return [
                'th',
                mergeAttributes(this.options.HTMLAttributes, HTMLAttributes),
                [
                    'button',
                    {
                        class: 'dot-cell-arrow'
                    }
                ],
                ['p', 0]
            ];
        }
    });
}
