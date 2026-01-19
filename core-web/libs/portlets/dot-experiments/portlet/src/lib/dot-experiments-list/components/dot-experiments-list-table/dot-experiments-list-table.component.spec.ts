import { byTestId, createComponentFactory, Spectator } from '@ngneat/spectator/jest';

import { NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';

import { ConfirmationService, MenuItem, MessageService } from 'primeng/api';
import { ConfirmPopup } from 'primeng/confirmpopup';
import { Menu, MenuItemContent } from 'primeng/menu';
import { Table } from 'primeng/table';

import { DotMessageService, DotFormatDateService } from '@dotcms/data-access';
import { LoginService } from '@dotcms/dotcms-js';
import { DotExperimentStatus, GroupedExperimentByStatus } from '@dotcms/dotcms-models';
import { DotEmptyContainerComponent, DotTimestampToDatePipe } from '@dotcms/ui';
import {
    DotFormatDateServiceMock,
    getExperimentMock,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { DotExperimentsListTableComponent } from './dot-experiments-list-table.component';

const MOCK_MENU_ITEMS: MenuItem[] = [
    // Delete Action
    {
        id: 'dot-experiments-delete',
        label: 'experiments.action.delete',
        visible: true,
        automationId: 'experiment-row-action-menu-delete'
    },
    // Delete Action
    {
        id: 'dot-experiments-go-to-configuration',
        label: 'experiments.action.configuration',
        visible: true,
        automationId: 'experiment-row-action-menu-got-to-configuration'
    },
    // Archive Action
    {
        id: 'dot-experiments-archive',
        label: 'experiments.action.archive',
        visible: true,
        automationId: 'experiment-row-action-menu-archive'
    }
];

const DRAFT_EXPERIMENT_MOCK = {
    ...getExperimentMock(0),
    actionsItemsMenu: [...MOCK_MENU_ITEMS]
};
const ARCHIVE_EXPERIMENT_MOCK = {
    ...getExperimentMock(1),
    status: DotExperimentStatus.ARCHIVED,
    actionsItemsMenu: [...MOCK_MENU_ITEMS]
};
const EXPERIMENT_MOCK = {
    ...getExperimentMock(1),
    status: DotExperimentStatus.RUNNING,
    actionsItemsMenu: [...MOCK_MENU_ITEMS]
};

@Pipe({ name: 'date', standalone: false })
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

    const createComponent = createComponentFactory({
        component: DotExperimentsListTableComponent,
        imports: [DotTimestampToDatePipe],
        componentMocks: [ConfirmPopup],
        declarations: [MockDatePipe],
        providers: [
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            MessageService,
            ConfirmationService,
            { provide: DotFormatDateService, useClass: DotFormatDateServiceMock },
            {
                provide: LoginService,
                useValue: { currentUserLanguageId: 'en-US' }
            }
        ],
        schemas: [NO_ERRORS_SCHEMA]
    });

    beforeEach(() => {
        spectator = createComponent();
    });

    describe('Input experiments', () => {
        it('should show empty component with no experiments found', () => {
            spectator.setInput('experimentGroupedByStatus', []);

            expect(spectator.query(DotEmptyContainerComponent)).toExist();
        });

        it('should show 2 instances of NgPrime Table component', () => {
            const INSTANCES_OF_NGPRIME_TABLE = 2;

            const groupedExperimentByStatus: GroupedExperimentByStatus[] = [
                { status: DotExperimentStatus.DRAFT, experiments: [DRAFT_EXPERIMENT_MOCK] },
                { status: DotExperimentStatus.ARCHIVED, experiments: [ARCHIVE_EXPERIMENT_MOCK] }
            ];

            spectator.setInput('experimentGroupedByStatus', groupedExperimentByStatus);

            const pTables = spectator.queryAll<Table>(Table);

            expect(pTables).toExist();
            expect(pTables.length).toBe(INSTANCES_OF_NGPRIME_TABLE);
        });

        it('should has experiment with columns correctly rendered', () => {
            const COLUMNS_QTY_BY_ROW = 4;

            const groupedExperimentByStatus: GroupedExperimentByStatus[] = [
                { status: DotExperimentStatus.DRAFT, experiments: [DRAFT_EXPERIMENT_MOCK] }
            ];

            spectator.setInput('experimentGroupedByStatus', groupedExperimentByStatus);

            const experimentRow = spectator.query(byTestId('experiment-row'));
            expect(experimentRow.querySelectorAll('td').length).toBe(COLUMNS_QTY_BY_ROW);

            expect(spectator.query(byTestId('experiment-row__name'))).toHaveText(
                DRAFT_EXPERIMENT_MOCK.name
            );
            expect(spectator.query(byTestId('experiment-row__createdDate'))).toHaveText(
                '10/10/2020 10:10 PM'
            );
            expect(spectator.query(byTestId('experiment-row__modDate'))).toHaveText(
                '10/10/2020 10:10 PM'
            );
        });

        it('should emit action when a row is clicked', () => {
            jest.spyOn(spectator.component.goToContainer, 'emit');
            const groupedExperimentByStatus: GroupedExperimentByStatus[] = [
                { status: DotExperimentStatus.DRAFT, experiments: [DRAFT_EXPERIMENT_MOCK] }
            ];

            spectator.setInput('experimentGroupedByStatus', groupedExperimentByStatus);

            spectator.click(byTestId('experiment-row'));
            expect(spectator.component.goToContainer.emit).toHaveBeenCalledWith(
                DRAFT_EXPERIMENT_MOCK
            );
        });

        it('should emit action when a row is activated with keyboard (Enter)', () => {
            jest.spyOn(spectator.component.goToContainer, 'emit');
            const groupedExperimentByStatus: GroupedExperimentByStatus[] = [
                { status: DotExperimentStatus.DRAFT, experiments: [DRAFT_EXPERIMENT_MOCK] }
            ];

            spectator.setInput('experimentGroupedByStatus', groupedExperimentByStatus);

            const experimentRow = spectator.query(byTestId('experiment-row')) as HTMLElement;
            experimentRow.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter' }));

            expect(spectator.component.goToContainer.emit).toHaveBeenCalledWith(
                DRAFT_EXPERIMENT_MOCK
            );
        });

        describe('Actions icons', () => {
            it('should has the MenuItems rendered as action of the row', () => {
                const groupedExperimentByStatus: GroupedExperimentByStatus[] = [
                    { status: DotExperimentStatus.RUNNING, experiments: [EXPERIMENT_MOCK] }
                ];

                const MENU_ITEMS_QTY = EXPERIMENT_MOCK.actionsItemsMenu.length;

                spectator.setInput('experimentGroupedByStatus', groupedExperimentByStatus);

                expect(spectator.query(Menu)).toExist();

                const actionMenuButton = spectator.query(byTestId('experiment-row__action-button'));
                expect(actionMenuButton).toExist();

                spectator.click(actionMenuButton);
                expect(spectator.queryAll(MenuItemContent)).toExist();
                expect(spectator.queryAll(MenuItemContent).length).toBe(MENU_ITEMS_QTY);
            });
        });
    });
});
