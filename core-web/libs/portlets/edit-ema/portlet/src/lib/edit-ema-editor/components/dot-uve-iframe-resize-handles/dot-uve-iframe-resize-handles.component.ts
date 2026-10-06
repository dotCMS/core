import { ChangeDetectionStrategy, Component, DestroyRef, inject } from '@angular/core';

import { UVEStore } from '../../../store/dot-uve.store';

type ResizeAxis = 'width' | 'height' | 'both';

@Component({
    selector: 'dot-uve-iframe-resize-handles',
    standalone: true,
    templateUrl: './dot-uve-iframe-resize-handles.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: { class: 'absolute inset-0 pointer-events-none' }
})
export class DotUveIframeResizeHandlesComponent {
    private readonly store = inject(UVEStore);
    private readonly destroyRef = inject(DestroyRef);

    /**
     * AbortController for the in-flight drag listeners. Used to release pointer
     * capture and detach pointermove/up/cancel listeners if the component is
     * destroyed before the user releases — prevents leaked listeners on
     * navigation away mid-drag.
     */
    #dragAbort: AbortController | null = null;

    /**
     * Capture target + pointerId, retained so the destroy path can release
     * pointer capture even when no explicit target is passed in (some
     * browsers leave capture stuck if the element goes away mid-drag).
     */
    #captureTarget: HTMLElement | null = null;
    #capturePointerId: number | null = null;

    constructor() {
        this.destroyRef.onDestroy(() => this.#endDrag(null));
    }

    onPointerDown(event: PointerEvent, axis: ResizeAxis): void {
        event.preventDefault();
        event.stopPropagation();

        // Cancel any prior in-flight drag (defensive — should not happen in practice).
        this.#endDrag(null);

        const target = event.target as HTMLElement;
        const pointerId = event.pointerId;
        target.setPointerCapture(pointerId);
        this.#captureTarget = target;
        this.#capturePointerId = pointerId;

        // Hide contentlet-tools / dropzone and flag editorState=RESIZING.
        // Order matters: set the resize flag *before* exiting the device
        // preset so the responsive-mode sync effect skips its canvas-snap
        // (which would otherwise produce a visual jump on pointer down).
        this.store.updateEditorResizeState();

        // Dragging from a device preset switches back to responsive so the
        // user-driven size and the canvas clamp take over. Use the
        // size-preserving exit so the iframe doesn't jump on pointer down.
        this.store.viewExitDevicePreset();

        const controller = new AbortController();
        this.#dragAbort = controller;
        const { signal } = controller;

        // viewIframeWidth/Height now represent the on-screen size (handles
        // never move with zoom), so the resize math is 1:1 with the cursor.
        // The canvas is centered (margin: 0 auto), so growing shifts the
        // handle by only half the size delta — measure the handle's current
        // edge each frame and grow by the cursor's distance from it.
        const onMove = (e: PointerEvent) => {
            const rect = target.getBoundingClientRect();
            const patch: { width?: number; height?: number } = {};

            if (axis === 'width' || axis === 'both') {
                const handleX = rect.left + rect.width / 2;
                patch.width = this.store.viewIframeWidth() + (e.clientX - handleX);
            }
            if (axis === 'height' || axis === 'both') {
                const handleY = rect.top + rect.height / 2;
                patch.height = this.store.viewIframeHeight() + (e.clientY - handleY);
            }

            this.store.viewSetIframeSize(patch);
        };

        const onUp = () => this.#endDrag(target, pointerId);

        target.addEventListener('pointermove', onMove, { signal });
        target.addEventListener('pointerup', onUp, { signal });
        target.addEventListener('pointercancel', onUp, { signal });
    }

    /**
     * Tear down the in-flight drag: release pointer capture, abort listeners,
     * flip editor state back to IDLE. Idempotent — safe to call when no drag
     * is active. Called on pointerup, pointercancel, and component destroy.
     */
    #endDrag(target: HTMLElement | null, pointerId?: number): void {
        if (!this.#dragAbort) {
            return;
        }
        this.#dragAbort.abort();
        this.#dragAbort = null;

        // Prefer the explicit args (pointerup path), fall back to the
        // captured pair (component-destroyed-mid-drag path). Without the
        // fallback, destroy could leave pointer capture stuck in some
        // browsers because the caller passes target=null.
        const releaseTarget = target ?? this.#captureTarget;
        const releaseId = pointerId ?? this.#capturePointerId ?? undefined;
        if (
            releaseTarget &&
            releaseId !== undefined &&
            releaseTarget.hasPointerCapture(releaseId)
        ) {
            releaseTarget.releasePointerCapture(releaseId);
        }
        this.#captureTarget = null;
        this.#capturePointerId = null;

        // Flip IDLE so the editor "unfreezes" even if the drag didn't
        // actually change the iframe size (no SDK auto-bounds emit). If
        // the size DID change, SET_BOUNDS will arrive shortly after with
        // fresh coords — the SET_BOUNDS handler is a no-op when state is
        // already IDLE. Also covers component-destroyed-mid-drag.
        this.store.updateEditorOnResizeEnd();
    }
}
