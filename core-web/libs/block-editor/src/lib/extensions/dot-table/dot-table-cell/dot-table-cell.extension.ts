import { TableCell } from '@tiptap/extension-table-cell';

import { mergeAttributes } from '@tiptap/core';

import { ViewContainerRef } from '@angular/core';
import { DotMenuItem, popperModifiers, SuggestionsComponent } from '@dotcms/block-editor';
import { quoteIcon } from '../../../shared/components/suggestions/suggestion-icons';
import tippy, { Instance, Props } from 'tippy.js';

import { DotTableCellPlugin } from './dot-table-cell.plugin';

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        DotTableCellExtension: {
            setCellFocus: (attributes: { class: string }) => ReturnType;
        };
    }
}

export function DotTableCellExtension(viewContainerRef: ViewContainerRef) {
    let tippyCellOptions: Instance | undefined;

    return TableCell.extend({
        content: 'block',

        // onCreate() {
        //     console.log('-----table create----');
        //     const component = viewContainerRef.createComponent(SuggestionsComponent);
        //     const element = component.location.nativeElement;
        //     component.instance.currentLanguage = this.editor.storage.dotConfig.lang;
        //     // Load ChangeTo Options
        //
        //     cellOptions[0].command = () => {
        //         this.editor.commands.addRowBefore();
        //         tippyCellOptions.hide();
        //     };
        //     cellOptions[1].command = () => {
        //         this.editor.commands.addRowAfter();
        //         tippyCellOptions.hide();
        //     };
        //     cellOptions[2].command = () => {
        //         this.editor.commands.addColumnBefore();
        //         tippyCellOptions.hide();
        //     };
        //     cellOptions[3].command = () => {
        //         this.editor.commands.addColumnAfter();
        //         tippyCellOptions.hide();
        //     };
        //     cellOptions[4].command = () => {
        //         this.editor.commands.deleteRow();
        //         tippyCellOptions.hide();
        //     };
        //     cellOptions[5].command = () => {
        //         this.editor.commands.deleteColumn();
        //         tippyCellOptions.hide();
        //     };
        //     cellOptions[6].command = () => {
        //         this.editor.commands.deleteTable();
        //         tippyCellOptions.hide();
        //     };
        //     cellOptions[7].command = () => {
        //         this.editor.commands.toggleHeaderRow();
        //         tippyCellOptions.hide();
        //     };
        //     cellOptions[8].command = () => {
        //         this.editor.commands.toggleHeaderColumn();
        //         tippyCellOptions.hide();
        //     };
        //
        //     component.instance.items = cellOptions;
        //     component.instance.title = '';
        //     //this.changeToElement.remove();
        //     component.changeDetectorRef.detectChanges();
        //
        //     const defaultTippyOptions: Partial<Props> = {
        //         duration: 500,
        //         maxWidth: 'none',
        //         placement: 'top-start',
        //         trigger: 'manual',
        //         interactive: true
        //     };
        //
        //     const { element: editorElement } = this.editor.options;
        //     tippyCellOptions = tippy(editorElement, {
        //         ...defaultTippyOptions,
        //         appendTo: document.body,
        //         getReferenceClientRect: null,
        //         content: element,
        //         placement: 'bottom-start',
        //         duration: 0,
        //         hideOnClick: true,
        //         popperOptions: {
        //             modifiers: popperModifiers
        //         },
        //         onHide: () => {
        //             // this.editor.storage.bubbleMenu.changeToIsOpen = false;
        //             // this.changeTo.instance.items = [];
        //             // this.changeTo.changeDetectorRef.detectChanges();
        //         },
        //         onShow: () => {
        //             if (this.editor.can().mergeCells()) {
        //                 component.instance.items.push({
        //                     label: 'Merge Cells',
        //                     icon: quoteIcon,
        //                     id: 'addColumn',
        //                     command: () => {
        //                         this.editor.commands.mergeCells();
        //                     }
        //                 });
        //                 component.changeDetectorRef.detectChanges();
        //             }
        //             console.log('can merge', this.editor.can().mergeCells());
        //             console.log('can split', this.editor.can().splitCell());
        //         }
        //     });
        // },

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
