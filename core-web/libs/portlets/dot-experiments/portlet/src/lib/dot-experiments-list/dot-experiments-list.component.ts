import { provideComponentStore } from '@ngrx/component-store';
import { Observable } from 'rxjs';

import { AsyncPipe, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, ComponentRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';

import { tap } from 'rxjs/operators';

import { DotAddToBundleModule } from '@components/_common/dot-add-to-bundle';
import {
    ComponentStatus,
    DotExperiment,
    DotExperimentStatus,
    ExperimentsStatusList,
    SidebarStatus
} from '@dotcms/dotcms-models';
import { DotMessagePipe, DotMessagePipeModule } from '@dotcms/ui';
import { DotDynamicDirective } from '@portlets/shared/directives/dot-dynamic.directive';

import { DotExperimentsCreateComponent } from './components/dot-experiments-create/dot-experiments-create.component';
import { DotExperimentsEmptyExperimentsComponent } from './components/dot-experiments-empty-experiments/dot-experiments-empty-experiments.component';
import { DotExperimentsListSkeletonComponent } from './components/dot-experiments-list-skeleton/dot-experiments-list-skeleton.component';
import { DotExperimentsListTableComponent } from './components/dot-experiments-list-table/dot-experiments-list-table.component';
import { DotExperimentsStatusFilterComponent } from './components/dot-experiments-status-filter/dot-experiments-status-filter.component';
import { DotExperimentsListStore, VmListExperiments } from './store/dot-experiments-list-store';

import { DotExperimentsUiHeaderComponent } from '../shared/ui/dot-experiments-header/dot-experiments-ui-header.component';

@Component({
    standalone: true,
    selector: 'dot-experiments-list',
    imports: [
        AsyncPipe,
        NgIf,
        DotExperimentsListSkeletonComponent,
        DotExperimentsEmptyExperimentsComponent,
        DotExperimentsStatusFilterComponent,
        DotExperimentsListTableComponent,
        DotExperimentsUiHeaderComponent,
        DotDynamicDirective,
        DotMessagePipeModule,
        ButtonModule,
        ConfirmDialogModule,
        DotAddToBundleModule
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
     * Go to the experiment report or configuration depending on the experiment status
     * @param {DotExperiment} experiment - Experiment to navigate to
     * @returns void
     * @memberof DotExperimentsShellComponent
     */

    goToContainerAction(experiment: DotExperiment) {
        const route = ['/edit-page/experiments/', experiment.pageId, experiment.id];

        if (
            experiment.status === DotExperimentStatus.RUNNING ||
            experiment.status === DotExperimentStatus.ENDED
        ) {
            route.push('reports');
        } else {
            route.push('configuration');
        }

        this.router.navigate([...route], {
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
