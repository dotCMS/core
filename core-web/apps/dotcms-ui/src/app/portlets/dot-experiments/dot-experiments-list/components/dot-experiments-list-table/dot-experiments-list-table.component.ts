import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { DotExperiment } from '@portlets/dot-experiments/shared/models/dot-experiments.model';
import { DotActionMenuItem } from '@models/dot-action-menu/dot-action-menu-item.model';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { ConfirmationService, MessageService } from 'primeng/api';
import { DotExperimentStatusList } from '@portlets/dot-experiments/shared/models/dot-experiments-constants';

@Component({
    selector: 'dot-experiments-list-table',
    templateUrl: './dot-experiments-list-table.component.html',
    styleUrls: ['./dot-experiments-list-table.component.scss'],
    providers: [MessageService],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotExperimentsListTableComponent {
    experimentGroupedByStatus: { [key: string]: DotExperiment[] };
    groupedExperimentsCount: number;
    rowActions: { [index: string]: DotActionMenuItem };
    experimentStatus = DotExperimentStatusList;

    @Output()
    archiveItem = new EventEmitter<DotExperiment>();
    @Output()
    deleteItem = new EventEmitter<DotExperiment>();

    constructor(
        private readonly dotMessageService: DotMessageService,
        private readonly confirmationService: ConfirmationService
    ) {}

    @Input()
    set experiments(items: { [key: string]: DotExperiment[] }) {
        this.experimentGroupedByStatus = items;
        this.groupedExperimentsCount = Object.keys(items).length;
    }

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
}
