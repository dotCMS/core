import { Observable } from 'rxjs';

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TooltipModule } from 'primeng/tooltip';

import { tap } from 'rxjs/operators';

import { StepStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

import { DotExperimentsConfigurationStore } from '../../store/dot-experiments-configuration-store';

@Component({
    selector: 'dot-experiments-configuration-targeting',
    standalone: true,
    imports: [CommonModule, CardModule, DotMessagePipe, ButtonModule, TooltipModule],
    templateUrl: './dot-experiments-configuration-targeting.component.html',
    styleUrls: ['./dot-experiments-configuration-targeting.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsConfigurationTargetingComponent {
    vm$: Observable<{
        experimentId: string;
        status: StepStatus;
        isExperimentADraft: boolean;
    }> = this.dotExperimentsConfigurationStore.targetStepVm$.pipe(
        tap(({ status }) => this.handleSidebar(status))
    );

    constructor(
        private readonly dotExperimentsConfigurationStore: DotExperimentsConfigurationStore
    ) {}

    setupTargeting() {
        // to be implemented
    }

    private handleSidebar(_status: StepStatus) {
        // to be implemented
    }
}
