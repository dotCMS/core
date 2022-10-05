import { byTestId, createComponentFactory, mockProvider, Spectator } from '@ngneat/spectator';
import { Pipe, PipeTransform } from '@angular/core';
import { DotExperimentsListTableComponent } from './dot-experiments-list-table.component';
import { DotMessageService } from '@services/dot-message/dot-messages.service';
import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmPopup, ConfirmPopupModule } from 'primeng/confirmpopup';
import { DotExperimentsEmptyExperimentsComponent } from '../dot-experiments-empty-experiments/dot-experiments-empty-experiments.component';
import {
    DotExperiment,
    DotExperimentStatusList,
    GroupedExperimentByStatus,
    TrafficProportionTypes
} from '@portlets/dot-experiments/shared/models/dot-experiments.model';
import { Table, TableModule } from 'primeng/table';
import { DotIconModule } from '@dotcms/ui';
import { UiDotIconButtonTooltipModule } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.module';
import { UiDotIconButtonTooltipComponent } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.component';
import { Toast, ToastModule } from 'primeng/toast';

const draftExperiments: DotExperiment[] = [
    {
        id: '111',
        pageId: '456',
        status: DotExperimentStatusList.DRAFT,
        archived: false,
        readyToStart: false,
        description: 'Praesent at molestie mauris, quis vulputate augue.',
        name: 'Praesent at molestie mauris',
        trafficAllocation: 100.0,
        scheduling: null,
        trafficProportion: {
            percentages: {},
            type: TrafficProportionTypes.SPLIT_EVENLY
        },
        creationDate: new Date('2022-08-21 14:50:03'),
        modDate: new Date('2022-08-21 18:50:03')
    }
];
const endedExperiments: DotExperiment[] = [
    {
        id: '111',
        pageId: '456',
        status: DotExperimentStatusList.ENDED,
        archived: false,
        readyToStart: false,
        description: 'Praesent at molestie mauris, quis vulputate augue.',
        name: 'Praesent at molestie mauris',
        trafficAllocation: 100.0,
        scheduling: null,
        trafficProportion: {
            percentages: {},
            type: TrafficProportionTypes.SPLIT_EVENLY
        },
        creationDate: new Date('2022-08-21 14:50:03'),
        modDate: new Date('2022-08-21 18:50:03')
    }
];
const archivedExperiments: DotExperiment[] = [
    {
        id: '111',
        pageId: '456',
        status: DotExperimentStatusList.ARCHIVED,
        archived: false,
        readyToStart: false,
        description: 'Praesent at molestie mauris, quis vulputate augue.',
        name: 'Praesent at molestie mauris',
        trafficAllocation: 100.0,
        scheduling: null,
        trafficProportion: {
            percentages: {},
            type: TrafficProportionTypes.SPLIT_EVENLY
        },
        creationDate: new Date('2022-08-21 14:50:03'),
        modDate: new Date('2022-08-21 18:50:03')
    }
];
const scheduledExperiments: DotExperiment[] = [
    {
        id: '111',
        pageId: '456',
        status: DotExperimentStatusList.SCHEDULED,
        archived: false,
        readyToStart: false,
        description: 'Praesent at molestie mauris, quis vulputate augue.',
        name: 'Praesent at molestie mauris',
        trafficAllocation: 100.0,
        scheduling: null,
        trafficProportion: {
            percentages: {},
            type: TrafficProportionTypes.SPLIT_EVENLY
        },
        creationDate: new Date('2022-08-21 14:50:03'),
        modDate: new Date('2022-08-21 18:50:03')
    }
];

@Pipe({ name: 'dm' })
class MockDmPipe implements PipeTransform {
    transform(value: string): string {
        return value;
    }
}

@Pipe({ name: 'date' })
class MockDatePipe implements PipeTransform {
    transform(value: string): string {
        return (value as unknown as Date).toLocaleDateString();
    }
}

