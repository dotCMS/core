import { ChangeDetectionStrategy, Component } from '@angular/core';
import { SkeletonModule } from 'primeng/skeleton';
import { CardModule } from 'primeng/card';

@Component({
    selector: 'dot-experiments-configuration-skeleton',
    standalone: true,
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
