import { ChangeDetectionStrategy, Component, inject, untracked } from '@angular/core';

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

    onPointerDown(event: PointerEvent, axis: ResizeAxis): void {
        event.preventDefault();
        event.stopPropagation();

        const target = event.target as HTMLElement;
        target.setPointerCapture(event.pointerId);

        // Hide contentlet-tools / dropzone and flag editorState=SCROLLING.
        // Order matters: set the scrolling flag *before* exiting the device
        // preset so the responsive-mode sync effect skips its canvas-snap
        // (which would otherwise produce a visual jump on pointer down).
        this.store.updateEditorScrollState();

        // Dragging from a device preset switches back to responsive so the
        // user-driven size and the canvas clamp take over. Use the
        // size-preserving exit so the iframe doesn't jump on pointer down.
        this.store.viewExitDevicePreset();

        // viewIframeWidth/Height now represent the on-screen size (handles
        // never move with zoom), so the resize math is 1:1 with the cursor.
        // The canvas is centered (margin: 0 auto), so growing shifts the
        // handle by only half the size delta — measure the handle's current
        // edge each frame and grow by the cursor's distance from it.
        //
        // Wrapped in untracked() so these signal reads don't accidentally
        // register as dependencies if this handler is ever invoked from
        // within a reactive context.
        const onMove = (e: PointerEvent) => {
            const rect = target.getBoundingClientRect();
            const patch: { width?: number; height?: number } = {};

            untracked(() => {
                if (axis === 'width' || axis === 'both') {
                    const handleX = rect.left + rect.width / 2;
                    patch.width = this.store.viewIframeWidth() + (e.clientX - handleX);
                }
                if (axis === 'height' || axis === 'both') {
                    const handleY = rect.top + rect.height / 2;
                    patch.height = this.store.viewIframeHeight() + (e.clientY - handleY);
                }
            });

            this.store.viewSetIframeSize(patch);
        };

        const onUp = (e: PointerEvent) => {
            target.releasePointerCapture(e.pointerId);
            target.removeEventListener('pointermove', onMove);
            target.removeEventListener('pointerup', onUp);
            target.removeEventListener('pointercancel', onUp);
            this.store.updateEditorOnScrollEnd();
        };

        target.addEventListener('pointermove', onMove);
        target.addEventListener('pointerup', onUp);
        target.addEventListener('pointercancel', onUp);
    }
}
