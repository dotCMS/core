import { ChangeDetectionStrategy, Component } from '@angular/core';

import { CardModule } from 'primeng/card';
import { SkeletonModule } from 'primeng/skeleton';

@Component({
    selector: 'dot-asset-card-skeleton',
    templateUrl: './dot-asset-card-skeleton.component.html',
    styleUrls: ['./dot-asset-card-skeleton.component.scss'],
    imports: [CardModule, SkeletonModule],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotAssetCardSkeletonComponent {}
