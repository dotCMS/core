import { ChangeDetectionStrategy, Component } from '@angular/core';

import { SkeletonModule } from 'primeng/skeleton';

@Component({
    selector: 'dot-experiments-reports-skeleton',
    imports: [SkeletonModule],
    templateUrl: './dot-experiments-reports-skeleton.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'w-full'
    }
})
export class DotExperimentsReportsSkeletonComponent {}
