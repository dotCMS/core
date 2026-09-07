import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Select } from 'primeng/select';

import { UVEStore } from '../../../store/dot-uve.store';

const ZOOM_PRESETS = [50, 75, 100, 150, 200];

@Component({
    selector: 'dot-uve-zoom-controls',
    standalone: true,
    templateUrl: './dot-uve-zoom-controls.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [Select, FormsModule],
    host: { class: 'flex items-center gap-1' }
})
export class DotUveZoomControlsComponent {
    protected readonly store = inject(UVEStore);

    readonly $viewZoomLevelPct = this.store.viewZoomLevel;

    /**
     * Standard zoom presets, plus the current zoom level when it doesn't match
     * any preset (e.g. auto-fit zoom from a device preset like 67%). Ensures
     * the select label always reflects the actual zoom.
     */
    readonly $zoomOptions = computed(() => {
        const current = this.$viewZoomLevelPct();
        const values = ZOOM_PRESETS.includes(current)
            ? ZOOM_PRESETS
            : [...ZOOM_PRESETS, current].sort((a, b) => a - b);
        return values.map((value) => ({ label: `${value}%`, value }));
    });

    onZoomChange(value: number): void {
        this.store.viewZoomSetLevel(value);
    }
}
