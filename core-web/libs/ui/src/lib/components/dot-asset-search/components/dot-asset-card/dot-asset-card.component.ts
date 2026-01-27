import { CUSTOM_ELEMENTS_SCHEMA, ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { Card } from 'primeng/card';
import { Chip } from 'primeng/chip';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotContentletStatusChipComponent } from '../../../dot-contentlet-status-chip/dot-contentlet-status-chip.component';

@Component({
    selector: 'dot-asset-card',
    templateUrl: './dot-asset-card.component.html',
    styleUrls: ['./dot-asset-card.component.scss'],
    imports: [Card, Chip, DotContentletStatusChipComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class DotAssetCardComponent {
    @Input() contentlet: DotCMSContentlet;
}
