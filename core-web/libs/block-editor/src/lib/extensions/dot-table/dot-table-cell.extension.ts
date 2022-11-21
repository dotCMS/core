import { TableCell } from '@tiptap/extension-table-cell';
import { mergeAttributes } from '@tiptap/core';
import { ViewContainerRef } from '@angular/core';
import { DotTableCellPlugin } from './dot-table-cell.plugin';

export function DotTableCellExtension(viewContainerRef: ViewContainerRef) {
    return TableCell.extend({
        renderHTML({ HTMLAttributes }) {
            return [
                'td',
                mergeAttributes(this.options.HTMLAttributes, HTMLAttributes),
                [
                    'button',
                    {
                        class: 'dot-cell-arrow'
                    }
                ],
                ['p', 0]
            ];
        },

        addProseMirrorPlugins() {
            return [
                DotTableCellPlugin({ editor: this.editor, viewContainerRef: viewContainerRef })
            ];
        }
    });
}
