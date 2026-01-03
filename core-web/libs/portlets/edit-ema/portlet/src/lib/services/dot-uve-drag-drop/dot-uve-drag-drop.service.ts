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
                const dragItem = uveStore.editor().dragItem;

                if (!dragItem && types.includes('dotcms/item')) {
                    return;
                }

                uveStore.setEditorState(EDITOR_STATE.DRAGGING);
                contentWindow?.postMessage(
                    {
                        name: __DOTCMS_UVE_EVENT__.UVE_REQUEST_BOUNDS
                    },
                    host
                );

                if (dragItem) {
                    return;
                }

                uveStore.setEditorDragItem(TEMPORAL_DRAG_ITEM);
                handlers.onDragEnter(event);
            });

        // Drag end
        fromEvent(this.window, 'dragend')
            .pipe(
                takeUntilDestroyed(this.destroyRef),
                filter((event: DragEvent) => event.dataTransfer?.dropEffect === 'none')
            )
            .subscribe(() => {
                handlers.onDragEnd();
            });

        // Drag over
        fromEvent(this.window, 'dragover')
            .pipe(
                takeUntilDestroyed(this.destroyRef),
                filter(() => !!uveStore.editor().dragItem)
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

