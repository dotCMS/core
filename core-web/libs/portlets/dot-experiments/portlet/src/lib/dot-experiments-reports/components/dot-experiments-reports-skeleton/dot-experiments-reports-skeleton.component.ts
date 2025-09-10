import { ChangeDetectionStrategy, Component } from '@angular/core';

import { SkeletonModule } from 'primeng/skeleton';

@Component({
    selector: 'dot-experiments-reports-skeleton',
    imports: [SkeletonModule],
    templateUrl: './dot-experiments-reports-skeleton.component.html',
    styleUrls: ['./dot-experiments-reports-skeleton.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsReportsSkeletonComponent {}
