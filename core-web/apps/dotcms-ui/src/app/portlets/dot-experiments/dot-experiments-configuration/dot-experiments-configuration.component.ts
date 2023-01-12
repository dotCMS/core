import { Observable } from 'rxjs';

import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { DotSessionStorageService } from '@dotcms/data-access';
import {
    DotExperiment,
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
    providers: [DotExperimentsConfigurationStore],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationComponent implements OnInit {
    vm$: Observable<ConfigurationViewModel> = this.dotExperimentsConfigurationStore.vm$;
    experimentSteps = ExperimentSteps;

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore,
        private readonly dotSessionStorageService: DotSessionStorageService,
        private readonly router: Router,
        private readonly route: ActivatedRoute
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
                variationName: null,
                experimentId: null
            },
            queryParamsHandling: 'merge'
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
                variationName: variant.variant.id,
                experimentId: this.route.snapshot.params.experimentId
            },
            queryParamsHandling: 'merge'
        });
    }
}
