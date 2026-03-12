import { mergeAttributes } from '@tiptap/core';
import { TableKit } from '@tiptap/extension-table';
import TableCell from '@tiptap/extension-table-cell';
import TableHeader from '@tiptap/extension-table-header';

const DotTable = TableKit.configure({
    table: { resizable: true },
    // Exclude the base TableHeader and TableCell since we're using custom ones
    tableHeader: false,
    tableCell: false
});

const DotTableHeader = TableHeader.extend({
    renderHTML({ HTMLAttributes }) {
        return [
            'th',
            mergeAttributes(this.options.HTMLAttributes, HTMLAttributes),
            ContextualMenuButton,
            ['p', 0]
        ];
    }
});

const ContextualMenuButton = ['button', { class: 'dot-cell-arrow' }];

const DotTableCell = TableCell.extend({
    renderHTML({ HTMLAttributes }) {
        return [
            'td',
            mergeAttributes(this.options.HTMLAttributes, HTMLAttributes),
            ContextualMenuButton,
            ['p', 0]
        ];
    }
});

export const DotCMSTableExtensions = [DotTable, DotTableHeader, DotTableCell];
