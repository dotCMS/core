import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { ChipModule } from 'primeng/chip';

import { DotContentState } from '@dotcms/dotcms-models';

import { DotContentletStatusPipe } from '../../dot-contentlet-status/dot-contentlet-status.pipe';

@Component({
    selector: 'dot-contentlet-status-chip',
    imports: [ChipModule, DotContentletStatusPipe],
    templateUrl: './dot-contentlet-status-chip.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true
})
export class DotContentletStatusChipComponent {
    state = input.required<DotContentState>();
}
