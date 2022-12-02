import { Table } from '@tiptap/extension-table';

export function DotTableExtension() {
    return Table.extend({
        resizable: true
    });
}
