import { DatePipe, TitleCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
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
    readonly $runningExperiment = input.required<DotExperiment>({ alias: 'runningExperiment' });

    /**
     * Whether the tag is an action rather than a destination (#37478).
     *
     * The editor is told which experiment is running on the page they are looking at; with the
     * panel available, being told should not cost them that page. The caller decides, because
     * whether a panel exists is not this component's business — the same split
     * `dot-ema-info-display` uses.
     *
     * `false` keeps the legacy reports route exactly as it is, which is what FR-017 (#37005)
     * constrains and what the switch-off path must keep.
     */
    readonly $asAction = input<boolean>(false, { alias: 'asAction' });

    /** Emitted instead of navigating when {@link $asAction} is set. */
    readonly viewResults = output<DotExperiment>();

    protected runningUntilDateFormat = RUNNING_UNTIL_DATE_FORMAT;

    protected onTagClick(): void {
        if (!this.$asAction()) {
            return;
        }

        this.viewResults.emit(this.$runningExperiment());
    }
}
