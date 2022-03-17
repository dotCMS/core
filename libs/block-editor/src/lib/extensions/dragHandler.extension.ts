import { Extension } from '@tiptap/core';
import { NodeSelection, Plugin, PluginKey } from 'prosemirror-state';
import { EditorView } from 'prosemirror-view';
import { ViewContainerRef } from '@angular/core';
import { DragHandlerComponent } from './components/drag-handler/drag-handler.component';

export const DragHandler = (viewContainerRef: ViewContainerRef) => {
    return Extension.create({
        name: 'dragHandler',

        addProseMirrorPlugins() {
            let nodeToBeDragged = null;
            const WIDTH = 24;
            const HANDLER_GAP = 50;
            const dragHandler =
                viewContainerRef.createComponent(DragHandlerComponent).location.nativeElement;

            function createRect(rect) {
                if (rect == null) {
                    return null;
                }
                const newRect = {
                    left: rect.left + document.body.scrollLeft,
                    top: rect.top + document.body.scrollTop,
                    width: rect.width,
                    height: rect.height,
                    bottom: 0,
                    right: 0
                };
                newRect.bottom = newRect.top + newRect.height;
                newRect.right = newRect.left + newRect.width;
                return newRect;
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
                    e.dataTransfer.setDragImage(nodeToBeDragged, 10, 10);
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
                dragHandler.style.display = 'none';
                document.body.appendChild(dragHandler);
            }

            return [
                new Plugin({
                    key: new PluginKey('dragHandler'),
                    view: (editorView) => {
                        bindEventsToDragHandler(editorView);
                        return {
                            destroy() {
                                removeNode(dragHandler);
                            }
                        };
                    },
                    props: {
                        handleDOMEvents: {
                            drop() {
                                setTimeout(() => {
                                    const node = document.querySelector(
                                        '.ProseMirror-hideselection'
                                    );
                                    if (node) {
                                        node.classList.remove('ProseMirror-hideselection');
                                    }
                                    dragHandler.style.display = 'none';
                                });
                                return false;
                            },
                            mousemove(view, event) {
                                const coords = {
                                    left: event.clientX + HANDLER_GAP,
                                    top: event.clientY
                                };
                                const position = view.posAtCoords(coords);
                                if (position && nodeHasContent(view, position.inside)) {
                                    nodeToBeDragged = getDirectChild(view.nodeDOM(position.inside));
                                    if (
                                        nodeToBeDragged &&
                                        !nodeToBeDragged.classList?.contains('ProseMirror')
                                    ) {
                                        const rect = createRect(
                                            nodeToBeDragged.getBoundingClientRect()
                                        );
                                        const win = nodeToBeDragged.ownerDocument.defaultView;
                                        rect.top += win.pageYOffset;
                                        rect.left += win.pageXOffset;
                                        dragHandler.style.left = rect.left - WIDTH + 'px';
                                        dragHandler.style.top = rect.top - 4 + 'px';
                                        dragHandler.style.display = 'block';
                                    } else {
                                        dragHandler.style.display = 'none';
                                    }
                                } else {
                                    nodeToBeDragged = null;
                                    dragHandler.style.display = 'none';
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
