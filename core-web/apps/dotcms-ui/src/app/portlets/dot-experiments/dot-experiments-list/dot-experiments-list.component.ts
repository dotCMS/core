import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { DotExperimentsStore } from '@portlets/dot-experiments/shared/stores/dot-experiments-store.service';
import { Observable } from 'rxjs';
import { DotExperiment, ExperimentsStatusList } from '../shared/models/dot-experiments.model';
import { DotMessagePipe } from '@dotcms/app/view/pipes';

interface VmListExperiments {
    isLoading: boolean;
    experiments: DotExperiment[];
    experimentsFiltered: { [key: string]: DotExperiment[] };
    experimentsFilterStatus: Array<string>;
}

@Component({
    selector: 'dot-experiments-list',
    templateUrl: './dot-experiments-list.component.html',
    styleUrls: ['./dot-experiments-list.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    providers: [DotMessagePipe]
})
export class DotExperimentsListComponent implements OnInit {
    vmListExperiments$: Observable<VmListExperiments> = this.experimentsStore.vmExperimentList$;

    statusOptions = ExperimentsStatusList.map(({ label, value }) => {
        return { value, label: this.dotMessagePipe.transform(label) };
    });

    constructor(
        private readonly experimentsStore: DotExperimentsStore,
        private readonly dotMessagePipe: DotMessagePipe
    ) {}

    ngOnInit() {
        this.experimentsStore.loadExperiments();
    }

    selectedStatusFilter($event: Array<string>) {
        this.experimentsStore.setExperimentsFilterStatus($event);
    }

    addNewExperiment() {
        // To implement
    }

    archiveExperiment(experimentId: string) {
        this.experimentsStore.archiveExperiment(experimentId);
    }

    deleteExperiment(experimentId: string) {
        this.experimentsStore.deleteExperiment(experimentId);
    }
}
