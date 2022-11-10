import { EditorState, Plugin, PluginKey, NodeSelection } from 'prosemirror-state';
import { Decoration, DecorationSet, EditorView } from 'prosemirror-view';

import { Editor } from '@tiptap/core';
import tippy, { Instance, Props } from 'tippy.js';
import { ComponentRef } from '@angular/core';
import { popperModifiers, SuggestionsComponent } from '@dotcms/block-editor';
import { quoteIcon } from '../../shared/components/suggestions/suggestion-icons';
import { getCellsOptions } from './utils';
import { MenuItem } from '@dotcms/dotcms-js';

// export const DotTableCellPlugin = (options: any) => {
//     const component = options.component.instance;
//
//     return new Plugin<any>({
//         key: options.pluginKey as PluginKey,
//         state: {
//             init() {},
//             apply(tr, state) {
//                 const action = tr.getMeta(this);
//                 console.log(action);
//             }
//         }
//     });
//};

class DotTableCellPluginView {
    public editor: Editor;
    public element: HTMLElement;
    public view: EditorView;
    public tippy: Instance | undefined;
    public tippyOptions?: Partial<Props>;
    public pluginKey: PluginKey;
    public component?: ComponentRef<SuggestionsComponent>;

    constructor(view, tippy) {
        // this.editor = editor;
        // this.element = element;
        this.view = view;
        this.tippy = tippy;
        //
        // this.tippyOptions = tippyOptions;
        //
        // // Detaches menu content from its current parent
        // // this.element.remove();
        // // this.element.style.visibility = 'visible';
        // this.pluginKey = pluginKey;
        // this.component = component;
    }

    init(): void {
        console.log('INIT');
    }

    update(view: EditorView, prevState?: EditorState): void {
        // console.log('view updated');
        //debugger;
    }

    destroy() {
        console.log('---------DESTROY------');
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
        tippyCellOptions?.setProps({
            getReferenceClientRect: () => (event.target as HTMLElement).getBoundingClientRect()
        });
        tippyCellOptions.show();
    }

    return new Plugin({
        key: new PluginKey('dotTableCell'),
        state: {
            init: (config, state) => {
                console.log('-----INIT-----');

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
                        console.log('mergeCellsOption.disabled ', component.instance.items);
                        setTimeout(() => {
                            component.changeDetectorRef.detectChanges();
                        });
                    }
                });

                component.instance.items = getCellsOptions(options.editor, tippyCellOptions);
                component.instance.title = '';
                //this.changeToElement.remove();
                component.changeDetectorRef.detectChanges();
            },
            apply: (tr, value, oldState, newState) => {
                //console.log('----apply-----', tr.getMeta(''));
            }
        },
        // view: (view) => new DotTableCellPluginView(view),
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
                    console.log(grandpaSelectedNode?.type?.name);
                    return DecorationSet.create(state.doc, [
                        setFocusDecoration(state.selection, grandpaSelectedNode)
                    ]);
                }
                console.log(grandpaSelectedNode?.type?.name);
                return null;
            },

            handleClick: (editorView, number, event) => {
                if ((event.target as HTMLButtonElement)?.classList.contains('dot-cell-arrow')) {
                    event.preventDefault();
                    event.stopPropagation();
                    // debugger;
                    displayTableOptions(event);
                }
            },

            handleDOMEvents: {
                click: (view, event) => {},

                contextmenu: (view, event) => {
                    const grandpaSelectedNode = view.state.selection.$from.node(
                        view.state.selection.$from.depth - 1
                    );

                    if (
                        grandpaSelectedNode.type.name === 'tableCell' ||
                        grandpaSelectedNode.type.name === 'tableHeader' ||
                        grandpaSelectedNode.type.name === 'tableRow'
                    ) {
                        event.preventDefault();
                        event.stopPropagation();
                        displayTableOptions(event);
                    }
                }
            }
        }
    });
};
