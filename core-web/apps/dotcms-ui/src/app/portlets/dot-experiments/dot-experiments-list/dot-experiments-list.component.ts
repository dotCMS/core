import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { DotExperiment } from '../shared/models/dot-experiments.model';
import { ExperimentsStatusList } from '@portlets/dot-experiments/shared/models/dot-experiments-constants';
import { DotMessagePipe } from '@dotcms/app/view/pipes';
import { ActivatedRoute } from '@angular/router';
import {
    DotExperimentsListStore,
    VmListExperiments
} from '@portlets/dot-experiments/dot-experiments-list/store/dot-experiments-list-store.service';

@Component({
    selector: 'dot-experiments-list',
    templateUrl: './dot-experiments-list.component.html',
    styleUrls: ['./dot-experiments-list.component.scss'],
    providers: [DotMessagePipe],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsListComponent implements OnInit {
    vm$: Observable<VmListExperiments> = this.dotExperimentsListStore.vm$;

    statusOptionList = ExperimentsStatusList.map(({ label, value }) => {
        return { value, label: this.dotMessagePipe.transform(label) };
    });

    constructor(
        private readonly dotExperimentsListStore: DotExperimentsListStore,
        private readonly dotMessagePipe: DotMessagePipe,
        private readonly route: ActivatedRoute
    ) {}

    ngOnInit() {
        const pageId = this.route.parent?.parent?.parent.snapshot.params.pageId;
        this.dotExperimentsListStore.loadExperiments(pageId);
    }

    /**
     * Update the list of selected statuses
     * @param {Array<string>} $event
     * @returns void
     * @memberof DotExperimentsListComponent
     */
    selectedStatusFilter($event: Array<string>) {
        this.dotExperimentsListStore.setFilterStatus($event);
    }

    /**
     * Add new experiment
     * @param experiment
     * @returns void
     * @memberof DotExperimentsListComponent
     */
    addNewExperiment() {
        // To implement
    }

    /**
     * Archive experiment
     * @param {DotExperiment} experiment
     * @returns void
     * @memberof DotExperimentsListComponent
     */
    archiveExperiment(experiment: DotExperiment) {
        this.dotExperimentsListStore.archiveExperiment(experiment);
    }

    /**
     * Delete experiment
     * @param {DotExperiment} experiment
     * @returns void
     * @memberof DotExperimentsListComponent
     */
    deleteExperiment(experiment: DotExperiment) {
        this.dotExperimentsListStore.deleteExperiment(experiment);
    }
}
