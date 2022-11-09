import { Table } from '@tiptap/extension-table';
import { DotMenuItem, popperModifiers, SuggestionsComponent } from '@dotcms/block-editor';
import { ViewContainerRef } from '@angular/core';
import { quoteIcon } from '../../shared/components/suggestions/suggestion-icons';
import tippy, { Instance, Props } from 'tippy.js';
import { DotTableCellPlugin } from './dot-table-cell/dot-table-cell.plugin';

export function DotTableExtension(viewContainerRef: ViewContainerRef) {
    let tippyCellOptions: Instance | undefined;

    return Table.extend({
        resizable: true,

        onCreate() {}
    });
}
