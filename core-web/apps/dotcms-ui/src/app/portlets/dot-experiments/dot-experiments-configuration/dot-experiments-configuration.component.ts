import { Observable } from 'rxjs';

import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService } from 'primeng/api';

import { DotMessagePipe } from '@dotcms/app/view/pipes';
import { DotSessionStorageService } from '@dotcms/data-access';
import {
    DotExperiment,
    DotExperimentStatusList,
    EditPageTabs,
    ExperimentSteps,
    SidebarStatus,
    Variant
} from '@dotcms/dotcms-models';
import {
    ConfigurationViewModel,
    DotExperimentsConfigurationStore
} from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';

@Component({
    selector: 'dot-experiments-configuration',
    templateUrl: './dot-experiments-configuration.component.html',
    styleUrls: ['./dot-experiments-configuration.component.scss'],
    providers: [DotExperimentsConfigurationStore, DotMessagePipe],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationComponent implements OnInit {
    vm$: Observable<ConfigurationViewModel> = this.dotExperimentsConfigurationStore.vm$;
    experimentSteps = ExperimentSteps;
    experimentStatus = DotExperimentStatusList;

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore,
        private readonly dotSessionStorageService: DotSessionStorageService,
        private readonly router: Router,
        private readonly route: ActivatedRoute,
        private readonly confirmationService: ConfirmationService,
        private readonly dotMessagePipe: DotMessagePipe
    ) {}

    ngOnInit(): void {
        this.dotExperimentsConfigurationStore.loadExperiment(
            this.route.snapshot.params.experimentId
        );
    }

    /**
     * Go to Experiment List
     * @param {string} pageId
     * @returns void
     * @memberof DotExperimentsConfigurationComponent
     */
    goToExperimentList(pageId: string) {
        this.router.navigate(['/edit-page/experiments/', pageId], {
            queryParams: {
                editPageTab: null,
                variantName: null,
                experimentId: null
            },
            queryParamsHandling: 'merge'
        });
    }

    /**
     * Run the Experiment
     * @param {DotExperiment} experiment
     * @returns void
     * @memberof DotExperimentsConfigurationVariantsComponent
     */
    runExperiment(experiment: DotExperiment) {
        this.dotExperimentsConfigurationStore.startExperiment(experiment);
    }

    /**
     * Stop the Experiment
     * @param {MouseEvent} $event
     * @param {DotExperiment} experiment
     * @returns void
     * @memberof DotExperimentsConfigurationVariantsComponent
     */
    stopExperiment($event: MouseEvent, experiment: DotExperiment) {
        this.confirmationService.confirm({
            target: $event.target,
            message: this.dotMessagePipe.transform('experiments.action.stop.delete-confirm'),
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: this.dotMessagePipe.transform('stop'),
            rejectLabel: this.dotMessagePipe.transform('dot.common.dialog.reject'),
            accept: () => {
                this.dotExperimentsConfigurationStore.stopExperiment(experiment);
            }
        });
    }

    /**
     * Sidebar controller
     * @param {SidebarStatus} action
     * @param {ExperimentSteps} step
     * @returns void
     * @memberof DotExperimentsConfigurationComponent
     */
    sidebarStatusController(action: SidebarStatus, step?: ExperimentSteps) {
        if (action === SidebarStatus.OPEN) {
            this.dotExperimentsConfigurationStore.openSidebar(step);
        } else {
            this.dotExperimentsConfigurationStore.closeSidebar();
        }
    }

    /**
     * Save a specific variant
     * @param data
     * @param {string} experimentId
     * @returns void
     * @memberof DotExperimentsConfigurationComponent
     */
    saveVariant(data: Pick<DotExperiment, 'name'>, experimentId: string) {
        this.dotExperimentsConfigurationStore.addVariant({
            name: data.name,
            experimentId
        });
    }

    /**
     * Edit a specific variant
     * @param data
     * @param {string} experimentId
     * @returns void
     * @memberof DotExperimentsConfigurationComponent
     */
    editVariant(data: Pick<DotExperiment, 'name' | 'id'>, experimentId: string) {
        this.dotExperimentsConfigurationStore.editVariant({
            data,
            experimentId
        });
    }

    /**
     * Delete a specific variant
     * @param {{ $event: MouseEvent; variant: Variant }} event
     * @param {string} experimentId
     * @returns void
     * @memberof DotExperimentsConfigurationComponent
     */
    deleteVariant(event: { $event: MouseEvent; variant: Variant }, experimentId: string) {
        this.confirmationService.confirm({
            target: event.$event.target,
            message: this.dotMessagePipe.transform('experiments.configure.variant.delete.confirm'),
            icon: 'pi pi-exclamation-triangle',
            acceptLabel: this.dotMessagePipe.transform('delete'),
            rejectLabel: this.dotMessagePipe.transform('dot.common.dialog.reject'),
            accept: () => {
                this.dotExperimentsConfigurationStore.deleteVariant({
                    experimentId,
                    variant: event.variant
                });
            }
        });
    }

    /**
     * Go to Edit Page / Content, set the VariantId to SessionStorage
     * @param {{ variant: Variant; mode: EditPageTabs }} variant
     * @param {EditPageTabs} tab
     * @returns void
     * @memberof DotExperimentsConfigurationVariantsComponent
     */
    goToEditPageVariant(variant: { variant: Variant; mode: EditPageTabs }) {
        this.dotSessionStorageService.setVariationId(variant.variant.id);
        this.router.navigate(['edit-page/content'], {
            queryParams: {
                editPageTab: variant.mode,
                variantName: variant.variant.id,
                experimentId: this.route.snapshot.params.experimentId
            },
            queryParamsHandling: 'merge'
        });
    }
}
