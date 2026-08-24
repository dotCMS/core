import { ChangeDetectionStrategy, Component } from '@angular/core';

import { CardModule } from 'primeng/card';
import { SkeletonModule } from 'primeng/skeleton';

@Component({
    selector: 'dot-experiments-configuration-skeleton',
    templateUrl: './dot-experiments-configuration-skeleton.component.html',
    styles: [
        `
            :host {
                width: 100%;
            }
        `
    ],
    imports: [SkeletonModule, CardModule],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationSkeletonComponent {}
