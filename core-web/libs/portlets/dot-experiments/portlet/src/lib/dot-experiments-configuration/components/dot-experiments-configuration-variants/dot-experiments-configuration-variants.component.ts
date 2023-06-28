import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, ComponentRef, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { ConfirmationService } from 'primeng/api';
import { AutoFocusModule } from 'primeng/autofocus';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { InplaceModule } from 'primeng/inplace';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';

import { tap } from 'rxjs/operators';

import { UiDotIconButtonTooltipModule } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.module';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotSessionStorageService } from '@dotcms/data-access';
import {
    ComponentStatus,
    DEFAULT_VARIANT_NAME,
    DotExperimentStatusList,
    DotPageMode,
    ExperimentSteps,
    MAX_INPUT_TITLE_LENGTH,
    MAX_VARIANTS_ALLOWED,
    StepStatus,
    TrafficProportion,
    Variant
} from '@dotcms/dotcms-models';
import {
    DotIconModule,
    DotMessagePipe,
    DotMessagePipeModule,
    UiDotIconButtonModule
} from '@dotcms/ui';
import { DotDynamicDirective } from '@portlets/shared/directives/dot-dynamic.directive';

import { DotExperimentsInlineEditTextComponent } from '../../../shared/ui/dot-experiments-inline-edit-text/dot-experiments-inline-edit-text.component';
import { DotExperimentsConfigurationStore } from '../../store/dot-experiments-configuration-store';
import { DotExperimentsConfigurationItemsCountComponent } from '../dot-experiments-configuration-items-count/dot-experiments-configuration-items-count.component';
import { DotExperimentsConfigurationVariantsAddComponent } from '../dot-experiments-configuration-variants-add/dot-experiments-configuration-variants-add.component';

@Component({
    selector: 'dot-experiments-configuration-variants',
    standalone: true,
    imports: [
        CommonModule,
        DotMessagePipeModule,
        DotIconModule,
        UiDotIconButtonModule,
        UiDotIconButtonTooltipModule,
        DotExperimentsConfigurationVariantsAddComponent,
        DotCopyButtonModule,
        DotExperimentsConfigurationItemsCountComponent,
        DotDynamicDirective,

        //PrimeNg
        CardModule,
        InplaceModule,
        ButtonModule,
        InputTextModule,
        TooltipModule,
        ConfirmPopupModule,
        AutoFocusModule,
        DotExperimentsInlineEditTextComponent
    ],
    templateUrl: './dot-experiments-configuration-variants.component.html',
    styleUrls: ['./dot-experiments-configuration-variants.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationVariantsComponent {
    vm$: Observable<{
        experimentId: string;
        trafficProportion: TrafficProportion;
        status: StepStatus;
        isExperimentADraft: boolean;
    }> = this.dotExperimentsConfigurationStore.variantsStepVm$.pipe(
        tap(({ status }) => this.handleSidebar(status))
    );
    dotPageMode = DotPageMode;
    @ViewChild(DotDynamicDirective, { static: true }) sidebarHost!: DotDynamicDirective;
    protected readonly statusList = ComponentStatus;
    protected readonly maxVariantsAllowed = MAX_VARIANTS_ALLOWED;
    protected readonly defaultVariantName = DEFAULT_VARIANT_NAME;
    protected readonly maxInputTitleLength = MAX_INPUT_TITLE_LENGTH;
    protected readonly DotExperimentStatusList = DotExperimentStatusList;
    private componentRef: ComponentRef<DotExperimentsConfigurationVariantsAddComponent>;

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore,
        private readonly confirmationService: ConfirmationService,
        private readonly dotMessagePipe: DotMessagePipe,
        private readonly dotSessionStorageService: DotSessionStorageService,
        private readonly router: Router,
        private readonly route: ActivatedRoute
    ) {}

    /**
     * Edit the name of the selected variant
     * @param {string} newVariantName
     * @param {Variant} variant
     * @param {string} experimentId
     * @memberof DotExperimentsConfigurationVariantsComponent
     */
    editVariantName(newVariantName: string, variant: Variant, experimentId: string) {
        this.dotExperimentsConfigurationStore.editVariant({
            data: {
                ...variant,
                name: newVariantName
            },
            experimentId
        });
    }

    /**
     * Open sidebar to Add a new Variant
     * @returns void
     * @memberof DotExperimentsConfigurationVariantsComponent
     */
    addVariant() {
        this.dotExperimentsConfigurationStore.openSidebar(ExperimentSteps.VARIANTS);
    }

    /**
     * Open sidebar to set Traffic Proportion
     * @returns void
     * @memberof DotExperimentsConfigurationVariantsComponent
     */
    changeTrafficProportion() {
        this.dotExperimentsConfigurationStore.openSidebar(ExperimentSteps.TRAFFICS_SPLIT);
    }

    /**
     * Delete a specific variant
     * @param {{ $event: MouseEvent; variant: Variant }} event
     * @param {string} experimentId
     * @returns void
     * @memberof DotExperimentsConfigurationVariantsComponent
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
     * @param {Variant} variant
     * @param {DotPageMode} mode
     * @returns void
     * @memberof DotExperimentsConfigurationVariantsComponent
     */
    goToEditPageVariant(variant: Variant, mode: DotPageMode) {
        this.dotSessionStorageService.setVariationId(variant.id);
        this.router.navigate(['edit-page/content'], {
            queryParams: {
                variantName: variant.id,
                experimentId: this.route.snapshot.params['experimentId'],
                mode: mode
            },
            queryParamsHandling: 'merge'
        });
    }

    private handleSidebar(status: StepStatus) {
        if (status && status.isOpen && status.status != ComponentStatus.SAVING) {
            this.loadSidebarComponent();
        } else {
            this.removeSidebarComponent();
        }
    }

    private loadSidebarComponent(): void {
        this.sidebarHost.viewContainerRef.clear();
        this.componentRef =
            this.sidebarHost.viewContainerRef.createComponent<DotExperimentsConfigurationVariantsAddComponent>(
                DotExperimentsConfigurationVariantsAddComponent
            );
    }

    private removeSidebarComponent() {
        if (this.componentRef) {
            this.sidebarHost.viewContainerRef.clear();
        }
    }
}
