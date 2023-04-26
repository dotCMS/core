import { provideComponentStore } from '@ngrx/component-store';
import { Observable } from 'rxjs';

import { AsyncPipe, JsonPipe, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, ComponentRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { RippleModule } from 'primeng/ripple';

import { tap } from 'rxjs/operators';

import { DotMessagePipe } from '@dotcms/app/view/pipes';
import {
    ComponentStatus,
    DotExperiment,
    ExperimentsStatusList,
    SidebarStatus
} from '@dotcms/dotcms-models';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotExperimentsCreateComponent } from '@portlets/dot-experiments/dot-experiments-list/components/dot-experiments-create/dot-experiments-create.component';
import { DotExperimentsEmptyExperimentsComponent } from '@portlets/dot-experiments/dot-experiments-list/components/dot-experiments-empty-experiments/dot-experiments-empty-experiments.component';
import { DotExperimentsListSkeletonComponent } from '@portlets/dot-experiments/dot-experiments-list/components/dot-experiments-list-skeleton/dot-experiments-list-skeleton.component';
import { DotExperimentsListTableComponent } from '@portlets/dot-experiments/dot-experiments-list/components/dot-experiments-list-table/dot-experiments-list-table.component';
import { DotExperimentsStatusFilterComponent } from '@portlets/dot-experiments/dot-experiments-list/components/dot-experiments-status-filter/dot-experiments-status-filter.component';
import {
    DotExperimentsListStore,
    VmListExperiments
} from '@portlets/dot-experiments/dot-experiments-list/store/dot-experiments-list-store';
import { DotExperimentsUiHeaderComponent } from '@portlets/dot-experiments/shared/ui/dot-experiments-header/dot-experiments-ui-header.component';
import { DotDynamicDirective } from '@portlets/shared/directives/dot-dynamic.directive';

const DOT_EXPERIMENT_LIST_COMPONENTS = [
    DotExperimentsListSkeletonComponent,
    DotExperimentsEmptyExperimentsComponent,
    DotExperimentsStatusFilterComponent,
    DotExperimentsListTableComponent
];

@Component({
    standalone: true,
    selector: 'dot-experiments-list',
    imports: [
        AsyncPipe,
        NgIf,
        // DotCMS
        ...DOT_EXPERIMENT_LIST_COMPONENTS,
        DotExperimentsUiHeaderComponent,
        DotDynamicDirective,
        DotMessagePipeModule,
        // PrimeNG
        ButtonModule,
        RippleModule,
        DotExperimentsStatusFilterComponent,
        JsonPipe
    ],
    templateUrl: './dot-experiments-list.component.html',
    styleUrls: ['./dot-experiments-list.component.scss'],
    providers: [DotMessagePipe, provideComponentStore(DotExperimentsListStore)],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsListComponent {
    @ViewChild(DotDynamicDirective, { static: true }) sidebarHost!: DotDynamicDirective;
    vm$: Observable<VmListExperiments> = this.dotExperimentsListStore.vm$.pipe(
        tap(({ sidebar }) => this.handleSidebar(sidebar))
    );
    statusOptionList = ExperimentsStatusList;
    private componentRef: ComponentRef<DotExperimentsCreateComponent>;

    constructor(
        private readonly dotExperimentsListStore: DotExperimentsListStore,
        private readonly dotMessagePipe: DotMessagePipe,
        private readonly router: Router
    ) {}

    /**
     * Update the list of selected statuses
     * @param {Array<string>} $event
     * @returns void
     * @memberof DotExperimentsListComponent
     */
    selectedStatusFilter($event: Array<string>): void {
        this.dotExperimentsListStore.setFilterStatus($event);
    }

    /**
     * Add new experiment
     * @returns void
     * @memberof DotExperimentsListComponent
     */
    addExperiment(): void {
        this.dotExperimentsListStore.openSidebar();
    }

    /**
     * Archive experiment
     * @param {DotExperiment} experiment
     * @returns void
     * @memberof DotExperimentsListComponent
     */
    archiveExperiment(experiment: DotExperiment): void {
        this.dotExperimentsListStore.archiveExperiment(experiment);
    }

    /**
     * Delete experiment
     * @param {DotExperiment} experiment
     * @returns void
     * @memberof DotExperimentsListComponent
     */
    deleteExperiment(experiment: DotExperiment): void {
        this.dotExperimentsListStore.deleteExperiment(experiment);
    }

    /**
     * Back to Edit Page / Content
     * @returns void
     * @memberof DotExperimentsShellComponent
     */
    goToBrowserBack(): void {
        this.router.navigate(['edit-page/content'], {
            queryParams: {
                mode: null,
                variantName: null,
                experimentId: null
            },
            queryParamsHandling: 'merge'
        });
    }

    /**
     * Opens the experiment report page for a given experiment
     * @param {DotExperiment} experiment The experiment whose report page needs to be opened
     * @returns void
     * @memberof DotExperimentsShellComponent
     */

    goToViewExperimentReport(experiment: DotExperiment) {
        this.router.navigate(['/edit-page/experiments/reports/', experiment.id], {
            queryParams: {
                mode: null,
                variantName: null,
                experimentId: null
            },
            queryParamsHandling: 'merge'
        });
    }

    private handleSidebar(status: SidebarStatus): void {
        if (status && status.isOpen && status.status != ComponentStatus.SAVING) {
            this.loadSidebarComponent();
        } else {
            this.removeSidebarComponent();
        }
    }

    private loadSidebarComponent(): void {
        this.sidebarHost.viewContainerRef.clear();
        this.componentRef =
            this.sidebarHost.viewContainerRef.createComponent<DotExperimentsCreateComponent>(
                DotExperimentsCreateComponent
            );
    }

    private removeSidebarComponent(): void {
        if (this.componentRef) {
            this.sidebarHost.viewContainerRef.clear();
        }
    }
}