describe('DotExperimentsListTableComponent', () => {
    let spectator: Spectator<DotExperimentsListTableComponent>;
    let uiDotIconButtonTooltipComponent: UiDotIconButtonTooltipComponent | null;
    let dotExperimentsEmpty: DotExperimentsEmptyExperimentsComponent | null;
    let confirmPopupComponent: ConfirmPopup | null;
    let toastComponent: Toast | null;

    const createComponent = createComponentFactory({
        imports: [
            TableModule,
            DotIconModule,
            UiDotIconButtonTooltipModule,
            ConfirmPopupModule,
            ToastModule
        ],
        component: DotExperimentsListTableComponent,
        componentMocks: [ConfirmPopup],
        declarations: [MockDmPipe, MockDatePipe, DotExperimentsEmptyExperimentsComponent],
        providers: [mockProvider(DotMessageService), MessageService, ConfirmationService]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    describe('Input experiments', () => {
        it('should show empty component with no experiments found', () => {
            spectator.setInput('experiments', {});
            dotExperimentsEmpty = spectator.query(DotExperimentsEmptyExperimentsComponent);

            expect(dotExperimentsEmpty).toExist();
        });

        it('should show 2 instances of NgPrime Table component', () => {
            const INSTANCES_OF_NGPRIME_TABLE = 2;
            const groupedExperimentByStatus: GroupedExperimentByStatus = {
                [DotExperimentStatusList.DRAFT]: [...draftExperiments],
                [DotExperimentStatusList.ARCHIVED]: [...archivedExperiments]
            };

            spectator.setInput('experiments', groupedExperimentByStatus);

            const pTables = spectator.queryAll<Table>(Table);

            expect(pTables).toExist();
            expect(pTables.length).toBe(INSTANCES_OF_NGPRIME_TABLE);
            expect(spectator.component.groupedExperimentsCount).toBe(INSTANCES_OF_NGPRIME_TABLE);
        });

        it('should has experiment with columns correctly rendered', () => {
            const COLUMNS_QTY_BY_ROW = 4;

            const groupedExperimentByStatus: GroupedExperimentByStatus = {
                [DotExperimentStatusList.DRAFT]: [...draftExperiments]
            };

            spectator.setInput('experiments', groupedExperimentByStatus);

            const experimentRow = spectator.query(byTestId('experiment-row'));
            expect(experimentRow.querySelectorAll('td').length).toBe(COLUMNS_QTY_BY_ROW);

            expect(spectator.query(byTestId('experiment-row-name'))).toHaveText(
                groupedExperimentByStatus.DRAFT[0].name
            );
            expect(spectator.query(byTestId('experiment-row-createdDate'))).toHaveText(
                groupedExperimentByStatus.DRAFT[0].creationDate.toLocaleDateString()
            );
            expect(spectator.query(byTestId('experiment-row-modDate'))).toHaveText(
                groupedExperimentByStatus.DRAFT[0].modDate.toLocaleDateString()
            );
        });

        describe('Actions icons', () => {
            it('should has DELETE icon when experiment is DRAFT', () => {
                const groupedExperimentByStatus: GroupedExperimentByStatus = {
                    [DotExperimentStatusList.DRAFT]: [...draftExperiments]
                };

                spectator.setInput('experiments', groupedExperimentByStatus);

                uiDotIconButtonTooltipComponent = spectator.query(UiDotIconButtonTooltipComponent);
                expect(uiDotIconButtonTooltipComponent.icon).toBe('delete');
            });

            it('should the row has DELETE icon when experiment is SCHEDULED', () => {
                const groupedExperimentByStatus: GroupedExperimentByStatus = {
                    [DotExperimentStatusList.SCHEDULED]: [...scheduledExperiments]
                };

                spectator.setInput('experiments', groupedExperimentByStatus);

                uiDotIconButtonTooltipComponent = spectator.query(UiDotIconButtonTooltipComponent);
                expect(uiDotIconButtonTooltipComponent.icon).toBe('delete');
            });
            it('should the row  has ARCHIVE icon when is ENDED', () => {
                const groupedExperimentByStatus: GroupedExperimentByStatus = {
                    [DotExperimentStatusList.ENDED]: [...endedExperiments]
                };

                spectator.setInput('experiments', groupedExperimentByStatus);

                uiDotIconButtonTooltipComponent = spectator.query(UiDotIconButtonTooltipComponent);
                expect(uiDotIconButtonTooltipComponent.icon).toBe('archive');
            });

            it('should the row not has any icon in action column', () => {
                const groupedExperimentByStatus: GroupedExperimentByStatus = {
                    [DotExperimentStatusList.ARCHIVED]: [...archivedExperiments]
                };

                spectator.setInput('experiments', groupedExperimentByStatus);

                uiDotIconButtonTooltipComponent = spectator.query(UiDotIconButtonTooltipComponent);
                expect(uiDotIconButtonTooltipComponent).not.toExist();
            });
        });
    });

    describe('Output deleteItem', () => {
        it('should emit the $event on click', () => {
            const itemToDelete = draftExperiments[0];
            const event = new MouseEvent('click');
            let output;

            spectator.output('deleteItem').subscribe((result) => (output = result));
            spectator.component.delete(event, itemToDelete);

            confirmPopupComponent = spectator.query(ConfirmPopup);
            confirmPopupComponent.accept();

            expect(output).toEqual(itemToDelete.id);
        });
    });

    describe('Output archiveItem', () => {
        it('should emit the $event on click', () => {
            const itemToArchive = endedExperiments[0];
            const event = new MouseEvent('click');
            let output;

            spectator.output('archiveItem').subscribe((result) => (output = result));
            spectator.component.archive(event, itemToArchive);

            confirmPopupComponent = spectator.query(ConfirmPopup);
            confirmPopupComponent.accept();

            toastComponent = spectator.query(Toast);

            expect(output).toEqual(itemToArchive.id);
            expect(toastComponent).toExist();
        });
    });
});
