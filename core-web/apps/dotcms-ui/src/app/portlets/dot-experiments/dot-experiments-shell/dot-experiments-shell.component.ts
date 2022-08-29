import { ChangeDetectionStrategy, Component } from '@angular/core';
import { DotExperimentsStore } from '../shared/stores/dot-experiments-store.service';

@Component({
    selector: 'dot-experiments-shell',
    templateUrl: 'dot-experiments-shell.component.html',
    styleUrls: ['./dot-experiments-shell.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsShellComponent {
    status$ = this.experimentsStore.getState$;
    constructor(private readonly experimentsStore: DotExperimentsStore) {}
}
