import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator';

import { Pipe, PipeTransform } from '@angular/core';
import { RouterTestingModule } from '@angular/router/testing';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ConfirmPopup, ConfirmPopupModule } from 'primeng/confirmpopup';
import { Table, TableModule } from 'primeng/table';
import { ToastModule } from 'primeng/toast';

import { UiDotIconButtonTooltipComponent } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.component';
import { UiDotIconButtonTooltipModule } from '@components/_common/dot-icon-button-tooltip/dot-icon-button-tooltip.module';
import { DotMessageService } from '@dotcms/data-access';
import { DotExperimentStatusList, GroupedExperimentByStatus } from '@dotcms/dotcms-models';
import { DotIconModule, DotMessagePipeModule } from '@dotcms/ui';
import { DotFormatDateServiceMock, MockDotMessageService } from '@dotcms/utils-testing';
import { DotRelativeDatePipe } from '@pipes/dot-relative-date/dot-relative-date.pipe';
import { getExperimentMock } from '@portlets/dot-experiments/test/mocks';
import { DotFormatDateService } from '@services/dot-format-date-service';

import { DotExperimentsListTableComponent } from './dot-experiments-list-table.component';

import { DotExperimentsEmptyExperimentsComponent } from '../dot-experiments-empty-experiments/dot-experiments-empty-experiments.component';

const DRAFT_EXPERIMENT_MOCK = getExperimentMock(0);
const ARCHIVE_EXPERIMENT_MOCK = {
    ...getExperimentMock(1),
    status: DotExperimentStatusList.ARCHIVED
};
const RUNNING_EXPERIMENT_MOCK = {
    ...getExperimentMock(1),
    status: DotExperimentStatusList.RUNNING
};
const ENDED_EXPERIMENT_MOCK = {
    ...getExperimentMock(2),
    status: DotExperimentStatusList.ENDED
};

const SCHEDULED_EXPERIMENT_MOCK = {
    ...getExperimentMock(1),
    status: DotExperimentStatusList.SCHEDULED
};

@Pipe({ name: 'date' })
class MockDatePipe implements PipeTransform {
    transform(value: string): string {
        return (value as unknown as Date).toLocaleDateString();
    }
}

const messageServiceMock = new MockDotMessageService({
    'experiments.list.name': 'Name',
    'experiments.list.created': 'Created',
    'experiments.list.modified': 'Modified',
    'experiments.action.delete': 'Delete',
    'experimentspage.not.experiments.found.filtered': 'Not experiments founds'
});

