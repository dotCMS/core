import { DatePipe, LowerCasePipe, NgIf, TitleCasePipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    Input,
    OnChanges,
    Output,
    SimpleChanges
} from '@angular/core';
import { RouterLink } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { ChipModule } from 'primeng/chip';
import { SkeletonModule } from 'primeng/skeleton';

import {
    DotExperiment,
    DotExperimentStatus,
    ExperimentsStatusIcons,
    RUNNING_UNTIL_DATE_FORMAT
} from '@dotcms/dotcms-models';
import { DotIconModule, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

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
        DotSafeHtmlPipe,
        DotMessagePipe,
        SkeletonModule,
        ButtonModule,
        ChipModule
    ],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsUiHeaderComponent implements OnChanges {
    @Input()
    title = '';

    @Input()
    experiment: DotExperiment;

    @Input()
    isLoading: boolean;

    @Output()
    goBack = new EventEmitter<boolean>();

    runningUntilDateFormat = RUNNING_UNTIL_DATE_FORMAT;
    statusIcon: string;
    protected readonly experimentStatus = DotExperimentStatus;

    ngOnChanges(changes: SimpleChanges): void {
        const { experiment } = changes;
        if (experiment && experiment.currentValue) {
            const { status } = experiment.currentValue;
            this.statusIcon = ExperimentsStatusIcons[status];
        }
    }
}
