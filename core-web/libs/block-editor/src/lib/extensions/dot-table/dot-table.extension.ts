import { Table } from '@tiptap/extension-table';
import { DotMenuItem, popperModifiers, SuggestionsComponent } from '@dotcms/block-editor';
import { ViewContainerRef } from '@angular/core';
import { quoteIcon } from '../../shared/components/suggestions/suggestion-icons';
import tippy, { Instance, Props } from 'tippy.js';

const cellOptions: DotMenuItem[] = [
    {
        label: 'Delete Column',
        icon: quoteIcon,
        id: 'deleteColumn'
    },
    {
        label: 'Add Column',
        icon: quoteIcon,
        id: 'addColumn'
    }
];

export function DotTableExtension(viewContainerRef: ViewContainerRef) {
    let tippyCellOptions: Instance | undefined;

    return Table.extend({
        resizable: true
        // onCreate() {
        //     console.log('---------EDITOR CREATION--------');
        //     const cellsMenu = viewContainerRef.createComponent(SuggestionsComponent);
        //     const element = cellsMenu.location.nativeElement;
        //     cellsMenu.instance.currentLanguage = this.editor.storage.dotConfig.lang;
        //
        //     cellOptions[0].command = () => {
        //         this.editor.commands.deleteColumn();
        //         tippyCellOptions.hide();
        //     };
        //     cellOptions[1].command = () => {
        //         this.editor.commands.addColumnBefore();
        //         tippyCellOptions.hide();
        //     };
        //
        //     cellsMenu.instance.items = cellOptions;
        //     cellsMenu.instance.title = '';
        //     cellsMenu.changeDetectorRef.detectChanges();
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
        //         hideOnClick: false,
        //         popperOptions: {
        //             modifiers: popperModifiers
        //         },
        //         onHide: () => {
        //             // this.editor.storage.bubbleMenu.changeToIsOpen = false;
        //             // this.changeTo.instance.items = [];
        //             // this.changeTo.changeDetectorRef.detectChanges();
        //         },
        //         onShow: () => {
        //             // this.editor.storage.bubbleMenu.changeToIsOpen = true;
        //             // this.updateChangeTo();
        //         }
        //     });
        // }
    });
}
