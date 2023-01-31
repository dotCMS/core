import { Observable } from 'rxjs';

import { ChangeDetectionStrategy, Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { DotMessagePipe } from '@dotcms/app/view/pipes';
import { DotSessionStorageService } from '@dotcms/data-access';
import {
    DOT_EXPERIMENT_STATUS_METADATA_MAP,
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
    experimentStatusMap: Record<DotExperimentStatusList, { classz: string; label: string }>;

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore,
        private readonly dotSessionStorageService: DotSessionStorageService,
        private readonly router: Router,
        private readonly route: ActivatedRoute
    ) {
        this.experimentStatusMap = this.statusTranslations();
    }

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
            data,
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
     * @param {Variant} variant
     * @param {string} experimentId
     * @returns void
     * @memberof DotExperimentsConfigurationComponent
     */
    deleteVariant(variant: Variant, experimentId: string) {
        this.dotExperimentsConfigurationStore.deleteVariant({
            experimentId,
            variant
        });
    }

    /**
     * Go to Edit Page / Content, set the VariantId to SessionStorage
     * @param {Variant} variant
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

    private statusTranslations() {
        const dotMessagePipe = inject(DotMessagePipe);
        const statusWithTranslations = { ...DOT_EXPERIMENT_STATUS_METADATA_MAP };

        Object.keys(DOT_EXPERIMENT_STATUS_METADATA_MAP).forEach((key) => {
            statusWithTranslations[key] = {
                ...DOT_EXPERIMENT_STATUS_METADATA_MAP[key],
                label: dotMessagePipe.transform(DOT_EXPERIMENT_STATUS_METADATA_MAP[key].label)
            };
        });

        return statusWithTranslations;
    }
}
