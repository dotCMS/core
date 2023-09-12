import { DatePipe, LowerCasePipe, NgIf, TitleCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';

import {
    DotExperiment,
    DotExperimentStatus,
    RUNNING_UNTIL_DATE_FORMAT
} from '@dotcms/dotcms-models';
import { DotIconModule, DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@Component({
    standalone: true,
    selector: 'dot-experiments-header',
    templateUrl: './dot-experiments-ui-header.component.html',
    styleUrls: ['./dot-experiments-ui-header.component.scss'],
    imports: [
        RouterLink,
        NgIf,
        LowerCasePipe,
        DatePipe,
        TitleCasePipe,
        DotIconModule,
        DotPipesModule,
        DotMessagePipe,
        SkeletonModule,
        TagModule,
        ButtonModule
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsUiHeaderComponent {
    @Input()
    title = '';

    @Input()
    experiment: DotExperiment;

    @Input()
    isLoading: boolean;

    @Output()
    goBack = new EventEmitter<boolean>();

    experimentStatus = DotExperimentStatus;
    runningUntilDateFormat = RUNNING_UNTIL_DATE_FORMAT;
}
