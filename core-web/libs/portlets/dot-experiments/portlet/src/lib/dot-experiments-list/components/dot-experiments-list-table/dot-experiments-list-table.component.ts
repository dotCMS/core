import { LowerCasePipe, NgForOf, NgIf, UpperCasePipe } from '@angular/common';
import {
    ChangeDetectionStrategy,
    Component,
    EventEmitter,
    inject,
    Input,
    Output
} from '@angular/core';

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
    DotRelativeDatePipe,
    PrincipalConfiguration
} from '@dotcms/ui';

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
        DotRelativeDatePipe,
        // PrimeNG
        ConfirmPopupModule,
        TableModule,
        ButtonModule,
        TooltipModule,
        MenuModule,
        DotEmptyContainerComponent
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
    private dotMessageService: DotMessageService = inject(DotMessageService);
    protected readonly emptyConfiguration: PrincipalConfiguration = {
        title: this.dotMessageService.get('experimentspage.not.experiments.found.filtered'),
        icon: 'pi-filter-fill rotate-180'
    };
}
