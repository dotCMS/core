import { LowerCasePipe, UpperCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { MenuModule } from 'primeng/menu';
import { TableModule } from 'primeng/table';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotExperiment, GroupedExperimentByStatus } from '@dotcms/dotcms-models';
import {
    DotEmptyContainerComponent,
    DotMessagePipe,
    DotTimestampToDatePipe,
    PrincipalConfiguration
} from '@dotcms/ui';

@Component({
    selector: 'dot-experiments-list-table',
    imports: [
        LowerCasePipe,
        UpperCasePipe,
        DotMessagePipe,
        ConfirmPopupModule,
        TableModule,
        ButtonModule,
        TooltipModule,
        MenuModule,
        DotEmptyContainerComponent,
        DotTimestampToDatePipe
    ],
    templateUrl: './dot-experiments-list-table.component.html',
    providers: [MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsListTableComponent {
    $experimentGroupedByStatus = input<GroupedExperimentByStatus[]>([], {
        alias: 'experimentGroupedByStatus'
    });

    $goToContainer = output<DotExperiment>({ alias: 'goToContainer' });

    private dotMessageService: DotMessageService = inject(DotMessageService);
    protected readonly emptyConfiguration: PrincipalConfiguration = {
        title: this.dotMessageService.get('experimentspage.not.experiments.found.filtered'),
        icon: 'pi-filter-fill rotate-180'
    };
}
