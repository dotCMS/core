import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { ChipModule } from 'primeng/chip';

import { DotContentState } from '@dotcms/dotcms-models';

import { DotContentletStatusPipe } from '../../dot-contentlet-status/dot-contentlet-status.pipe';
import { DotMessagePipe } from '../../dot-message/dot-message.pipe';

@Component({
    selector: 'dot-contentlet-status-chip',
    imports: [ChipModule, DotContentletStatusPipe, DotMessagePipe],
    templateUrl: './dot-contentlet-status-chip.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true
})
export class DotContentletStatusChipComponent {
    state = input<DotContentState | null>(null);
}
