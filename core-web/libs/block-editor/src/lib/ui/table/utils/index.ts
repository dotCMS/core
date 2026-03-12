import { Editor } from '@tiptap/core';

import { DotMenuItem } from '../../../shared';

/** Any floating menu instance that can be hidden (Floating UI or legacy). */
export interface TableCellMenuRef {
    hide(): void;
}

export const getCellsOptions = (editor: Editor, cellMenuRef: TableCellMenuRef) => {
    const menu: DotMenuItem[] = [
        {
            label: 'Toggle row Header',
            icon: 'check',
            id: 'toggleRowHeader',
            command: () => {
                editor.commands.toggleHeaderRow();
                cellMenuRef.hide();
            },
            tabindex: '0'
        },
        {
            label: 'Toggle column Header',
            icon: 'check',
            id: 'toggleColumnHeader',
            command: () => {
                editor.commands.toggleHeaderColumn();
                cellMenuRef.hide();
            },
            tabindex: '1'
        },
        {
            id: 'divider'
        },
        {
            label: 'Merge Cells',
            icon: 'call_merge',
            id: 'mergeCells',
            command: () => {
                editor.commands.mergeCells();
                cellMenuRef.hide();
            },
            disabled: true,
            tabindex: '2'
        },
        {
            label: 'Split Cells',
            icon: 'call_split',
            id: 'splitCells',
            command: () => {
                editor.commands.splitCell();
                cellMenuRef.hide();
            },
            disabled: true,
            tabindex: '3'
        },

        {
            id: 'divider'
        },
        {
            label: 'Insert row above',
            icon: 'arrow_upward',
            id: 'insertAbove',
            command: () => {
                editor.commands.addRowBefore();
                cellMenuRef.hide();
            },
            tabindex: '4'
        },
        {
            label: 'Insert row below',
            icon: 'arrow_downward',
            id: 'insertBellow',
            command: () => {
                editor.commands.addRowAfter();
                cellMenuRef.hide();
            },
            tabindex: '5'
        },
        {
            label: 'Insert column left',
            icon: 'arrow_back',
            id: 'insertLeft',
            command: () => {
                editor.commands.addColumnBefore();
                cellMenuRef.hide();
            },
            tabindex: '6'
        },
        {
            label: 'Insert column right',
            icon: 'arrow_forward',
            id: 'insertRight',
            command: () => {
                editor.commands.addColumnAfter();
                cellMenuRef.hide();
            },
            tabindex: '7'
        },
        {
            id: 'divider'
        },
        {
            label: 'Delete row',
            icon: 'delete',
            id: 'deleteRow',
            command: () => {
                editor.commands.deleteRow();
                cellMenuRef.hide();
            },
            tabindex: '8'
        },
        {
            label: 'Delete Column',
            icon: 'delete',
            id: 'deleteColumn',
            command: () => {
                editor.commands.deleteColumn();
                cellMenuRef.hide();
            },
            tabindex: '9'
        },
        {
            label: 'Delete Table',
            icon: 'delete',
            id: 'deleteTable',
            command: () => {
                editor.commands.deleteTable();
                cellMenuRef.hide();
            },
            tabindex: '10'
        }
    ];

    return menu;
};
