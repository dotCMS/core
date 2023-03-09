import { LowerCasePipe, NgForOf, NgIf } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

import { ConfirmPopupModule } from 'primeng/confirmpopup';

import { Goal, GOALS_METADATA_MAP } from '@dotcms/dotcms-models';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

/**
 * Component to show the conditions of a specific Goal
 */
@Component({
    selector: 'dot-experiments-experiment-goal-configuration-detail',
    standalone: true,
    imports: [ConfirmPopupModule, DotMessagePipeModule, NgIf, NgForOf, LowerCasePipe],
    templateUrl: './dot-experiments-experiment-goal-configuration-detail.component.html',
    styleUrls: ['./dot-experiments-experiment-goal-configuration-detail.component.scss'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsExperimentGoalConfigurationDetailComponent {
    goalTypeMap = GOALS_METADATA_MAP;

    @Input()
    goal!: Goal;
}
