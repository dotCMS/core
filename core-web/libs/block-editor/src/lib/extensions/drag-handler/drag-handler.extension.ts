import { ViewContainerRef } from '@angular/core';
import { EditorView } from 'prosemirror-view';
import { NodeSelection, Plugin, PluginKey } from 'prosemirror-state';
import { Extension } from '@tiptap/core';

import { DragHandlerComponent } from './drag-handler.component';
import { deselectCurrentNode } from '@dotcms/block-editor';

export const DragHandler = (viewContainerRef: ViewContainerRef) => {
    return Extension.create({
        name: 'dragHandler',

        addProseMirrorPlugins() {
            let nodeToBeDragged = null;
            const HANDLER_LEFT_OFFSET = 25;
            const HANDLER_GAP = 50;
            const dragHandler =
                viewContainerRef.createComponent(DragHandlerComponent).location.nativeElement;

            function getPositon(container, node) {
                const top =
                    node.getBoundingClientRect().top - container.getBoundingClientRect().top;
                const left =
                    node.getBoundingClientRect().left - container.getBoundingClientRect().left;

                return { top, left };
            }

            function removeNode(node) {
                if (node && node.parentNode) {
                    node.parentNode.removeChild(node);
                }
            }

            function blockPosAtCoords(coords, view) {
                const pos = view.posAtCoords(coords);
                if (pos) {
                    const node = getDirectChild(view.nodeDOM(pos.inside));
                    if (node && node.nodeType === 1) {
                        const desc = view.docView.nearestDesc(node, true);
                        if (!(!desc || desc === view.docView)) {
                            return desc.posBefore;
                        }
                    }
                }

                return null;
            }

            function dragStart(e, view: EditorView) {
                if (!e.dataTransfer) return;
                const coords = { left: e.clientX + HANDLER_GAP, top: e.clientY };
                const pos = blockPosAtCoords(coords, view);
                if (pos != null) {
                    view.dispatch(
                        view.state.tr.setSelection(NodeSelection.create(view.state.doc, pos))
                    );
                    const slice = view.state.selection.content();
                    e.dataTransfer.clearData();
                    e?.dataTransfer?.setDragImage(nodeToBeDragged, 10, 10);
                    view.dragging = { slice, move: true };
                }
            }

            // Get the direct child of the Editor. To cover cases when the user is hovering nested nodes.
            function getDirectChild(node) {
                while (node && node.parentNode) {
                    if (
                        node.classList?.contains('ProseMirror') ||
                        node.parentNode.classList?.contains('ProseMirror')
                    ) {
                        break;
                    }

                    node = node.parentNode;
                }

                return node;
            }

            // Check if node has content and is not an empty <p>. To show the handler.
            function nodeHasContent(view: EditorView, positon: number): boolean {
                const node = view.nodeDOM(positon);

                return (
                    !!node?.hasChildNodes() &&
                    !(node.childNodes.length === 1 && node.childNodes[0].nodeName == 'BR')
                );
            }

            function bindEventsToDragHandler(editorView: EditorView) {
                dragHandler.setAttribute('draggable', 'true');
                dragHandler.addEventListener('dragstart', (e) => dragStart(e, editorView));
                dragHandler.classList.remove('visible');
                editorView.dom.parentElement.appendChild(dragHandler);
            }

            function hanlderScroll() {
                dragHandler.classList.remove('visible');
            }

            function canDragNode(node): boolean {
                return (
                    node &&
                    !node.classList?.contains('ProseMirror') && // is not root node.
                    !node.innerText.startsWith('/') // The suggestion is not open.
                );
            }

            function hideDragHandler(): void {
                const node = document.querySelector('.ProseMirror-hideselection');
                node?.classList.remove('ProseMirror-hideselection');
                dragHandler.classList.remove('visible');
            }

            return [
                new Plugin({
                    key: new PluginKey('dragHandler'),
                    view: (editorView) => {
                        // Called before the browser performs the next repaint
                        requestAnimationFrame(() => bindEventsToDragHandler(editorView));
                        // We need to also react to page scrolling.
                        document.body.addEventListener('scroll', hanlderScroll, true);

                        return {
                            destroy() {
                                removeNode(dragHandler);
                                document.body.removeEventListener('scroll', hanlderScroll, true);
                            }
                        };
                    },
                    props: {
                        handleDOMEvents: {
                            drop(view: EditorView, dragEvent: DragEvent) {
                                const directChildNode = getDirectChild(dragEvent.target);
                                // Disable the drop in the table node;
                                if (directChildNode.nodeName === 'TABLE') {
                                    return true;
                                }

                                requestAnimationFrame(() => {
                                    hideDragHandler();
                                    deselectCurrentNode(view);
                                    // remove table node because prosmirror duplicate the node on D&D
                                    // https://github.com/ueberdosis/tiptap/issues/2250
                                    if (nodeToBeDragged.nodeName === 'TABLE') {
                                        removeNode(nodeToBeDragged);
                                    }
                                });

                                return false;
                            },
                            mousemove(view, event: MouseEvent) {
                                const coords = {
                                    left: event.clientX + HANDLER_GAP,
                                    top: event.clientY
                                };
                                const position = view.posAtCoords(coords);
                                if (position && nodeHasContent(view, position.inside)) {
                                    nodeToBeDragged = getDirectChild(view.nodeDOM(position.inside));
                                    if (canDragNode(nodeToBeDragged)) {
                                        const { top, left } = getPositon(
                                            view.dom.parentElement,
                                            nodeToBeDragged
                                        );

                                        dragHandler.style.left = left - HANDLER_LEFT_OFFSET + 'px';
                                        dragHandler.style.top = top < 0 ? 0 : top + 'px';
                                        dragHandler.classList.add('visible');
                                    } else {
                                        dragHandler.classList.remove('visible');
                                    }
                                } else {
                                    nodeToBeDragged = null;
                                    dragHandler.classList.remove('visible');
                                }

                                return false;
                            }
                        }
                    }
                })
            ];
        }
    });
};
