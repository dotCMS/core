import { LowerCasePipe, NgForOf, NgIf, NgTemplateOutlet, UpperCasePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { RouterLink } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmPopupModule } from 'primeng/confirmpopup';
import { TableModule } from 'primeng/table';

import { UiDotIconButtonTooltipModule } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.module';
import { DotMessageService } from '@dotcms/data-access';
import {
    DotExperiment,
    DotExperimentStatusList,
    GroupedExperimentByStatus
} from '@dotcms/dotcms-models';
import { DotActionMenuItem } from '@models/dot-action-menu/dot-action-menu-item.model';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotRelativeDatePipe } from '@pipes/dot-relative-date/dot-relative-date.pipe';
import { DotExperimentsEmptyExperimentsComponent } from '@portlets/dot-experiments/dot-experiments-list/components/dot-experiments-empty-experiments/dot-experiments-empty-experiments.component';

@Component({
    standalone: true,
    selector: 'dot-experiments-list-table',
    imports: [
        RouterLink,
        NgIf,
        NgForOf,
        NgTemplateOutlet,
        LowerCasePipe,
        UpperCasePipe,
        // dotCMS
        DotMessagePipeModule,
        DotRelativeDatePipe,
        DotExperimentsEmptyExperimentsComponent,
        UiDotIconButtonTooltipModule,
        // PrimeNG
        ConfirmPopupModule,
        TableModule
    ],
    templateUrl: './dot-experiments-list-table.component.html',
    styleUrls: ['./dot-experiments-list-table.component.scss'],
    providers: [MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsListTableComponent {
    rowActions: { [index: string]: DotActionMenuItem };
    experimentStatus = DotExperimentStatusList;

    @Input() experimentsCount: number;

    @Input() experimentGroupedByStatus: GroupedExperimentByStatus[];

    @Output()
    archiveItem = new EventEmitter<DotExperiment>();
    @Output()
    deleteItem = new EventEmitter<DotExperiment>();

    @Output()
    goToReport = new EventEmitter<DotExperiment>();

    constructor(
        private readonly dotMessageService: DotMessageService,
        private readonly confirmationService: ConfirmationService
    ) {}

    /**
     * Show a confirmation dialog to Archive an experiment
     * @param {Event} event
     * @param {DotExperiment} item
     * @returns void
     * @memberof DotExperimentsListTableComponent
     */
    archive(event: Event, item: DotExperiment) {
        this.confirmationService.confirm({
            target: event.target,
            message: this.dotMessageService.get('experiments.action.archive.confirm-question'),
            icon: 'pi pi-exclamation-triangle',
            accept: () => this.archiveItem.emit(item)
        });
    }

    /**
     * Show a confirmation dialog to delete an experiment
     * @param {Event} event
     * @param {DotExperiment} item
     * @returns void
     * @memberof DotExperimentsListTableComponent
     */
    delete(event: Event, item: DotExperiment) {
        this.confirmationService.confirm({
            target: event.target,
            message: this.dotMessageService.get('experiments.action.delete.confirm-question'),
            icon: 'pi pi-exclamation-triangle',
            accept: () => this.deleteItem.emit(item)
        });
    }

    /**
     * Go to report of experiment
     * @param {DotExperiment} item
     * @returns void
     * @memberof DotExperimentsListTableComponent
     */
    viewReports(item: DotExperiment) {
        this.goToReport.emit(item);
    }
}
