import { Table } from '@tiptap/extension-table';
import { ViewContainerRef } from '@angular/core';

export function DotTableExtension(viewContainerRef: ViewContainerRef) {
    return Table.extend({
        resizable: true
    });
}
