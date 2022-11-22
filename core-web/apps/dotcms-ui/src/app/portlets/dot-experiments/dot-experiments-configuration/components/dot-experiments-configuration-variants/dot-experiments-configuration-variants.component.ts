import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { ButtonModule } from 'primeng/button';
import {
    DEFAULT_VARIANT_ID,
    MAX_VARIANTS_ALLOWED
} from '@portlets/dot-experiments/shared/models/dot-experiments-constants';
import { DotExperimentsConfigurationItemsCountComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-items-count/dot-experiments-configuration-items-count.component';
import { DotIconModule } from '@dotcms/ui';
import { DotDynamicDirective } from '@portlets/shared/directives/dot-dynamic.directive';
import { DotExperimentsConfigurationVariantsAddComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-variants-add/dot-experiments-configuration-variants-add.component';
import {
    DotExperiment,
    EditPageTabs,
    ExperimentSteps,
    StepStatus,
    Variant
} from '@portlets/dot-experiments/shared/models/dot-experiments.model';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { UiDotIconButtonTooltipModule } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.module';
import { Status } from '@portlets/shared/models/shared-models';

@Component({
    selector: 'dot-experiments-configuration-variants',
    standalone: true,
    imports: [
        CommonModule,
        DotExperimentsConfigurationItemsCountComponent,
        DotMessagePipeModule,
        DotIconModule,
        DotDynamicDirective,
        UiDotIconButtonModule,
        //PrimeNg
        CardModule,
        ButtonModule,
        UiDotIconButtonTooltipModule,
        DotExperimentsConfigurationVariantsAddComponent
    ],
    templateUrl: './dot-experiments-configuration-variants.component.html',
    styleUrls: ['./dot-experiments-configuration-variants.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationVariantsComponent {
    currentStep = ExperimentSteps.VARIANTS;
    currentStatus = Status;
    maxVariantsAllowed = MAX_VARIANTS_ALLOWED;
    defaultVariantId = DEFAULT_VARIANT_ID;

    @Input() status: StepStatus;
    @Input() variants: Variant[];
    @Output() showSidebar = new EventEmitter<void>();
    @Output() hiddenSidebar = new EventEmitter<void>();
    @Output() delete = new EventEmitter<Variant>();
    @Output() save = new EventEmitter<Pick<DotExperiment, 'name'>>();
    @Output() goToEditPage = new EventEmitter<{ variant: Variant; mode: EditPageTabs }>();
}
