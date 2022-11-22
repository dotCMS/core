import { ChangeDetectionStrategy, Component } from '@angular/core';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store.service';
import { tap } from 'rxjs/operators';
import { Router } from '@angular/router';
import { provideComponentStore } from '@ngrx/component-store';
import {
    DotExperiment,
    EditPageTabs,
    ExperimentSteps,
    Variant
} from '@portlets/dot-experiments/shared/models/dot-experiments.model';
import { DotExperimentsSessionStorageService } from '@portlets/dot-experiments/shared/services/dot-experiments-session-storage.service';

@Component({
    selector: 'dot-experiments-configuration',
    templateUrl: './dot-experiments-configuration.component.html',
    styleUrls: ['./dot-experiments-configuration.component.scss'],
    providers: [provideComponentStore(DotExperimentsConfigurationStore)],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationComponent {
    pageId: string;
    vm$ = this.dotExperimentsConfigurationStore.vm$.pipe(tap((vm) => (this.pageId = vm.pageId)));

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore,
        private readonly dotExperimentsSessionStorageService: DotExperimentsSessionStorageService,
        private readonly router: Router
    ) {}

    /**
     * Go to Experiment List
     * @returns void
     * @memberof DotExperimentsConfigurationComponent
     */
    goToExperimentList() {
        this.router.navigate(['/edit-page/experiments/', this.pageId], {
            queryParamsHandling: 'preserve'
        });
    }

    /**
     * Show the sidebar form to add variant
     * @returns void
     * @memberof DotExperimentsConfigurationComponent
     */
    showAddVariant() {
        this.dotExperimentsConfigurationStore.openSidebar(ExperimentSteps.VARIANTS);
    }

    /**
     * Hide sidebar
     * @returns void
     * @memberof DotExperimentsConfigurationComponent
     */
    closeSidebar() {
        this.dotExperimentsConfigurationStore.closeSidebar();
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
        this.dotExperimentsSessionStorageService.setVariationId(variant.variant.id);
        this.router.navigate(['edit-page/content'], {
            queryParams: { editPageTab: variant.mode, variationName: variant.variant.id },
            queryParamsHandling: 'merge'
        });
    }
}
