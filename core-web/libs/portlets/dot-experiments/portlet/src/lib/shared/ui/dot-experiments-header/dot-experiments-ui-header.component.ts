import { DatePipe, TitleCasePipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    inject,
    Input,
    OnChanges,
    Output,
    SimpleChanges
} from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';

import { DotMessageService } from '@dotcms/data-access';
import {
    DotExperiment,
    DotExperimentStatus,
    ExperimentsStatusIcons,
    RUNNING_UNTIL_DATE_FORMAT
} from '@dotcms/dotcms-models';

type TagSeverity = 'success' | 'info' | 'warn' | 'secondary';

@Component({
    selector: 'dot-experiments-header',
    templateUrl: './dot-experiments-ui-header.component.html',
    imports: [SkeletonModule, ButtonModule, TagModule],
    providers: [DatePipe, TitleCasePipe],
    changeDetection: ChangeDetectionStrategy.OnPush,
    host: {
        class: 'w-full block'
    }
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
    private readonly dotMessageService = inject(DotMessageService);
    private readonly titleCasePipe = inject(TitleCasePipe);
    private readonly datePipe = inject(DatePipe);
    protected readonly statusSeverityMap: Record<DotExperimentStatus, TagSeverity> = {
        [DotExperimentStatus.RUNNING]: 'success',
        [DotExperimentStatus.SCHEDULED]: 'info',
        [DotExperimentStatus.DRAFT]: 'warn',
        [DotExperimentStatus.ENDED]: 'info',
        [DotExperimentStatus.ARCHIVED]: 'secondary'
    };

    ngOnChanges(changes: SimpleChanges): void {
        const { experiment } = changes;
        if (experiment && experiment.currentValue) {
            const { status } = experiment.currentValue;
            this.statusIcon = ExperimentsStatusIcons[status];
        }
    }

    get statusTagValue(): string {
        const status = this.experiment?.status;
        if (!status) {
            return '';
        }

        const statusLabelKey = this.titleCasePipe.transform(status) ?? status;
        const statusLabel = this.dotMessageService.get(statusLabelKey);

        if (status !== DotExperimentStatus.RUNNING) {
            return statusLabel;
        }

        const endDate = this.experiment?.scheduling?.endDate;
        if (!endDate) {
            return statusLabel;
        }

        const until = this.dotMessageService.get('dot.common.until');
        const formattedEndDate = this.datePipe.transform(endDate, this.runningUntilDateFormat);

        return `${statusLabel} ${until} ${formattedEndDate ?? ''}`.trim();
    }
}
