import { ChangeDetectionStrategy, Component, OnInit, ViewChild } from '@angular/core';
import { Observable } from 'rxjs';
import { DotExperiment } from '../shared/models/dot-experiments.model';
import { ExperimentsStatusList } from '@portlets/dot-experiments/shared/models/dot-experiments-constants';
import { DotMessagePipe } from '@dotcms/app/view/pipes';
import {
    DotExperimentsListStore,
    VmListExperiments
} from '@portlets/dot-experiments/dot-experiments-list/store/dot-experiments-list-store.service';
import { DotDynamicDirective } from '@portlets/shared/directives/dot-dynamic.directive';
import { DotExperimentsCreateComponent } from '@portlets/dot-experiments/dot-experiments-create/dot-experiments-create.component';
import { delay, take } from 'rxjs/operators';

@Component({
    selector: 'dot-experiments-list',
    templateUrl: './dot-experiments-list.component.html',
    styleUrls: ['./dot-experiments-list.component.scss'],
    providers: [DotExperimentsListStore, DotMessagePipe],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsListComponent implements OnInit {
    @ViewChild(DotDynamicDirective, { static: true }) dotDynamicHost!: DotDynamicDirective;

    vm$: Observable<VmListExperiments> = this.dotExperimentsListStore.vm$;

    statusOptionList = ExperimentsStatusList.map(({ label, value }) => {
        return { value, label: this.dotMessagePipe.transform(label) };
    });

    constructor(
        private readonly dotExperimentsListStore: DotExperimentsListStore,
        private readonly dotMessagePipe: DotMessagePipe
    ) {}

    ngOnInit() {
        this.dotExperimentsListStore.loadExperiments();
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
        const viewContainerRef = this.dotDynamicHost.viewContainerRef;
        viewContainerRef.clear();
        const componentRef = viewContainerRef.createComponent<DotExperimentsCreateComponent>(
            DotExperimentsCreateComponent
        );
        componentRef.instance.closedSidebar.pipe(delay(500), take(1)).subscribe(() => {
            viewContainerRef.clear();
        });
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
