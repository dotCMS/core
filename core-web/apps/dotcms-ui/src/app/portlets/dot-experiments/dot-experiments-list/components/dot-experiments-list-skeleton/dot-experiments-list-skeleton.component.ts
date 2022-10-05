import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dot-experiments-list-skeleton',
    templateUrl: './dot-experiments-list-skeleton.component.html',
    styleUrls: ['./dot-experiments-list-skeleton.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsListSkeletonComponent {}
