import { ChangeDetectionStrategy, Component, OnInit } from '@angular/core';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store.service';
import { ActivatedRoute, Router } from '@angular/router';

import { DotSessionStorageService } from '@dotcms/data-access';
import {
    DotExperiment,
    EditPageTabs,
    ExperimentSteps,
    SidebarStatus,
    Variant
} from '@dotcms/dotcms-models';

@Component({
    selector: 'dot-experiments-configuration',
    templateUrl: './dot-experiments-configuration.component.html',
    styleUrls: ['./dot-experiments-configuration.component.scss'],
    providers: [DotExperimentsConfigurationStore],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationComponent implements OnInit {
    vm$ = this.dotExperimentsConfigurationStore.vm$;

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
     * @returns void
     * @memberof DotExperimentsConfigurationComponent
     */
    goToExperimentList(pageId: string) {
        this.router.navigate(['/edit-page/experiments/', pageId], {
            queryParamsHandling: 'preserve'
        });
    }

    /**
     * Open/Close sidebar
     * @returns void
     * @memberof DotExperimentsConfigurationComponent
     */
    sidebarStatusChanged(action: SidebarStatus) {
        if (action === SidebarStatus.OPEN) {
            this.dotExperimentsConfigurationStore.openSidebar(ExperimentSteps.VARIANTS);
        } else {
            this.dotExperimentsConfigurationStore.closeSidebar();
        }
    }

    /**
     * Save a specific variant
     * @param {Pick<DotExperiment, 'name'>} variant
     * @returns void
     * @memberof DotExperimentsConfigurationComponent
     */
    saveVariant(variant: Pick<DotExperiment, 'name'>) {
        this.dotExperimentsConfigurationStore.addVariant(variant);
    }

    /**
     * Delete a specific variant
     * @param {Variant} variant
     * @returns void
     * @memberof DotExperimentsConfigurationComponent
     */
    deleteVariant(variant: Variant) {
        this.dotExperimentsConfigurationStore.deleteVariant(variant);
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
            queryParams: { editPageTab: variant.mode, variationName: variant.variant.id },
            queryParamsHandling: 'merge'
        });
    }
}
