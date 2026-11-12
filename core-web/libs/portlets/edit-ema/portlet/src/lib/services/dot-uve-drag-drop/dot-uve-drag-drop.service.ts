import { fromEvent } from 'rxjs';

import { Injectable, inject, DestroyRef, ElementRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { filter } from 'rxjs/operators';

import { WINDOW, isEqual } from '@dotcms/utils';

import {
    IFRAME_LEGACY_SCROLL_THROTTLE_MS,
    IFRAME_SCROLL_VELOCITY,
    IFRAME_SCROLL_ZONE
} from '../../shared/consts';
import { EDITOR_STATE } from '../../shared/enums';
import { UVEStore } from '../../store/dot-uve.store';
import { TEMPORAL_DRAG_ITEM, getDragItemData } from '../../utils';
import { UveIframeMessengerService } from '../iframe-messenger/uve-iframe-messenger.service';

export interface DragDropHandlers {
    onDrop: (event: DragEvent) => void;
    onDragEnter: (event: DragEvent) => void;
    onDragOver: (event: DragEvent) => void;
    onDragLeave: () => void;
    onDragEnd: () => void;
    onDragStart: (event: DragEvent) => void;
}

/**
 * The speed the page scrolls at for the pointer's height, in signed px per 60fps frame: negative
 * toward the top, positive toward the bottom, 0 in the middle. It eases quadratically from the
 * minimum just inside an edge zone to the maximum at the edge, and stays at the maximum at and
 * past the edge. Both edges are treated alike, and the nearer edge wins where the zones overlap
 * on a short iframe.
 *
 * @param clientY the pointer's vertical position in the admin window
 * @param iframeRect where the iframe is in the admin window
 * @returns the velocity, 0 outside both edge zones
 */
function getEdgeScrollVelocity(clientY: number, iframeRect: DOMRect): number {
    const distanceToTop = clientY - iframeRect.top;
    const distanceToBottom = iframeRect.bottom - clientY;
    const nearTop = distanceToTop <= distanceToBottom;
    const depth = IFRAME_SCROLL_ZONE - (nearTop ? distanceToTop : distanceToBottom);

    if (depth <= 0) {
        return 0;
    }

    const { MIN, MAX } = IFRAME_SCROLL_VELOCITY;
    const eased = (Math.min(depth, IFRAME_SCROLL_ZONE) / IFRAME_SCROLL_ZONE) ** 2;

    return (nearTop ? -1 : 1) * (MIN + (MAX - MIN) * eased);
}

@Injectable()
export class DotUveDragDropService {
    private readonly window = inject(WINDOW);
    private readonly destroyRef = inject(DestroyRef);
    private readonly iframeMessenger = inject(UveIframeMessengerService);

    setupDragEvents(
        uveStore: InstanceType<typeof UVEStore>,
        iframe: ElementRef<HTMLIFrameElement>,
        customDragImage: ElementRef<HTMLDivElement>,
        handlers: DragDropHandlers
    ): void {
        // The velocity the page was last asked to scroll at; 0 when it is not scrolling.
        let lastVelocity = 0;
        // When the last one-step scroll request went out, read from `Date.now()`.
        let lastLegacyScrollAt = Number.NEGATIVE_INFINITY;

        /** Ask the page to scroll at `velocity`, only when it differs from the last ask. */
        const scrollAtVelocity = (velocity: number) => {
            if (velocity === lastVelocity) {
                return;
            }

            lastVelocity = velocity;
            this.iframeMessenger.scrollAtVelocity(velocity);
        };

        const stopScrolling = () => scrollAtVelocity(0);

        // The page scrolls on its own until told to stop: leaving the editor mid-scroll must stop
        // it, or it runs to the page's limit.
        this.destroyRef.onDestroy(stopScrolling);

        /**
         * Pages on an older `@dotcms/uve` ignore the velocity and scroll a smooth 120px step per
         * request. Sending one per `dragover` made the steps interrupt each other, so they go
         * out at most once per throttle window. Newer pages ignore them.
         */
        const sendLegacyScroll = (direction: 'up' | 'down') => {
            const now = Date.now();

            if (now - lastLegacyScrollAt < IFRAME_LEGACY_SCROLL_THROTTLE_MS) {
                return;
            }

            lastLegacyScrollAt = now;
            this.iframeMessenger.scrollInsideIframe(direction);
        };

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

                // Flush bounds at drag start. `setEditorDragItem` is deferred to
                // the next frame (to snapshot the drag image first), so the
                // `dragenter` handler can fire before it and bail without
                // flushing — leaving the dropzone with empty bounds and no
                // targets. Flushing here guarantees bounds regardless of that race.
                this.iframeMessenger.flushBounds();
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
                this.iframeMessenger.flushBounds();

                if (dragItem) {
                    return;
                }

                uveStore.setEditorDragItem(TEMPORAL_DRAG_ITEM);
                handlers.onDragEnter(event);
            });

        // Drag end
        fromEvent(this.window, 'dragend')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(() => {
                stopScrolling();
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
                // Beside the iframe a scroll never starts (a palette tile picked up near the
                // bottom must not scroll the page), but one already running follows the pointer.
                const velocity =
                    isInsideIframe || lastVelocity !== 0
                        ? getEdgeScrollVelocity(event.clientY, iframeRect)
                        : 0;

                if (!velocity) {
                    stopScrolling();
                    uveStore.setEditorState(EDITOR_STATE.DRAGGING);

                    return;
                }

                // The page scrolls itself at this speed until told otherwise. The drop targets
                // stay: a page that scrolls reports it, and at the top or bottom nothing scrolls.
                scrollAtVelocity(velocity);
                sendLegacyScroll(velocity < 0 ? 'up' : 'down');

                handlers.onDragOver(event);
            });

        // Drag leave
        fromEvent(this.window, 'dragleave')
            .pipe(
                takeUntilDestroyed(this.destroyRef),
                filter((event: DragEvent) => !event.relatedTarget)
            )
            .subscribe(() => {
                const dragItem = uveStore.editorDragItem();

                // A drag from the editor comes back or ends with dragend, so it keeps its
                // scroll, its drag item and its drop targets while the pointer is outside the
                // app window. A file from the desktop gets no dragend, so it stops here.
                if (dragItem && !isEqual(dragItem, TEMPORAL_DRAG_ITEM)) {
                    return;
                }

                stopScrolling();
                handlers.onDragLeave();
            });

        // Drop
        fromEvent(this.window, 'drop')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe((event: DragEvent) => {
                stopScrolling();
                handlers.onDrop(event);
            });
    }
}
