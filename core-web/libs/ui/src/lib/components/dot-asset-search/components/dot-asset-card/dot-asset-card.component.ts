import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { Card } from 'primeng/card';
import { Tag } from 'primeng/tag';

import { DotCMSContentlet } from '@dotcms/dotcms-models';

import { DotContentThumbnailComponent } from '../../../dot-content-thumbnail/dot-content-thumbnail.component';
import { DotContentletStatusBadgeComponent } from '../../../dot-contentlet-status-badge/dot-contentlet-status-badge.component';

@Component({
    selector: 'dot-asset-card',
    templateUrl: './dot-asset-card.component.html',
    styleUrls: ['./dot-asset-card.component.scss'],
    imports: [Card, Tag, DotContentletStatusBadgeComponent, DotContentThumbnailComponent],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAssetCardComponent {
    @Input() contentlet: DotCMSContentlet;
}
