import { DatePipe, JsonPipe, LowerCasePipe, NgIf, TitleCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { RouterLink } from '@angular/router';

import { SkeletonModule } from 'primeng/skeleton';
import { TagModule } from 'primeng/tag';

import { DotExperiment, DotExperimentStatus } from '@dotcms/dotcms-models';
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
        // DotCMS
        DotIconModule,
        DotPipesModule,
        // PrimeNG
        SkeletonModule,
        TagModule,
        DotMessagePipe,
        DatePipe,
        JsonPipe,
        TitleCasePipe
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
}
