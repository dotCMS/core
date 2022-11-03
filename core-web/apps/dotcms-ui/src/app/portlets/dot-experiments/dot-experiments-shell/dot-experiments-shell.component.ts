import { ChangeDetectionStrategy, Component } from '@angular/core';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import {
    DotExperimentsShellState,
    DotExperimentsShellStore
} from '@portlets/dot-experiments/dot-experiments-shell/store/dot-experiments-shell-store.service';
import { provideComponentStore } from '@ngrx/component-store';
import { MessageService } from 'primeng/api';

@Component({
    selector: 'dot-experiments-shell',
    templateUrl: 'dot-experiments-shell.component.html',
    styleUrls: ['./dot-experiments-shell.component.scss'],
    providers: [provideComponentStore(DotExperimentsShellStore), MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsShellComponent {
    readonly vm$: Observable<DotExperimentsShellState> = this.cs.state$;

    constructor(private readonly cs: DotExperimentsShellStore, private readonly router: Router) {}

    /**
     * Back to Edit Page / Content
     * @returns void
     * @memberof DotExperimentsShellComponent
     */
    goBack() {
        this.router.navigate(['edit-page/content'], { queryParamsHandling: 'preserve' });
    }
}
