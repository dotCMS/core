import { mergeAttributes } from '@tiptap/core';
import { TableHeader } from '@tiptap/extension-table-header';

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
