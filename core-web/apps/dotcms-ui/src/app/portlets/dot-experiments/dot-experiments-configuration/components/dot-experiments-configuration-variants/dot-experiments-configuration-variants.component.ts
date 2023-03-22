import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InplaceModule } from 'primeng/inplace';
import { InputTextModule } from 'primeng/inputtext';
import { TooltipModule } from 'primeng/tooltip';

import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { UiDotIconButtonTooltipModule } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.module';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import {
    ComponentStatus,
    DEFAULT_VARIANT_NAME,
    DotExperiment,
    EditPageTabs,
    ExperimentSteps,
    MAX_VARIANTS_ALLOWED,
    SIDEBAR_STATUS,
    StepStatus,
    Variant
} from '@dotcms/dotcms-models';
import { DotIconModule } from '@dotcms/ui';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotExperimentsConfigurationItemsCountComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-items-count/dot-experiments-configuration-items-count.component';
import { DotExperimentsConfigurationVariantsAddComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-variants-add/dot-experiments-configuration-variants-add.component';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';

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

        //PrimeNg
        CardModule,
        InplaceModule,
        ButtonModule,
        InputTextModule,
        TooltipModule
    ],
    templateUrl: './dot-experiments-configuration-variants.component.html',
    styleUrls: ['./dot-experiments-configuration-variants.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationVariantsComponent {
    vm$: Observable<{ status: StepStatus; isExperimentADraft: boolean }> =
        this.dotExperimentsConfigurationStore.variantsStepVm$;
    statusList = ComponentStatus;
    sidebarStatusList = SIDEBAR_STATUS;
    maxVariantsAllowed = MAX_VARIANTS_ALLOWED;
    defaultVariantName = DEFAULT_VARIANT_NAME;
    experimentStepName = ExperimentSteps.VARIANTS;

    @Input() variants: Variant[];
    @Output() sidebarStatusChanged = new EventEmitter<SIDEBAR_STATUS>();
    @Output() delete = new EventEmitter<{ $event: MouseEvent; variant: Variant }>();
    @Output() edit = new EventEmitter<Pick<DotExperiment, 'name' | 'id'>>();
    @Output() save = new EventEmitter<Pick<DotExperiment, 'name'>>();
    @Output() goToEditPage = new EventEmitter<{ variant: Variant; mode: EditPageTabs }>();

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore
    ) {}

    /**
     * Edit the name of the selected variant
     * @param {string} newVariantName
     * @param {Variant} variant
     */
    editVariantName(newVariantName: string, variant: Variant) {
        this.edit.emit({
            ...variant,
            name: newVariantName
        });
    }
}
