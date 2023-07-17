import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import {
    Goals,
    GOALS_METADATA_MAP,
    RangeOfDateAndTime,
    SummaryLegend
} from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-experiments-experiment-summary',
    standalone: true,
    imports: [CommonModule, DotMessagePipe],
    templateUrl: './dot-experiments-experiment-summary.component.html',
    styleUrls: ['./dot-experiments-experiment-summary.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsExperimentSummaryComponent {
    goalTypeMap = GOALS_METADATA_MAP;

    @Input()
    goals: Goals;

    @Input()
    scheduling: RangeOfDateAndTime;

    @Input()
    sessionsReached: number;

    @Input()
    suggestedWinner: SummaryLegend | null = null;
}
