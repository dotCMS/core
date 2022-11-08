import { Injector } from '@angular/core';
import { Node, mergeAttributes, NodeViewRenderer } from '@tiptap/core';
import { TableCell } from '@tiptap/extension-table-cell';
import { AngularNodeViewRenderer } from '@dotcms/block-editor';
import { DotTableCellComponent } from './components/dot-table-cell.component';
import { Plugin } from 'prosemirror-state';

export const DotTableCellNode = (injector: Injector): Node<any> => {
    return TableCell.extend({
        // addOptions() {
        //     return {
        //         HTMLAttributes: { data: 'test' }
        //     };
        // },

        // addNodeView(): NodeViewRenderer {
        //     return AngularNodeViewRenderer(DotTableCellComponent, { injector });
        // },

        onFocus({ event }) {
            // The editor is focused.
            //   console.log('cell focus');
        },
        onTransaction({ transaction }) {
            // The editor state has changed.
            //  console.log('dot table cell: ', transaction.selection);
        },

        mouseover({ event }) {
            console.log('----------a');
        },
        // renderHTML({ HTMLAttributes }) {
        //     return [
        //         'td',
        //         mergeAttributes(this.options.HTMLAttributes, HTMLAttributes),
        //         [
        //             'button',
        //             {
        //                 class: 'dot-cell-arrow',
        //                 click: () => {
        //                     console.log('test click');
        //                 },
        //                 value: 'Test'
        //             }
        //         ],
        //         ['textNode', 0]
        //     ];
        // },

        addProseMirrorPlugins() {
            const cellArrow = document.createElement('button');
            cellArrow.value = 'button';

            return [
                new Plugin({
                    props: {
                        handleDOMEvents: {
                            click(view, event) {
                                console.log(
                                    'click NEW ',
                                    (event.target as HTMLButtonElement).tagName === 'button'
                                );
                            },
                            mouseover(view, event) {
                                //console.log('view mouse over', view);
                                // console.log('encima de una celda');
                                // console.log('view', view.state);
                                //  console.log('event', event);
                                //
                                const pos = view.posAtCoords({
                                    left: event.clientX,
                                    top: event.clientY
                                });
                                let node = view.domAtPos(pos.pos);

                                // console.log(
                                //     `${node.node.nodeName} ==> ${node.node.parentNode.nodeName} `
                                // );
                                // console.log(node.node.nodeName === ('TD' || 'TH'));
                                node.offset;
                                const newNode = !!node.node.nodeName.match(/^(TH|TR)$/)
                                    ? node.node
                                    : !!node.node.parentNode.nodeName.match(/^(TH|TR)$/)
                                    ? node.node.parentNode
                                    : null;

                                if (newNode) {
                                    console.log('new Node', newNode?.nodeName);

                                    // console.log('view.nodeDOM parent', node?.node.parentNode.nodeName);
                                    // console.log('view.nodeDOM', node?.node.nodeName);

                                    if (newNode) {
                                        console.log('arrow');
                                        console.log('es un TH: ', newNode);
                                        console.log(
                                            (
                                                newNode as HTMLTableCellElement
                                            ).getBoundingClientRect()
                                        );
                                        console.log((newNode as HTMLTableCellElement).clientTop);
                                    }
                                }

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
};
