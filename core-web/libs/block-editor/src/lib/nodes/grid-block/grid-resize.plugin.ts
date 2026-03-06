import { Editor } from '@tiptap/core';
import { Plugin, PluginKey } from '@tiptap/pm/state';
import { EditorView } from '@tiptap/pm/view';

export const GRID_RESIZE_PLUGIN_KEY = new PluginKey('gridResize');

/** Snap a drag ratio to the nearest 12-column grid split. */
function snapToGrid(ratio: number): number[] {
    const col1 = Math.min(11, Math.max(1, Math.round(ratio * 12)));

    return [col1, 12 - col1];
}

/**
 * Creates a preview overlay that sits on top of the grid block during drag.
 * This overlay has two colored regions showing the proposed column split,
 * completely bypassing ProseMirror's DOM management.
 */
function createPreviewOverlay(): HTMLDivElement {
    const overlay = document.createElement('div');
    overlay.className = 'grid-block__drag-preview';

    const left = document.createElement('div');
    left.className = 'grid-block__drag-preview-col';

    const right = document.createElement('div');
    right.className = 'grid-block__drag-preview-col';

    overlay.appendChild(left);
    overlay.appendChild(right);

    return overlay;
}

export function GridResizePlugin(editor: Editor): Plugin {
    let overlayContainer: HTMLDivElement | null = null;
    let currentHandles: { handle: HTMLDivElement; gridEl: HTMLElement }[] = [];
    let dragging = false;

    function getOrCreateOverlay(view: EditorView): HTMLDivElement {
        if (!overlayContainer) {
            overlayContainer = document.createElement('div');
            overlayContainer.className = 'grid-block__resize-overlay';
            overlayContainer.style.position = 'absolute';
            overlayContainer.style.top = '0';
            overlayContainer.style.left = '0';
            overlayContainer.style.width = '0';
            overlayContainer.style.height = '0';
            overlayContainer.style.overflow = 'visible';
            overlayContainer.style.pointerEvents = 'none';

            const editorParent = view.dom.parentElement;

            if (editorParent) {
                editorParent.style.position = 'relative';
                editorParent.appendChild(overlayContainer);
            }
        }

        return overlayContainer;
    }

    function removeAllHandles() {
        for (const { handle } of currentHandles) {
            handle.remove();
        }

        currentHandles = [];
    }

    function positionHandle(
        handle: HTMLDivElement,
        gridEl: HTMLElement,
        container: HTMLDivElement
    ) {
        const cols = gridEl.querySelectorAll<HTMLElement>(':scope > .grid-block__column');
        const firstCol = cols[0];
        const secondCol = cols[1];

        if (!firstCol) {
            return;
        }

        const containerRect = container.getBoundingClientRect();
        const gridRect = gridEl.getBoundingClientRect();
        const colRect = firstCol.getBoundingClientRect();

        const centerX = secondCol
            ? (colRect.right + secondCol.getBoundingClientRect().left) / 2
            : colRect.right;

        handle.style.left = `${centerX - containerRect.left - 6}px`;
        handle.style.top = `${gridRect.top - containerRect.top}px`;
        handle.style.height = `${gridRect.height}px`;
    }

    function findGridBlockPos(view: EditorView, gridEl: HTMLElement): number | null {
        const firstCol = gridEl.querySelector(':scope > .grid-block__column');

        if (!firstCol) {
            return null;
        }

        try {
            const pos = view.posAtDOM(firstCol, 0);
            const $pos = view.state.doc.resolve(pos);

            for (let d = $pos.depth; d >= 1; d--) {
                if ($pos.node(d).type.name === 'gridColumn') {
                    return $pos.before(d - 1);
                }
            }
        } catch {
            return null;
        }

        return null;
    }

    function syncHandles(view: EditorView) {
        if (dragging) {
            return;
        }

        removeAllHandles();

        if (!editor.isEditable) {
            return;
        }

        const container = getOrCreateOverlay(view);
        const gridBlocks = view.dom.querySelectorAll<HTMLElement>('.grid-block');

        gridBlocks.forEach((gridEl) => {
            const handle = document.createElement('div');
            handle.className = 'grid-block__resize-handle';
            handle.style.pointerEvents = 'auto';
            container.appendChild(handle);
            positionHandle(handle, gridEl, container);
            currentHandles.push({ handle, gridEl });
            attachDragListeners(handle, gridEl, view, container);
        });
    }

    function attachDragListeners(
        handle: HTMLDivElement,
        gridEl: HTMLElement,
        view: EditorView,
        container: HTMLDivElement
    ) {
        handle.addEventListener('pointerdown', (e: PointerEvent) => {
            e.preventDefault();
            e.stopPropagation();

            if (dragging) {
                return;
            }

            const gridBlockPos = findGridBlockPos(view, gridEl);

            if (gridBlockPos == null) {
                return;
            }

            const gridNode = view.state.doc.nodeAt(gridBlockPos);
            const storedColumns: number[] = gridNode?.attrs?.columns ?? [6, 6];
            const startRatio = storedColumns[0] / 12;

            const startX = e.clientX;
            const gridRect = gridEl.getBoundingClientRect();
            const gap = parseFloat(getComputedStyle(gridEl).gap) || 0;
            const usableWidth = gridRect.width - gap;

            dragging = true;
            handle.classList.add('grid-block__resize-handle--active');

            const preview = createPreviewOverlay();
            const containerRect = container.getBoundingClientRect();
            preview.style.left = `${gridRect.left - containerRect.left}px`;
            preview.style.top = `${gridRect.top - containerRect.top}px`;
            preview.style.width = `${gridRect.width}px`;
            preview.style.height = `${gridRect.height}px`;
            preview.style.gap = `${gap}px`;
            container.appendChild(preview);

            const previewCols = preview.querySelectorAll<HTMLElement>(
                '.grid-block__drag-preview-col'
            );
            const previewLeft = previewCols[0];
            const previewRight = previewCols[1];

            const setPreviewRatio = (ratio: number) => {
                const pct1 = ratio * 100;
                const pct2 = (1 - ratio) * 100;
                previewLeft.style.flex = `0 0 calc(${pct1}% - ${gap / 2}px)`;
                previewRight.style.flex = `0 0 calc(${pct2}% - ${gap / 2}px)`;
            };

            setPreviewRatio(startRatio);

            let currentRatio = startRatio;

            const onPointerMove = (moveEvent: PointerEvent) => {
                const deltaX = moveEvent.clientX - startX;
                currentRatio = Math.min(
                    11 / 12,
                    Math.max(1 / 12, startRatio + deltaX / usableWidth)
                );

                setPreviewRatio(currentRatio);
                positionHandle(handle, gridEl, container);
            };

            const onPointerUp = () => {
                document.removeEventListener('pointermove', onPointerMove);
                document.removeEventListener('pointerup', onPointerUp);

                handle.classList.remove('grid-block__resize-handle--active');
                preview.remove();

                const snappedColumns = snapToGrid(currentRatio);

                dragging = false;

                const node = view.state.doc.nodeAt(gridBlockPos);

                if (node && node.type.name === 'gridBlock') {
                    const tr = view.state.tr.setNodeMarkup(gridBlockPos, undefined, {
                        ...node.attrs,
                        columns: snappedColumns
                    });
                    view.dispatch(tr);
                }
            };

            document.addEventListener('pointermove', onPointerMove);
            document.addEventListener('pointerup', onPointerUp);
        });
    }

    return new Plugin({
        key: GRID_RESIZE_PLUGIN_KEY,
        view(view: EditorView) {
            requestAnimationFrame(() => syncHandles(view));

            return {
                update(view: EditorView) {
                    requestAnimationFrame(() => syncHandles(view));
                },
                destroy() {
                    removeAllHandles();

                    if (overlayContainer) {
                        overlayContainer.remove();
                        overlayContainer = null;
                    }
                }
            };
        }
    });
}
