import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { DotExperiment } from '../shared/models/dot-experiments.model';
import { ExperimentsStatusList } from '@portlets/dot-experiments/shared/models/dot-experiments-constants';
import { DotMessagePipe } from '@dotcms/app/view/pipes';
import { ActivatedRoute } from '@angular/router';
import {
    DotExperimentsListStore,
    VmListExperiments
} from '@portlets/dot-experiments/dot-experiments-list/store/dot-experiments-list-store.service';
import { DotDynamicDirective } from '@portlets/shared/directives/dot-dynamic.directive';
import { DotExperimentsCreateComponent } from '@portlets/dot-experiments/dot-experiments-create/dot-experiments-create.component';
import { delay, filter, takeUntil } from 'rxjs/operators';

@Component({
    selector: 'dot-experiments-list',
    templateUrl: './dot-experiments-list.component.html',
    styleUrls: ['./dot-experiments-list.component.scss'],
    providers: [DotMessagePipe],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsListComponent implements OnInit, OnDestroy {
    @ViewChild(DotDynamicDirective, { static: true }) dotDynamicHost!: DotDynamicDirective;

    vm$: Observable<VmListExperiments> = this.dotExperimentsListStore.vm$;

    statusOptionList = ExperimentsStatusList.map(({ label, value }) => {
        return { value, label: this.dotMessagePipe.transform(label) };
    });

    private unsubscribe$ = new Subject<void>();

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
        const viewContainerRef = this.dotDynamicHost.viewContainerRef;
        viewContainerRef.clear();
        const componentRef = viewContainerRef.createComponent<DotExperimentsCreateComponent>(
            DotExperimentsCreateComponent
        );
        componentRef.instance.vm$
            .pipe(
                takeUntil(this.unsubscribe$),
                filter(({ isOpenSidebar }) => !isOpenSidebar),
                delay(500)
            )
            .subscribe(() => viewContainerRef.clear());
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

    ngOnDestroy(): void {
        this.unsubscribe$.next();
        this.unsubscribe$.complete();
    }
}
