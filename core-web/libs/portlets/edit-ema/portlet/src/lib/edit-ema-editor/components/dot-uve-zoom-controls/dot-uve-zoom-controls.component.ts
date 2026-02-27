import { Component, inject } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { UVEStore } from '../../../store/dot-uve.store';

@Component({
    selector: 'dot-uve-zoom-controls',
    standalone: true,
    templateUrl: './dot-uve-zoom-controls.component.html',
    styleUrls: ['./dot-uve-zoom-controls.component.scss'],
    imports: [ButtonModule]
})
export class DotUveZoomControlsComponent {
    protected readonly store = inject(UVEStore);

    readonly $viewZoomLevel = this.store.$viewZoomLevel;
    readonly $viewZoomLabel = this.store.viewZoomLabel.bind(this.store);

    viewZoomIn(): void {
        this.store.viewZoomIn();
    }

    viewZoomOut(): void {
        this.store.viewZoomOut();
    }

    resetView(): void {
        this.store.viewZoomReset();
    }

    viewZoomLabel(): string {
        return this.store.viewZoomLabel();
    }
}
