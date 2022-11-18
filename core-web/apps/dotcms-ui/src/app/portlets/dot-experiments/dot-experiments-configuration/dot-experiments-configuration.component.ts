import { ChangeDetectionStrategy, Component } from '@angular/core';
import {
    DotExperimentsConfigurationStore,
    VmConfigurationExperiments
} from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store.service';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { provideComponentStore } from '@ngrx/component-store';

@Component({
    selector: 'dot-experiments-configuration',
    templateUrl: './dot-experiments-configuration.component.html',
    styleUrls: ['./dot-experiments-configuration.component.scss'],
    providers: [provideComponentStore(DotExperimentsConfigurationStore)],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationComponent {
    pageId: string;
    vm$: Observable<VmConfigurationExperiments> = this.dotExperimentsConfigurationStore.vm$.pipe(
        tap((vm) => (this.pageId = vm.pageId))
    );

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore,
        private readonly router: Router
    ) {}

    goToExperimentList() {
        this.router.navigate(['/edit-page/experiments/', this.pageId], {
            queryParamsHandling: 'preserve'
        });
    }
}
