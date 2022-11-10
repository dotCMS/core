import { Editor } from '@tiptap/core';
import { quoteIcon } from '../../../shared/components/suggestions/suggestion-icons';
import { Instance } from 'tippy.js';

export const getCellsOptions = (editor: Editor, tippy: Instance) => {
    return [
        {
            label: 'Insert row above',
            icon: quoteIcon,
            id: 'insertAbove',
            command: () => {
                editor.commands.addRowBefore();
                tippy.hide();
            }
        },
        {
            label: 'Insert row below',
            icon: quoteIcon,
            id: 'insertBellow',
            command: () => {
                editor.commands.addRowAfter();
                tippy.hide();
            }
        },
        {
            label: 'Insert column left',
            icon: quoteIcon,
            id: 'insertLeft',
            command: () => {
                editor.commands.addColumnBefore();
                tippy.hide();
            }
        },
        {
            label: 'Insert column right',
            icon: quoteIcon,
            id: 'inertRight',
            command: () => {
                editor.commands.addColumnAfter();
                tippy.hide();
            }
        },
        {
            label: 'Delete row',
            icon: quoteIcon,
            id: 'deleteRow',
            command: () => {
                editor.commands.deleteRow();
                tippy.hide();
            }
        },
        {
            label: 'Delete Column',
            icon: quoteIcon,
            id: 'deleteColumn',
            command: () => {
                editor.commands.deleteColumn();
                tippy.hide();
            }
        },
        {
            label: 'Toggle row Header',
            icon: quoteIcon,
            id: 'toggleRowHeader',
            command: () => {
                editor.commands.toggleHeaderRow();
                tippy.hide();
            }
        },
        {
            label: 'Toggle column Header',
            icon: quoteIcon,
            id: 'toggleColumnHeader',
            command: () => {
                editor.commands.toggleHeaderColumn();
                tippy.hide();
            }
        }
    ];
};
