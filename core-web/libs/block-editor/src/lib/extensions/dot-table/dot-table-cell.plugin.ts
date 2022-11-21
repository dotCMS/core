import { EditorState, Plugin, PluginKey } from 'prosemirror-state';
import { Decoration, DecorationSet, EditorView } from 'prosemirror-view';

import { Editor } from '@tiptap/core';
import tippy, { Instance, Props } from 'tippy.js';
import { ComponentRef } from '@angular/core';
import { popperModifiers, SuggestionsComponent } from '@dotcms/block-editor';
import { getCellsOptions } from './utils';

class DotTableCellPluginView {
    public editor: Editor;
    public element: HTMLElement;
    public view: EditorView;
    public tippy: Instance | undefined;
    public tippyOptions?: Partial<Props>;
    public pluginKey: PluginKey;
    public component?: ComponentRef<SuggestionsComponent>;

    constructor(view, tippy) {
        this.view = view;
        this.tippy = tippy;
    }

    init(): void {}

    update(view: EditorView, prevState?: EditorState): void {}

    destroy() {
        this.tippy.destroy();
    }
}

export const DotTableCellPlugin = (options) => {
    let tippyCellOptions;

    // dynamic selection to capture table cells, works with text nodes.
    function setFocusDecoration(selection, node): Decoration {
        return Decoration.node(
            selection.from - (selection.$from.parentOffset + 2),
            selection.to + (node.textContent.length - selection.$to.parentOffset + 2),
            {
                class: 'focus'
            }
        );
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

    return new Plugin({
        key: new PluginKey('dotTableCell'),
        state: {
            apply: (tr) => {},
            init: (config, state) => {
                const component = options.viewContainerRef.createComponent(SuggestionsComponent);
                const element = component.location.nativeElement;
                component.instance.currentLanguage = options.editor.storage.dotConfig.lang;

                const defaultTippyOptions: Partial<Props> = {
                    duration: 500,
                    maxWidth: 'none',
                    placement: 'top-start',
                    trigger: 'manual',
                    interactive: true
                };

                const { element: editorElement } = options.editor.options;
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
                    onHide: () => {},
                    onShow: () => {
                        const mergeCellsOption = component.instance.items.find(
                            (item) => item.id == 'mergeCells'
                        );
                        const splitCellsOption = component.instance.items.find(
                            (item) => item.id == 'splitCells'
                        );

                        mergeCellsOption.disabled = !options.editor.can().mergeCells();
                        splitCellsOption.disabled = !options.editor.can().splitCell();
                        setTimeout(() => {
                            component.changeDetectorRef.detectChanges();
                        });
                    }
                });

                component.instance.items = getCellsOptions(options.editor, tippyCellOptions);
                component.instance.title = '';
                //this.changeToElement.remove();
                component.changeDetectorRef.detectChanges();
            }
        },

        view: (view) => new DotTableCellPluginView(view, tippyCellOptions),
        props: {
            decorations(state) {
                // get grandparent of the state selection.
                const grandpaSelectedNode = state.selection.$from.node(
                    state.selection.$from.depth - 1
                );
                if (
                    grandpaSelectedNode?.type?.name == 'tableCell' ||
                    grandpaSelectedNode?.type?.name == 'tableHeader'
                ) {
                    return DecorationSet.create(state.doc, [
                        setFocusDecoration(state.selection, grandpaSelectedNode)
                    ]);
                }

                return null;
            },

            handleDOMEvents: {
                contextmenu: (view, event) => {
                    const grandpaSelectedNode = view.state.selection.$from.node(
                        view.state.selection.$from.depth - 1
                    );

                    if (
                        grandpaSelectedNode.type.name === 'tableCell' ||
                        grandpaSelectedNode.type.name === 'tableHeader' ||
                        grandpaSelectedNode.type.name === 'tableRow'
                    ) {
                        displayTableOptions(event);
                    }
                },

                mousedown: (view, event) => {
                    const grandpaSelectedNode = view.state.selection.$from.node(
                        view.state.selection.$from.depth - 1
                    );

                    if (isArrowClicked(event.target as HTMLButtonElement)) {
                        displayTableOptions(event);
                    } else if (
                        event.button === 2 &&
                        (grandpaSelectedNode.type.name === 'tableCell' ||
                            grandpaSelectedNode.type.name === 'tableHeader' ||
                            grandpaSelectedNode.type.name === 'tableRow')
                    ) {
                        event.preventDefault();
                    }
                }
            }
        }
    });
};
