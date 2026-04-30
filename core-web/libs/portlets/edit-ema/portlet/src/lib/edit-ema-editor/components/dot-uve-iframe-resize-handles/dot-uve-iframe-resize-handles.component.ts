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

        const startX = event.clientX;
        const startY = event.clientY;
        const startWidth = this.store.viewIframeWidth();
        const startHeight = this.store.viewIframeHeight();
        const zoom = this.store.$viewZoomLevel();

        const target = event.target as HTMLElement;
        target.setPointerCapture(event.pointerId);

        const onMove = (e: PointerEvent) => {
            const dx = (e.clientX - startX) / zoom;
            const dy = (e.clientY - startY) / zoom;

            const patch: { width?: number; height?: number } = {};
            if (axis === 'width' || axis === 'both') {
                patch.width = startWidth + dx;
            }
            if (axis === 'height' || axis === 'both') {
                patch.height = startHeight + dy;
            }
            this.store.viewSetIframeSize(patch);
        };

        const onUp = (e: PointerEvent) => {
            target.releasePointerCapture(e.pointerId);
            target.removeEventListener('pointermove', onMove);
            target.removeEventListener('pointerup', onUp);
            target.removeEventListener('pointercancel', onUp);
        };

        target.addEventListener('pointermove', onMove);
        target.addEventListener('pointerup', onUp);
        target.addEventListener('pointercancel', onUp);
    }
}
