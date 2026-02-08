import { DatePipe, TitleCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input } from '@angular/core';
import { RouterLink, RouterModule } from '@angular/router';

import { TagModule } from 'primeng/tag';

import { DotExperiment, RUNNING_UNTIL_DATE_FORMAT } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-ema-running-experiment',
    imports: [TagModule, RouterModule, RouterLink, DotMessagePipe, TitleCasePipe, DatePipe],
    templateUrl: './dot-ema-running-experiment.component.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEmaRunningExperimentComponent {
    @Input() runningExperiment: DotExperiment;

    protected runningUntilDateFormat = RUNNING_UNTIL_DATE_FORMAT;
}