describe('DotExperimentsListTableComponent', () => {
    let spectator: Spectator<DotExperimentsListTableComponent>;
    let uiDotIconButtonTooltipComponent: UiDotIconButtonTooltipComponent | null;
    let dotExperimentsEmpty: DotExperimentsEmptyExperimentsComponent | null;
    let confirmPopupComponent: ConfirmPopup | null;

    const createComponent = createComponentFactory({
        imports: [
            TableModule,
            DotIconModule,
            UiDotIconButtonTooltipModule,
            ConfirmPopupModule,
            ToastModule,
            DotMessagePipeModule,
            RouterTestingModule,
            DotRelativeDatePipe
        ],
        component: DotExperimentsListTableComponent,
        componentMocks: [ConfirmPopup],
        declarations: [MockDatePipe],
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            MessageService,
            ConfirmationService,
            { provide: DotFormatDateService, useClass: DotFormatDateServiceMock }
        ]
    });

    beforeEach(() => {
        spectator = createComponent();
        spectator.setInput('experimentsCount', 2);
    });

    describe('Input experiments', () => {
        it('should show empty component with no experiments found', () => {
            spectator.setInput('experimentGroupedByStatus', []);
            spectator.setInput('experimentsCount', 0);
            dotExperimentsEmpty = spectator.query(DotExperimentsEmptyExperimentsComponent);

            expect(dotExperimentsEmpty).toExist();
        });

        it('should show 2 instances of NgPrime Table component', () => {
            const INSTANCES_OF_NGPRIME_TABLE = 2;

            const groupedExperimentByStatus: GroupedExperimentByStatus[] = [
                { status: DotExperimentStatusList.DRAFT, experiments: [DRAFT_EXPERIMENT_MOCK] },
                { status: DotExperimentStatusList.ARCHIVED, experiments: [ARCHIVE_EXPERIMENT_MOCK] }
            ];

            spectator.setInput('experimentGroupedByStatus', groupedExperimentByStatus);

            const pTables = spectator.queryAll<Table>(Table);

            expect(pTables).toExist();
            expect(pTables.length).toBe(INSTANCES_OF_NGPRIME_TABLE);
            expect(spectator.component.experimentsCount).toBe(INSTANCES_OF_NGPRIME_TABLE);
        });

        it('should has experiment with columns correctly rendered', () => {
            const COLUMNS_QTY_BY_ROW = 4;

            const groupedExperimentByStatus: GroupedExperimentByStatus[] = [
                { status: DotExperimentStatusList.DRAFT, experiments: [DRAFT_EXPERIMENT_MOCK] }
            ];

            spectator.setInput('experimentGroupedByStatus', groupedExperimentByStatus);

            const experimentRow = spectator.query(byTestId('experiment-row'));
            expect(experimentRow.querySelectorAll('td').length).toBe(COLUMNS_QTY_BY_ROW);

            expect(spectator.query(byTestId('experiment-row__name'))).toHaveText(
                DRAFT_EXPERIMENT_MOCK.name
            );
            expect(spectator.query(byTestId('experiment-row__createdDate'))).toHaveText(
                '1 hour ago'
            );
            expect(spectator.query(byTestId('experiment-row__modDate'))).toHaveText('1 hour ago');
        });

        describe('Actions icons', () => {
            it('should has DELETE icon when experiment is DRAFT', () => {
                const groupedExperimentByStatus: GroupedExperimentByStatus[] = [
                    { status: DotExperimentStatusList.DRAFT, experiments: [DRAFT_EXPERIMENT_MOCK] }
                ];

                spectator.setInput('experimentGroupedByStatus', groupedExperimentByStatus);

                uiDotIconButtonTooltipComponent = spectator.query(UiDotIconButtonTooltipComponent);
                expect(uiDotIconButtonTooltipComponent.icon).toBe('delete');
            });

            it('should the row has DELETE icon when experiment is SCHEDULED', () => {
                const groupedExperimentByStatus: GroupedExperimentByStatus[] = [
                    {
                        status: DotExperimentStatusList.SCHEDULED,
                        experiments: [SCHEDULED_EXPERIMENT_MOCK]
                    }
                ];

                spectator.setInput('experimentGroupedByStatus', groupedExperimentByStatus);

                uiDotIconButtonTooltipComponent = spectator.query(UiDotIconButtonTooltipComponent);
                expect(uiDotIconButtonTooltipComponent.icon).toBe('delete');
            });
            it('should the row  has ARCHIVE icon when is ENDED', () => {
                const groupedExperimentByStatus: GroupedExperimentByStatus[] = [
                    {
                        status: DotExperimentStatusList.ENDED,
                        experiments: [ENDED_EXPERIMENT_MOCK]
                    }
                ];

                spectator.setInput('experimentGroupedByStatus', groupedExperimentByStatus);

                expect(spectator.queryAll(UiDotIconButtonTooltipComponent)[0].icon).toBe(
                    'settings'
                );
                expect(spectator.queryAll(UiDotIconButtonTooltipComponent)[1].icon).toBe('archive');
            });

            it('should the row not has any icon in action column', () => {
                const groupedExperimentByStatus: GroupedExperimentByStatus[] = [
                    {
                        status: DotExperimentStatusList.ARCHIVED,
                        experiments: [ARCHIVE_EXPERIMENT_MOCK]
                    }
                ];

                spectator.setInput('experimentGroupedByStatus', groupedExperimentByStatus);

                uiDotIconButtonTooltipComponent = spectator.query(UiDotIconButtonTooltipComponent);
                expect(uiDotIconButtonTooltipComponent).not.toExist();
            });

            it('should the row  has Configuration icon when is RUNNING', () => {
                const groupedExperimentByStatus: GroupedExperimentByStatus[] = [
                    {
                        status: DotExperimentStatusList.RUNNING,
                        experiments: [RUNNING_EXPERIMENT_MOCK]
                    }
                ];

                spectator.setInput('experimentGroupedByStatus', groupedExperimentByStatus);

                uiDotIconButtonTooltipComponent = spectator.query(UiDotIconButtonTooltipComponent);
                expect(uiDotIconButtonTooltipComponent.icon).toBe('settings');
            });

            it('should the row  has Configuration icon when is RUNNING', () => {
                const groupedExperimentByStatus: GroupedExperimentByStatus[] = [
                    {
                        status: DotExperimentStatusList.ENDED,
                        experiments: [RUNNING_EXPERIMENT_MOCK]
                    }
                ];

                spectator.setInput('experimentGroupedByStatus', groupedExperimentByStatus);

                uiDotIconButtonTooltipComponent = spectator.query(UiDotIconButtonTooltipComponent);
                expect(uiDotIconButtonTooltipComponent.icon).toBe('settings');
            });
        });
    });

    describe('Output deleteItem', () => {
        it('should emit the $event on click', () => {
            const itemToDelete = DRAFT_EXPERIMENT_MOCK;
            const event = new MouseEvent('click');
            let output;

            spectator.output('deleteItem').subscribe((result) => (output = result));
            spectator.component.delete(event, itemToDelete);

            confirmPopupComponent = spectator.query(ConfirmPopup);
            confirmPopupComponent.accept();

            expect(output).toEqual(itemToDelete);
        });
    });

    describe('Output archiveItem', () => {
        it('should emit the $event on click', () => {
            const itemToArchive = ENDED_EXPERIMENT_MOCK;
            const event = new MouseEvent('click');
            let output;

            spectator.output('archiveItem').subscribe((result) => (output = result));
            spectator.component.archive(event, itemToArchive);

            confirmPopupComponent = spectator.query(ConfirmPopup);
            confirmPopupComponent.accept();

            expect(output).toEqual(itemToArchive);
        });
    });

    describe('Output viewReports', () => {
        it('should emit the $event on click', () => {
            const itemToView = RUNNING_EXPERIMENT_MOCK;
            let output;

            spectator.output('goToReport').subscribe((result) => (output = result));
            spectator.component.viewReports(itemToView);

            expect(output).toEqual(itemToView);
        });
    });
});
