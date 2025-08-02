import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { Goals, RangeOfDateAndTime, SummaryLegend } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-experiments-experiment-summary',
    imports: [CommonModule, DotMessagePipe, ButtonModule],
    templateUrl: './dot-experiments-experiment-summary.component.html',
    styleUrls: ['./dot-experiments-experiment-summary.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsExperimentSummaryComponent {
    @Input()
    goals: Goals;

    @Input()
    scheduling: RangeOfDateAndTime;

    @Input()
    sessionsReached: number;

    @Input()
    suggestedWinner: SummaryLegend | null = null;

    @Output()
    updateResults = new EventEmitter();
}
