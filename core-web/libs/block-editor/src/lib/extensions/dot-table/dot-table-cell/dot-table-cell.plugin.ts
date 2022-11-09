import { EditorState, Plugin, PluginKey, NodeSelection } from 'prosemirror-state';
import { Decoration, DecorationSet, EditorView } from 'prosemirror-view';
import { update } from 'lodash';
import { BubbleLinkFormViewProps } from '../../bubble-link-form/plugins/bubble-link-form.plugin';
import { Editor } from '@tiptap/core';
import tippy, { Instance, Props } from 'tippy.js';
import { ComponentRef } from '@angular/core';
import { DotMenuItem, popperModifiers, SuggestionsComponent } from '@dotcms/block-editor';
import { quoteIcon } from '../../../shared/components/suggestions/suggestion-icons';

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

const cellOptions: DotMenuItem[] = [
    {
        label: 'Insert row above',
        icon: quoteIcon,
        id: 'deleteColumn'
    },
    {
        label: 'Insert row below',
        icon: quoteIcon,
        id: 'addColumn'
    },
    {
        label: 'Insert column left',
        icon: quoteIcon,
        id: 'deleteColumn'
    },
    {
        label: 'Insert column right',
        icon: quoteIcon,
        id: 'addColumn'
    },
    {
        label: 'Delete row',
        icon: quoteIcon,
        id: 'deleteColumn'
    },
    {
        label: 'Delete Column',
        icon: quoteIcon,
        id: 'addColumn'
    },
    {
        label: 'Toggle row Header',
        icon: quoteIcon,
        id: 'addColumn'
    },
    {
        label: 'Toggle column Header',
        icon: quoteIcon,
        id: 'addColumn'
    }
];

class DotTableCellPluginView {
    public editor: Editor;
    public element: HTMLElement;
    public view: EditorView;
    public tippy: Instance | undefined;
    public tippyOptions?: Partial<Props>;
    public pluginKey: PluginKey;
    public component?: ComponentRef<SuggestionsComponent>;

    constructor(view) {
        // this.editor = editor;
        // this.element = element;
        // this.view = view;
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
        console.log('view updated');
        //debugger;
    }

    destroy() {}
}

export const DotTableCellPlugin = (options) => {
    let tippyCellOptions: Instance | undefined;

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

                console.log('----table create----');
                const component = options.viewContainerRef.createComponent(SuggestionsComponent);
                const element = component.location.nativeElement;
                component.instance.currentLanguage = options.editor.storage.dotConfig.lang;
                cellOptions[0].command = () => {
                    options.editor.commands.addRowBefore();
                    tippyCellOptions.hide();
                };
                cellOptions[1].command = () => {
                    options.editor.commands.addRowAfter();
                    tippyCellOptions.hide();
                };
                cellOptions[2].command = () => {
                    options.editor.commands.addColumnBefore();
                    tippyCellOptions.hide();
                };
                cellOptions[3].command = () => {
                    options.editor.commands.addColumnAfter();
                    tippyCellOptions.hide();
                };
                cellOptions[4].command = () => {
                    options.editor.commands.deleteRow();
                    tippyCellOptions.hide();
                };
                cellOptions[5].command = () => {
                    options.editor.commands.deleteColumn();
                    tippyCellOptions.hide();
                };

                cellOptions[6].command = () => {
                    options.editor.commands.toggleHeaderRow();
                    tippyCellOptions.hide();
                };

                cellOptions[7].command = () => {
                    options.editor.commands.toggleHeaderColumn();
                    tippyCellOptions.hide();
                };

                component.instance.items = cellOptions;
                component.instance.title = '';
                //this.changeToElement.remove();
                component.changeDetectorRef.detectChanges();

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
                    onHide: () => {
                        // this.editor.storage.bubbleMenu.changeToIsOpen = false;
                        // this.changeTo.instance.items = [];
                        // this.changeTo.changeDetectorRef.detectChanges();
                    },
                    onShow: () => {
                        if (options.editor.can().mergeCells()) {
                            component.instance.items.push({
                                label: 'Merge Cells',
                                icon: quoteIcon,
                                id: 'addColumn',
                                command: () => {
                                    options.editor.commands.mergeCells();
                                }
                            });
                            component.changeDetectorRef.detectChanges();
                        }
                        console.log('can merge', options.editor.can().mergeCells());
                        console.log('can split', options.editor.can().splitCell());
                    }
                });
            },
            apply: (tr, value, oldState, newState) => {
                console.log('----apply-----', tr.getMeta(''));
            }
        },
        // view: (view) => new DotTableCellPluginView(view),
        view: (view) => new DotTableCellPluginView(view),
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
                    displayTableOptions(event);
                }
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
                        event.preventDefault();
                        event.stopPropagation();
                        displayTableOptions(event);
                    }
                }
            }
        }
    });
};
