import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dot-experiments-list-skeleton',
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
