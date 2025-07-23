import { Plugin, PluginKey } from 'prosemirror-state';
import { Decoration, DecorationSet } from 'prosemirror-view';
import tippy, { Props } from 'tippy.js';

import { ViewContainerRef } from '@angular/core';

import { Extension } from '@tiptap/core';

import { getCellsOptions } from './utils';

import { SuggestionsComponent, popperModifiers } from '../../shared';

// DotTable Options
const TableCellContextMenuPlugin = (options) => {
    let tippyCellOptions;

    function setFocusDecoration(selection): Decoration {
        // get the before and after position of the parent cell where the selection is.
        return Decoration.node(selection.$to.before(3), selection.$to.after(3), {
            class: 'focus'
        });
    }

    function displayTableOptions(event: MouseEvent): void {
        event.preventDefault();
        event.stopPropagation();
        tippyCellOptions?.setProps({
            getReferenceClientRect: () => (event.target as HTMLElement).getBoundingClientRect()
        });
        tippyCellOptions.show();
    }

    function isArrowClicked(element: HTMLButtonElement): boolean {
        return element?.classList.contains('dot-cell-arrow');
    }

    function isNodeRelatedToTable(node): boolean {
        return (
            node?.type.name === 'tableCell' ||
            node?.type.name === 'tableHeader' ||
            node?.type.name === 'tableRow'
        );
    }

    return new Plugin({
        key: new PluginKey('dotTableCell'),
        state: {
            // eslint-disable-next-line
            apply: () => {},
            init: () => {
                const { editor, viewContainerRef } = options;
                const component = viewContainerRef.createComponent(SuggestionsComponent);
                const element = component.location.nativeElement;
                component.instance.currentLanguage = editor.storage.dotConfig.lang;

                const defaultTippyOptions: Partial<Props> = {
                    duration: 500,
                    maxWidth: 'none',
                    placement: 'top-start',
                    trigger: 'manual',
                    interactive: true
                };

                const { element: editorElement } = editor.options;
                tippyCellOptions = tippy(editorElement, {
                    ...defaultTippyOptions,
                    appendTo: document.body,
                    getReferenceClientRect: null,
                    content: element,
                    placement: 'bottom-start',
                    duration: 0,
                    hideOnClick: true,
                    popperOptions: {
                        modifiers: popperModifiers
                    },
                    onShow: () => {
                        editor.commands.freezeScroll(true);
                        const mergeCellsOption = component.instance.items.find(
                            (item) => item.id == 'mergeCells'
                        );
                        const splitCellsOption = component.instance.items.find(
                            (item) => item.id == 'splitCells'
                        );

                        mergeCellsOption.disabled = !editor.can().mergeCells();
                        splitCellsOption.disabled = !editor.can().splitCell();
                        setTimeout(() => {
                            component.changeDetectorRef.detectChanges();
                        });
                    },
                    onHide: () => editor.commands.freezeScroll(false)
                });

                component.instance.items = getCellsOptions(options.editor, tippyCellOptions);
                component.instance.title = '';
                component.changeDetectorRef.detectChanges();
            }
        },
        props: {
            decorations(state) {
                // Table cells deep is 3, this approach will work while we don't allow nested tables.
                const parentCell =
                    state.selection.$from.depth > 3 ? state.selection.$from.node(3) : null;

                if (
                    parentCell?.type?.name == 'tableCell' ||
                    parentCell?.type?.name == 'tableHeader'
                ) {
                    return DecorationSet.create(state.doc, [setFocusDecoration(state.selection)]);
                }

                return null;
            },

            handleDOMEvents: {
                contextmenu: (view, event) => {
                    const grandpaSelectedNode = view.state.selection.$from.node(
                        view.state.selection.$from.depth - 1
                    );

                    if (isNodeRelatedToTable(grandpaSelectedNode)) {
                        displayTableOptions(event);
                    }
                },

                mousedown: (view, event) => {
                    const grandpaSelectedNode = view.state.selection.$from.node(
                        view.state.selection.$from.depth - 1
                    );

                    if (isArrowClicked(event.target as HTMLButtonElement)) {
                        displayTableOptions(event);
                    } else if (event.button === 2 && isNodeRelatedToTable(grandpaSelectedNode)) {
                        event.preventDefault();
                    }
                }
            }
        }
    });
};

export const DotTableCellContextMenu = (viewContainerRef: ViewContainerRef) =>
    Extension.create({
        name: 'dotTableCellContextMenu',
        addProseMirrorPlugins() {
            return [
                TableCellContextMenuPlugin({
                    editor: this.editor,
                    viewContainerRef: viewContainerRef
                })
            ];
        }
    });
