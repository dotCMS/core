import { TableHeader } from '@tiptap/extension-table-header';
import { mergeAttributes } from '@tiptap/core';

export function DotTableHeaderExtension() {
    return TableHeader.extend({
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
