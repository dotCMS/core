import { LowerCasePipe, NgForOf, NgIf, UpperCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { MenuModule } from 'primeng/menu';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';

import { DotExperiment, GroupedExperimentByStatus } from '@dotcms/dotcms-models';
import { DotMessagePipe } from '@dotcms/ui';
import { DotRelativeDatePipe } from '@pipes/dot-relative-date/dot-relative-date.pipe';

import { DotExperimentsEmptyExperimentsComponent } from '../dot-experiments-empty-experiments/dot-experiments-empty-experiments.component';

@Component({
    standalone: true,
    selector: 'dot-experiments-list-table',
    imports: [
        NgIf,
        LowerCasePipe,
        UpperCasePipe,
        NgForOf,
        // dotCMS
        DotMessagePipe,
        DotExperimentsEmptyExperimentsComponent,
        DotRelativeDatePipe,
        // PrimeNG
        ConfirmPopupModule,
        TableModule,
        ButtonModule,
        TooltipModule,
        MenuModule
    ],
    templateUrl: './dot-experiments-list-table.component.html',
    styleUrls: ['./dot-experiments-list-table.component.scss'],
    providers: [MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsListTableComponent {
    @Input() experimentGroupedByStatus: GroupedExperimentByStatus[] = [];

    @Output()
    goToContainer = new EventEmitter<DotExperiment>();
}
