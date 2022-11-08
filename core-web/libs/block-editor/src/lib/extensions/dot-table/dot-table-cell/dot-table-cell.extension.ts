import { TableCell } from '@tiptap/extension-table-cell';
import { Plugin, PluginKey } from 'prosemirror-state';
import { mergeAttributes } from '@tiptap/core';
import { CellSelection } from '@_ueberdosis/prosemirror-tables';
import { ViewContainerRef } from '@angular/core';
import {
    DotMenuItem,
    LINK_FORM_PLUGIN_KEY,
    popperModifiers,
    SuggestionsComponent
} from '@dotcms/block-editor';
import { quoteIcon } from '../../../shared/components/suggestions/suggestion-icons';
import tippy, { Instance, Props } from 'tippy.js';
import { element } from 'protractor';
import { state } from '@angular/animations';
import { Decoration, DecorationSet } from 'prosemirror-view';

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        DotTableCellExtension: {
            setCellFocus: (attributes: { class: string }) => ReturnType;
        };
    }
}

export function DotTableCellExtension(viewContainerRef: ViewContainerRef) {
    let focus = '';
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
            label: 'Delete Table',
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

    let tippyCellOptions: Instance | undefined;

    return TableCell.extend({
        content: 'block',
        editorProps: {
            handleClick: (data) => {
                console.log('handle click from editProprs ', data);
            }
        },

        onCreate() {
            console.log('-----table create----');
            const component = viewContainerRef.createComponent(SuggestionsComponent);
            const element = component.location.nativeElement;
            component.instance.currentLanguage = this.editor.storage.dotConfig.lang;
            // Load ChangeTo Options

            cellOptions[0].command = () => {
                this.editor.commands.addRowBefore();
                tippyCellOptions.hide();
            };
            cellOptions[1].command = () => {
                this.editor.commands.addRowAfter();
                tippyCellOptions.hide();
            };
            cellOptions[2].command = () => {
                this.editor.commands.addColumnBefore();
                tippyCellOptions.hide();
            };
            cellOptions[3].command = () => {
                this.editor.commands.addColumnAfter();
                tippyCellOptions.hide();
            };
            cellOptions[4].command = () => {
                this.editor.commands.deleteRow();
                tippyCellOptions.hide();
            };
            cellOptions[5].command = () => {
                this.editor.commands.deleteColumn();
                tippyCellOptions.hide();
            };
            cellOptions[6].command = () => {
                this.editor.commands.deleteTable();
                tippyCellOptions.hide();
            };
            cellOptions[7].command = () => {
                this.editor.commands.toggleHeaderRow();
                tippyCellOptions.hide();
            };
            cellOptions[8].command = () => {
                this.editor.commands.toggleHeaderColumn();
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

            const { element: editorElement } = this.editor.options;
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
                    // this.editor.storage.bubbleMenu.changeToIsOpen = true;
                    // this.updateChangeTo();
                }
            });
        },

        // addAttributes() {
        //     return {
        //         focus: { rendered: true }
        //     };
        // },

        addCommands() {
            return {
                setCellFocus: (attributes) => (all) => {
                    focus = 'focus';
                    return null;
                }
            };
        },

        addOptions() {
            return {
                HTMLAttributes: { class: focus },
                onFocus: (data) => {
                    console.log('focus', data);
                },
                onCreate({ editor }) {
                    console.log('-----------create---------');
                }
            };
        },

        onFocus({ event }) {
            // The editor is focused.
        },
        onTransaction({ transaction }) {
            // The editor state has changed.

            const focus = transaction.getMeta('dotTableCell');
            if (focus) {
                console.log('-------IS FOCUS------', focus);
            }

            if (transaction.selection instanceof CellSelection) {
                devicePixelRatio;
                console.log('cell selected');
            } else {
            }
        },

        onUpdate() {
            // general editor update
        },

        mouseover({ event }) {
            console.log('----------a');
        },
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
                new Plugin({
                    key: new PluginKey('dotTableCell'),

                    props: {
                        decorations(state) {
                            // get grandparent of the node.
                            const tableCell = state.selection.$from.node(
                                state.selection.$from.depth - 1
                            );

                            if (tableCell?.type?.name == 'tableCell') {
                                //tableCell node, since the textNode is the actual selection.
                                const decoration = Decoration.node(
                                    state.selection.from - 2,
                                    state.selection.to + 2,
                                    {
                                        class: 'focus'
                                    }
                                );
                                return DecorationSet.create(state.doc, [decoration]);
                            }

                            return null;
                        },

                        handleClickOn: (editorView, pos, node, nodePos, event) => {
                            // if (node.type.name === 'tableCell') {
                            //     // const resolved = editorView.state.doc.resolve(nodePos);
                            //     // DecorationSet.create(editorView.state.doc, [
                            //     //     Decoration.inline(resolved.before(), resolved.after(), {
                            //     //         class: 'beto'
                            //     //     })
                            //     // ]);
                            //     //
                            //     // const decoration = Decoration.node(
                            //     //     resolved.before(),
                            //     //     resolved.after(),
                            //     //     {
                            //     //         class: 'beto'
                            //     //     }
                            //     // );
                            //     //
                            //     // // editorView.dispatch(
                            //     // //     editorView.state.tr.setNodeAttribute(nodePos, 'beto', 'test')
                            //     // // );
                            //     // DecorationSet.create(editorView.state.doc, [decoration]);
                            //
                            //     console.log('table cell focus');
                            //     //   this.editor.chain().setCellFocus({ class: 'focus' }).run();
                            //     // this.setCellFocus();
                            //
                            //     // node.attrs = { ...node.attrs, class: 'focus' };
                            //     // node.console.log('tablecell');
                            //     // tr.setMeta(LINK_FORM_PLUGIN_KEY, { isOpen: true, openOnClick });
                            //     // editorView.state.tr.setMeta('dotTableCell', { focus: true });
                            //     // this.editor.commands.updateAttributes('tableCell', {
                            //     //     class: 'focus'
                            //     // });
                            //     // const tr = editorView.state.tr.setNodeAttribute(
                            //     //     nodePos + 1,
                            //     //     'focus',
                            //     //     ''
                            //     // );
                            //     //
                            //     // editorView.dispatch(tr);
                            // }
                        },

                        handleClick: (editorView, number, event) => {
                            if (
                                (event.target as HTMLButtonElement)?.classList.contains(
                                    'dot-cell-arrow'
                                )
                            ) {
                                tippyCellOptions?.setProps({
                                    getReferenceClientRect: () =>
                                        (event.target as HTMLElement).getBoundingClientRect()
                                });
                                tippyCellOptions.show();
                            }
                        },
                        handleDOMEvents: {
                            click(view, event) {
                                console.log('click handle DOM events');
                            },
                            mouseover(view, event) {
                                // const pos = view.posAtCoords({
                                //     left: event.clientX,
                                //     top: event.clientY
                                // });
                                // let node = view.domAtPos(pos.inside);
                                //
                                // console.log('moseOver', node);
                                // // console.log(
                                // //     `${node.node.nodeName} ==> ${node.node.parentNode.nodeName} `
                                // // );
                                // // console.log(node.node.nodeName === ('TD' || 'TH'));
                                //
                                // const newNode = !!node.node.nodeName.match(/^(TH|TD)$/)
                                //     ? node.node
                                //     : !!node.node.parentNode.nodeName.match(/^(TH|TD)$/)
                                //     ? node.node.parentNode
                                //     : null;
                                // if (newNode) {
                                //     debugger;
                                //     (newNode as HTMLElement).classList.add('hover');
                                //     console.log('new Node', newNode?.nodeName);
                                //
                                //     // console.log('view.nodeDOM parent', node?.node.parentNode.nodeName);
                                //     // console.log('view.nodeDOM', node?.node.nodeName);
                                //
                                //     if (newNode) {
                                //         // console.log('arrow');
                                //         console.log('es un TH: ', newNode);
                                //         // console.log(
                                //         //     (
                                //         //         newNode as HTMLTableCellElement
                                //         //     ).getBoundingClientRect()
                                //         // );
                                //         // console.log((newNode as HTMLTableCellElement).clientTop);
                                //     }
                                // console.log(
                                //     'parent',
                                //     findParentNode(view.state.selection.$from, [
                                //         NodeTypes.TABLE_CELL
                                //     ])?.type.name === NodeTypes.TABLE_CELL
                                // );
                            },
                            mouseleave(view, event) {
                                //   console.log('mouse leave');
                            }
                        }
                    }
                })
            ];
        }
    });
}
