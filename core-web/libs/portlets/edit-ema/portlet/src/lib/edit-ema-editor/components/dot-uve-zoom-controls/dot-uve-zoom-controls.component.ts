import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { Select } from 'primeng/select';

import { UVEStore } from '../../../store/dot-uve.store';

const ZOOM_OPTIONS = [50, 75, 100, 150, 200].map((value) => ({
    label: `${value}%`,
    value
}));

@Component({
    selector: 'dot-uve-zoom-controls',
    standalone: true,
    templateUrl: './dot-uve-zoom-controls.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [ButtonModule, Select, FormsModule],
    host: { class: 'flex items-center bg-gray-100 rounded-full px-3 py-1 gap-1' }
})
export class DotUveZoomControlsComponent {
    protected readonly store = inject(UVEStore);

    readonly $viewZoomLevel = this.store.$viewZoomLevel;
    readonly zoomOptions = ZOOM_OPTIONS;

    onZoomChange(value: number): void {
        this.store.viewZoomSetLevel(value);
    }

    resetView(): void {
        this.store.viewZoomReset();
    }
}
