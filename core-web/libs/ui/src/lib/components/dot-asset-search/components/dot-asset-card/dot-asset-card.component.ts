import { CUSTOM_ELEMENTS_SCHEMA, ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { Card } from 'primeng/card';
import { Tag } from 'primeng/tag';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotContentletStatusBadgeComponent } from '../../../dot-contentlet-status-badge/dot-contentlet-status-badge.component';

@Component({
    selector: 'dot-asset-card',
    templateUrl: './dot-asset-card.component.html',
    styleUrls: ['./dot-asset-card.component.scss'],
    imports: [Card, Tag, DotContentletStatusBadgeComponent],
    changeDetection: ChangeDetectionStrategy.OnPush,
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class DotAssetCardComponent {
    @Input() contentlet: DotCMSContentlet;
}
