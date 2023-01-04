import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { ButtonModule } from 'primeng/button';

import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { UiDotIconButtonTooltipModule } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.module';
import { DotIconModule } from '@dotcms/ui';
import {
    DEFAULT_VARIANT_NAME,
    DotExperiment,
    EditPageTabs,
    ExperimentSteps,
    MAX_VARIANTS_ALLOWED,
    SidebarStatus,
    Status,
    StepStatus,
    Variant
} from '@dotcms/dotcms-models';
import { DotExperimentsConfigurationVariantsAddComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-variants-add/dot-experiments-configuration-variants-add.component';
import { InputTextModule } from 'primeng/inputtext';
import { DotExperimentsConfigurationItemsCountComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-items-count/dot-experiments-configuration-items-count.component';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { InplaceModule } from 'primeng/inplace';

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
        InputTextModule
    ],
    templateUrl: './dot-experiments-configuration-variants.component.html',
    styleUrls: ['./dot-experiments-configuration-variants.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationVariantsComponent {
    statusList = Status;
    sidebarStatusList = SidebarStatus;
    maxVariantsAllowed = MAX_VARIANTS_ALLOWED;
    defaultVariantName = DEFAULT_VARIANT_NAME;
    experimentStepName = ExperimentSteps.VARIANTS;
    loading: boolean;

    @Input() stepStatus: StepStatus;
    @Input() variants: Variant[];
    @Output() sidebarStatusChanged = new EventEmitter<SidebarStatus>();
    @Output() delete = new EventEmitter<Variant>();
    @Output() edit = new EventEmitter<Pick<DotExperiment, 'name' | 'id'>>();
    @Output() save = new EventEmitter<Pick<DotExperiment, 'name'>>();
    @Output() goToEditPage = new EventEmitter<{ variant: Variant; mode: EditPageTabs }>();

    editVariantName(newVariantName: string, variant: Variant) {
        this.edit.emit({
            ...variant,
            name: newVariantName
        });
        this.loading = true;
    }
}
