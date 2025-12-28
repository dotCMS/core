import { Component, inject } from '@angular/core';
import { DotUveZoomService } from '../../../services/dot-uve-zoom/dot-uve-zoom.service';
import { ButtonModule } from 'primeng/button';

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
    protected readonly zoomService = inject(DotUveZoomService);

    readonly $zoomLevel = this.zoomService.$zoomLevel;
    readonly $zoomLabel = this.zoomService.zoomLabel.bind(this.zoomService);

    zoomIn(): void {
        this.zoomService.zoomIn();
    }

    zoomOut(): void {
        this.zoomService.zoomOut();
    }

    resetView(): void {
        this.zoomService.resetZoom();
    }

    zoomLabel(): string {
        return this.zoomService.zoomLabel();
    }
}

