import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import {
    DotExperimentsConfigurationStore,
    VmConfigurationExperiments
} from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store.service';
import { Observable } from 'rxjs';

@Component({
    selector: 'dot-experiments-configuration',
    templateUrl: './dot-experiments-configuration.component.html',
    styleUrls: ['./dot-experiments-configuration.component.scss'],
    providers: [DotExperimentsConfigurationStore],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationComponent implements OnInit {
    vm$: Observable<VmConfigurationExperiments> = this.dotExperimentsConfigurationStore.vm$;

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore
    ) {}

    ngOnInit(): void {
        this.dotExperimentsConfigurationStore.loadExperiment();
    }
}
