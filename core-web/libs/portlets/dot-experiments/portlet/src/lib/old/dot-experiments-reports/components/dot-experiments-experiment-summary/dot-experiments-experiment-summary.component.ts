import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { Goals, RangeOfDateAndTime, SummaryLegend } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-experiments-experiment-summary',
    imports: [DotMessagePipe, DatePipe, DecimalPipe],
    templateUrl: './dot-experiments-experiment-summary.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'flex justify-center p-4 text-gray-800 leading-[140%]'
    }
})
export class DotExperimentsExperimentSummaryComponent {
    $goals = input.required<Goals>({ alias: 'goals' });
    $scheduling = input.required<RangeOfDateAndTime>({ alias: 'scheduling' });
    $sessionsReached = input.required<number>({ alias: 'sessionsReached' });
    $suggestedWinner = input<SummaryLegend | null>(null, { alias: 'suggestedWinner' });

    updateResults = output<void>();
}
