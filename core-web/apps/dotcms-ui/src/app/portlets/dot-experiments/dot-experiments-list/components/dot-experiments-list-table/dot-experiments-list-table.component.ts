import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import {
    DotExperiment,
    DotExperimentStatusList
} from '@portlets/dot-experiments/shared/models/dot-experiments.model';
import { DotActionMenuItem } from '@models/dot-action-menu/dot-action-menu-item.model';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { ConfirmationService, MessageService } from 'primeng/api';

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
    archiveItem = new EventEmitter<string>();
    @Output()
    deleteItem = new EventEmitter<string>();

    constructor(
        private readonly dotMessageService: DotMessageService,
        private readonly confirmationService: ConfirmationService,
        private readonly messageService: MessageService
    ) {}

    @Input()
    set experiments(items: { [key: string]: DotExperiment[] }) {
        this.experimentGroupedByStatus = items;
        this.groupedExperimentsCount = Object.keys(items).length;
    }

    archive(event: Event, item: DotExperiment) {
        this.confirmationService.confirm({
            target: event.target,
            message: this.dotMessageService.get('experiments.action.archive.confirm-question'),
            icon: 'pi pi-exclamation-triangle',
            accept: () => {
                this.messageService.add({
                    severity: 'info',
                    summary: this.dotMessageService.get(
                        'experiments.action.archived.confirm-title'
                    ),
                    detail: this.dotMessageService.get(
                        'experiments.action.archived.confirm-message',
                        item.name
                    )
                });
                this.archiveItem.emit(item.id);
            }
        });
    }

    delete(event: Event, item: DotExperiment) {
        this.confirmationService.confirm({
            target: event.target,
            message: this.dotMessageService.get('experiments.action.delete.confirm-question'),
            icon: 'pi pi-exclamation-triangle',
            accept: () => {
                this.messageService.add({
                    severity: 'info',
                    summary: this.dotMessageService.get('experiments.action.delete.confirm-title'),
                    detail: this.dotMessageService.get(
                        'experiments.action.delete.confirm-message',
                        item.name
                    )
                });
                this.deleteItem.emit(item.id);
            }
        });
    }
}
