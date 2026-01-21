import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { Goals, RangeOfDateAndTime, SummaryLegend } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-experiments-experiment-summary',
    imports: [CommonModule, DotMessagePipe],
    templateUrl: './dot-experiments-experiment-summary.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex justify-center p-4 text-gray-800 leading-[140%]'
    }
})
export class DotExperimentsExperimentSummaryComponent {
    goals = input<Goals>();
    scheduling = input<RangeOfDateAndTime>();
    sessionsReached = input<number>();
    suggestedWinner = input<SummaryLegend | null>(null);

    updateResults = output();
}
