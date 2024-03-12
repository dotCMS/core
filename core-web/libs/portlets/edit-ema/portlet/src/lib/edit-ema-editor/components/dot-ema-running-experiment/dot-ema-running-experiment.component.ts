import { Observable, of } from 'rxjs';

import { AsyncPipe, DatePipe, NgIf, TitleCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnInit, inject } from '@angular/core';
import { RouterLink, RouterModule } from '@angular/router';

import { TagModule } from 'primeng/tag';

import { catchError, map } from 'rxjs/operators';

import {
    DotExperiment,
    DotExperimentStatus,
    RUNNING_UNTIL_DATE_FORMAT
} from '@dotcms/dotcms-models';
import { DotExperimentsService } from '@dotcms/portlets/dot-experiments/data-access';
import { DotMessagePipe } from '@dotcms/ui';

@Component({
    selector: 'dot-ema-running-experiment',
    standalone: true,
    imports: [
        TagModule,
        RouterModule,
        RouterLink,
        DotMessagePipe,
        TitleCasePipe,
        DatePipe,
        NgIf,
        AsyncPipe
    ],
    templateUrl: './dot-ema-running-experiment.component.html',
    styleUrl: './dot-ema-running-experiment.component.scss',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotEmaRunningExperimentComponent implements OnInit {
    @Input() pageId: string;

    protected readonly dotExperimentsService = inject(DotExperimentsService);

    protected runningExperiment$: Observable<DotExperiment>;

    protected runningUntilDateFormat = RUNNING_UNTIL_DATE_FORMAT;

    ngOnInit(): void {
        this.runningExperiment$ = this.dotExperimentsService
            .getByStatus(this.pageId, DotExperimentStatus.RUNNING)
            .pipe(
                catchError(() => of(null)),
                map((experiments) => (experiments && experiments.length ? experiments[0] : null))
            );
    }
}
