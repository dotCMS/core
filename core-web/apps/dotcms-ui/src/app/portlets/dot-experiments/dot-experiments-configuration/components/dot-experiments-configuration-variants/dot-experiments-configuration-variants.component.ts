import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CardModule } from 'primeng/card';
import { TrafficProportion } from '@portlets/dot-experiments/shared/models/dot-experiments.model';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { ButtonModule } from 'primeng/button';
import { MAX_VARIANTS_ALLOWED } from '@portlets/dot-experiments/shared/models/dot-experiments-constants';
import { DotExperimentsConfigurationItemsCountComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-items-count/dot-experiments-configuration-items-count.component';
import { DotIconModule } from '@dotcms/ui';

@Component({
    selector: 'dot-experiments-configuration-variants',
    standalone: true,
    imports: [
        CommonModule,
        DotExperimentsConfigurationItemsCountComponent,
        DotMessagePipeModule,
        DotIconModule,
        //PrimeNg
        CardModule,
        ButtonModule
    ],
    templateUrl: './dot-experiments-configuration-variants.component.html',
    styleUrls: ['./dot-experiments-configuration-variants.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationVariantsComponent {
    maxLength = MAX_VARIANTS_ALLOWED;
    @Input()
    trafficProportion: TrafficProportion;

    changeTrafficProportionType() {
        // to implemented
    }

    viewVariant() {
        // to be implemented
    }

    addNewVariant() {
        // to be implemented
    }
}
