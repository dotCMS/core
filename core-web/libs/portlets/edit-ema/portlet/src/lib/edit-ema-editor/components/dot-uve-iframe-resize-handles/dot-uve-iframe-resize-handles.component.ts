import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

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

    readonly $isResponsive = this.store.$viewIsResponsiveMode;

    onPointerDown(event: PointerEvent, axis: ResizeAxis): void {
        if (!this.$isResponsive()) {
            return;
        }

        event.preventDefault();
        event.stopPropagation();

        const zoom = this.store.$viewZoomLevel();
        const target = event.target as HTMLElement;
        target.setPointerCapture(event.pointerId);

        // Hide contentlet-tools / dropzone while resizing — same plumbing as
        // iframe scroll: clears editorBounds + editorContentArea and flips
        // editorState to SCROLLING. Released on pointerup/cancel.
        this.store.updateEditorScrollState();

        // The canvas is centered (margin: 0 auto), so growing the iframe shifts
        // the right/bottom handle by only half the size delta — naïve "iframe
        // width += cursor dx" makes the cursor visibly outpace the handle.
        // Solve by measuring the handle's current edge each frame and growing
        // the iframe by the cursor's distance from that edge: the handle ends
        // up under the cursor regardless of how the layout shifts.
        const onMove = (e: PointerEvent) => {
            const rect = target.getBoundingClientRect();
            const patch: { width?: number; height?: number } = {};

            if (axis === 'width' || axis === 'both') {
                const handleX = rect.left + rect.width / 2;
                const dxScreen = e.clientX - handleX;
                patch.width = this.store.viewIframeWidth() + dxScreen / zoom;
            }
            if (axis === 'height' || axis === 'both') {
                const handleY = rect.top + rect.height / 2;
                const dyScreen = e.clientY - handleY;
                patch.height = this.store.viewIframeHeight() + dyScreen / zoom;
            }

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
