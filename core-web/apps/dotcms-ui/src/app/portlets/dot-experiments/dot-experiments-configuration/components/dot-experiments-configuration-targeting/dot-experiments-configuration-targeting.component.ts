import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';

@Component({
    selector: 'dot-experiments-configuration-targeting',
    standalone: true,
    imports: [CommonModule, CardModule, DotMessagePipeModule, ButtonModule],
    templateUrl: './dot-experiments-configuration-targeting.component.html',
    styleUrls: ['./dot-experiments-configuration-targeting.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationTargetingComponent {
    setupTargeting() {
        // to be implemented
    }
}
