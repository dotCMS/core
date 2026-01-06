import { Component, inject } from '@angular/core';
import { ButtonModule } from 'primeng/button';

import { UVEStore } from '../../../store/dot-uve.store';

@Component({
    selector: 'dot-uve-zoom-controls',
    standalone: true,
    templateUrl: './dot-uve-zoom-controls.component.html',
    styleUrls: ['./dot-uve-zoom-controls.component.scss'],
    imports: [
        ButtonModule
    ]
})
export class DotUveZoomControlsComponent {
    protected readonly store = inject(UVEStore);

    readonly $zoomLevel = this.store.$zoomLevel;
    readonly $zoomLabel = this.store.zoomLabel.bind(this.store);

    zoomIn(): void {
        this.store.zoomIn();
    }

    zoomOut(): void {
        this.store.zoomOut();
    }

    resetView(): void {
        this.store.resetZoom();
    }

    zoomLabel(): string {
        return this.store.zoomLabel();
    }
}

