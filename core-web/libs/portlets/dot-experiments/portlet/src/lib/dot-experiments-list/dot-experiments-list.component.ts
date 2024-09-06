import { provideComponentStore } from '@ngrx/component-store';
import { Observable } from 'rxjs';

import { AsyncPipe, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, ComponentRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';

import { tap } from 'rxjs/operators';

import { DotMessageService } from '@dotcms/data-access';
import {
    ComponentStatus,
    CONFIGURATION_CONFIRM_DIALOG_KEY,
    DotExperiment,
    DotExperimentStatus,
    ExperimentsStatusList,
    SidebarStatus
} from '@dotcms/dotcms-models';
import {
    DotAddToBundleComponent,
    DotDynamicDirective,
    DotEmptyContainerComponent,
    DotMessagePipe,
    PrincipalConfiguration
} from '@dotcms/ui';

import { DotExperimentsCreateComponent } from './components/dot-experiments-create/dot-experiments-create.component';
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
        DotExperimentsStatusFilterComponent,
        DotExperimentsListTableComponent,
        DotExperimentsUiHeaderComponent,
        DotDynamicDirective,
        DotMessagePipe,
        ButtonModule,
        ConfirmDialogModule,
        DotAddToBundleComponent,
        DotEmptyContainerComponent
    ],
    templateUrl: './dot-experiments-list.component.html',
    styleUrls: ['./dot-experiments-list.component.scss'],
    providers: [provideComponentStore(DotExperimentsListStore)],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsListComponent {
    @ViewChild(DotDynamicDirective, { static: true }) sidebarHost!: DotDynamicDirective;
    vm$: Observable<VmListExperiments> = this.dotExperimentsListStore.vm$.pipe(
        tap(({ sidebar }) => this.handleSidebar(sidebar))
    );
    statusOptionList = ExperimentsStatusList;
    confirmDialogKey = CONFIGURATION_CONFIRM_DIALOG_KEY;

    protected readonly emptyConfiguration: PrincipalConfiguration = {
        title: this.dotMessageService.get('experimentspage.not.experiments.founds'),
        icon: 'pi-filter-fill rotate-180'
    };
    private componentRef: ComponentRef<DotExperimentsCreateComponent>;

    constructor(
        private readonly dotExperimentsListStore: DotExperimentsListStore,
        private readonly router: Router,
        private readonly dotMessageService: DotMessageService
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
