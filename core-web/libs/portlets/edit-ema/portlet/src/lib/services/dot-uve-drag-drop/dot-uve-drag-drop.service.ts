import { fromEvent } from 'rxjs';

import { Injectable, inject, DestroyRef, ElementRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { filter } from 'rxjs/operators';

import { __DOTCMS_UVE_EVENT__ } from '@dotcms/types/internal';
import { WINDOW } from '@dotcms/utils';

import { IFRAME_SCROLL_ZONE } from '../../shared/consts';
import { EDITOR_STATE } from '../../shared/enums';
import { UVEStore } from '../../store/dot-uve.store';
import { TEMPORAL_DRAG_ITEM, getDragItemData } from '../../utils';

export interface DragDropHandlers {
    onDrop: (event: DragEvent) => void;
    onDragEnter: (event: DragEvent) => void;
    onDragOver: (event: DragEvent) => void;
    onDragLeave: () => void;
    onDragEnd: () => void;
    onDragStart: (event: DragEvent) => void;
}

@Injectable()
export class DotUveDragDropService {
    private readonly window = inject(WINDOW);
    private readonly destroyRef = inject(DestroyRef);

    setupDragEvents(
        uveStore: InstanceType<typeof UVEStore>,
        iframe: ElementRef<HTMLIFrameElement>,
        customDragImage: ElementRef<HTMLDivElement>,
        contentWindow: Window | null,
        host: string,
        handlers: DragDropHandlers
    ): void {
        // Drag start
        fromEvent(this.window, 'dragstart')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((event: DragEvent) => {
                const { dataset } = event.target as HTMLDivElement;
                const data = getDragItemData(dataset);
                const shouldUseCustomDragImage = dataset.useCustomDragImage === 'true';

                if (shouldUseCustomDragImage && customDragImage?.nativeElement) {
                    event.dataTransfer?.setDragImage(customDragImage.nativeElement, 0, 0);
                }

                event.dataTransfer?.setData('dotcms/item', '');

                if (!data) {
                    return;
                }

                // Request fresh container bounds the instant the drag begins.
                // `setEditorDragItem` is deferred to the next animation frame
                // (so the browser can snapshot the drag image before Angular
                // re-renders), which means the `dragenter` handler — the only
                // other place that flushes bounds — can fire first, see no
                // drag item yet, and bail before posting UVE_FLUSH_BOUNDS. When
                // that race happens the dropzone is left with stale/empty
                // `editorBounds` and renders no drop targets. Flushing here, at
                // drag start, guarantees bounds are requested regardless of the
                // race so they have arrived by the time `dragover` flips the
                // editor into DRAGGING.
                contentWindow?.postMessage({ name: __DOTCMS_UVE_EVENT__.UVE_FLUSH_BOUNDS }, host);

                requestAnimationFrame(() => uveStore.setEditorDragItem(data));
            });

        // Drag enter
        fromEvent(this.window, 'dragenter')
            .pipe(
                takeUntilDestroyed(this.destroyRef),
                filter((event: DragEvent & { fromElement?: HTMLElement }) => !event.fromElement)
            )
            .subscribe((event: DragEvent) => {
                event.preventDefault();

                const types = event.dataTransfer?.types || [];
                const dragItem = uveStore.editorDragItem();

                if (!dragItem && types.includes('dotcms/item')) {
                    return;
                }

                uveStore.setEditorState(EDITOR_STATE.DRAGGING);
                // Drag enter: dropzone needs current bounds before any
                // pixel of movement, so flush past the auto-bounds debounce.
                contentWindow?.postMessage(
                    {
                        name: __DOTCMS_UVE_EVENT__.UVE_FLUSH_BOUNDS
                    },
                    host
                );

                if (dragItem) {
                    return;
                }

                uveStore.setEditorDragItem(TEMPORAL_DRAG_ITEM);
                handlers.onDragEnter(event);
            });

        // Drag end — reset editor UI state after EVERY drag gesture, not just
        // cancelled ones. Previously this was filtered to `dropEffect === 'none'`
        // (cancelled drops only), leaving successful drops to be reset by the
        // async save→reload. That left a window where `editorState` stayed
        // DRAGGING after a successful drop: the next drag then had no clean
        // IDLE→DRAGGING transition, so `$handleIsDraggingEffect` (which flushes
        // container bounds on that transition) never re-fired and the dropzone
        // showed no targets. `dragend` always fires when the gesture ends and
        // `handleDrop` has already consumed the drag item synchronously (drop
        // fires before dragend), so resetting here is safe and guarantees a
        // clean IDLE state for the next drag.
        fromEvent(this.window, 'dragend')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(() => {
                handlers.onDragEnd();
            });

        // Drag over
        fromEvent(this.window, 'dragover')
            .pipe(
                takeUntilDestroyed(this.destroyRef),
                filter(() => !!uveStore.editorDragItem())
            )
            .subscribe((event: DragEvent) => {
                event.preventDefault();

                if (!iframe?.nativeElement) {
                    return;
                }

                const iframeRect = iframe.nativeElement.getBoundingClientRect();
                const isInsideIframe =
                    event.clientX > iframeRect.left && event.clientX < iframeRect.right;

                if (!isInsideIframe) {
                    uveStore.setEditorState(EDITOR_STATE.DRAGGING);
                    return;
                }

                let direction: 'up' | 'down' | undefined;

                if (
                    event.clientY > iframeRect.top &&
                    event.clientY < iframeRect.top + IFRAME_SCROLL_ZONE
                ) {
                    direction = 'up';
                }

                if (
                    event.clientY > iframeRect.bottom - IFRAME_SCROLL_ZONE &&
                    event.clientY <= iframeRect.bottom
                ) {
                    direction = 'down';
                }

                if (!direction) {
                    uveStore.setEditorState(EDITOR_STATE.DRAGGING);
                    return;
                }

                uveStore.updateEditorScrollDragState();

                contentWindow?.postMessage(
                    { name: __DOTCMS_UVE_EVENT__.UVE_SCROLL_INSIDE_IFRAME, direction },
                    host
                );

                handlers.onDragOver(event);
            });

        // Drag leave
        fromEvent(this.window, 'dragleave')
            .pipe(
                takeUntilDestroyed(this.destroyRef),
                filter((event: DragEvent) => !event.relatedTarget)
            )
            .subscribe(() => {
                handlers.onDragLeave();
            });

        // Drop
        fromEvent(this.window, 'drop')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((event: DragEvent) => {
                handlers.onDrop(event);
            });
    }
}
