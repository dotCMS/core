import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';

import { TrafficProportionTypes } from '@dotcms/dotcms-models';
import { DotIconModule } from '@dotcms/ui';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

@Component({
    selector: 'dot-experiments-configuration-traffic',
    standalone: true,
    imports: [CommonModule, DotMessagePipeModule, CardModule, ButtonModule, DotIconModule],
    templateUrl: './dot-experiments-configuration-traffic.component.html',
    styleUrls: ['./dot-experiments-configuration-traffic.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationTrafficComponent {
    @Input()
    trafficAllocation: string;
    trafficProportionType: string;

    @Input()
    set TrafficProportionTypes(type: TrafficProportionTypes) {
        this.trafficProportionType =
            type === TrafficProportionTypes.SPLIT_EVENLY
                ? 'experiments.configure.traffic.split.evenly'
                : 'experiments.configure.traffic.split.custom';
    }

    changeTrafficAllocation() {
        //to be implemented
    }

    changeTrafficProportion() {
        //to be implemented
    }
}
