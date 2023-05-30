import { ChangeDetectionStrategy, Component } from '@angular/core';

import { SkeletonModule } from 'primeng/skeleton';

@Component({
    standalone: true,
    selector: 'dot-experiments-list-skeleton',
    imports: [SkeletonModule],
    templateUrl: './dot-experiments-list-skeleton.component.html',
    styles: [
        `
            :host {
                width: 100%;
            }
        `
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsListSkeletonComponent {}
