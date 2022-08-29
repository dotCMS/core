import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dot-experiments-list',
    templateUrl: './dot-experiments-list.component.html',
    styleUrls: ['./dot-experiments-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsListComponent {
    title = 'Experiments list';
}
