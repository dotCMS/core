import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator/jest';
import { of } from 'rxjs';

import { ActivatedRoute } from '@angular/router';

import { ConfirmationService, MessageService } from 'primeng/api';
import { Card } from 'primeng/card';
import { Tooltip } from 'primeng/tooltip';

import {
    DotExperimentsService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import {
    ComponentStatus,
    ExperimentSteps,
    GOAL_TYPES,
    Goals,
    StepStatus
} from '@dotcms/dotcms-models';
import {
    ACTIVE_ROUTE_MOCK_CONFIG,
    getExperimentMock,
    GoalsMock,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { DotExperimentsConfigurationGoalsComponent } from './dot-experiments-configuration-goals.component';

import { DotExperimentsDetailsTableComponent } from '../../../shared/ui/dot-experiments-details-table/dot-experiments-details-table.component';
import { DotExperimentsConfigurationStore } from '../../store/dot-experiments-configuration-store';
import { DotExperimentsConfigurationGoalSelectComponent } from '../dot-experiments-configuration-goal-select/dot-experiments-configuration-goal-select.component';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.goals.name': 'Goal',
    'experiments.configure.goals.add': 'button add',
    'experiments.goal.reach_page.name': 'reach_page',
    'experiments.goal.reach_page.description': 'description',
    'experiments.configure.goals.no.seleted.goal.message': 'empty message',
    'experiments.goal.conditions.query.parameter': 'Query Parameter'
});
const EXPERIMENT_MOCK = getExperimentMock(0);
const EXPERIMENT_MOCK_WITH_GOAL = getExperimentMock(2);

function getVmMock(
    goals = GoalsMock,
    disabledTooltipLabel = null
): {
    experimentId: string;
    goals: Goals;
    status: StepStatus;
    isExperimentADraft: boolean;
    disabledTooltipLabel: null | string;
} {
    return {
        experimentId: EXPERIMENT_MOCK.id,
        goals: goals,
        status: {
            status: ComponentStatus.IDLE,
            isOpen: false,
            experimentStep: null
        },
        isExperimentADraft: true,
        disabledTooltipLabel
    };
}

describe('DotExperimentsConfigurationGoalsComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationGoalsComponent>;
    let store: DotExperimentsConfigurationStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;
    let confirmationService: ConfirmationService;

    const createComponent = createComponentFactory({
        component: DotExperimentsConfigurationGoalsComponent,
        componentProviders: [],
        imports: [DotExperimentsDetailsTableComponent],
        providers: [
            DotExperimentsConfigurationStore,
            ConfirmationService,
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            mockProvider(DotExperimentsService),
            mockProvider(ActivatedRoute, ACTIVE_ROUTE_MOCK_CONFIG),
            mockProvider(MessageService),
            mockProvider(DotHttpErrorManagerService)
        ]
    });
    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        store = spectator.inject(DotExperimentsConfigurationStore);

        dotExperimentsService = spectator.inject(DotExperimentsService);
        confirmationService = spectator.inject(ConfirmationService);
    });

    describe('no goal selected yet', () => {
        beforeEach(() => {
            dotExperimentsService.getById.mockReturnValue(of(EXPERIMENT_MOCK));

            store.loadExperiment(EXPERIMENT_MOCK.id);

            spectator.detectChanges();
        });

        test('should render the card', () => {
            expect(spectator.queryAll(Card).length).toEqual(1);
            expect(spectator.query(byTestId('goals-card-name'))).toContainText('Goal');
            expect(spectator.query(byTestId('goals-card-name'))).toHaveClass(
                'p-label-input-required'
            );
            expect(spectator.query(byTestId('goals-add-button'))).toExist();
        });

        test('should render empty message of select a goal', () => {
            expect(spectator.query(byTestId('goals-empty-msg'))).toContainText('empty message');
        });

        test('should enabled the button of add goal', () => {
            const addButton = spectator.query(byTestId('goals-add-button')) as HTMLButtonElement;

            expect(addButton.disabled).not.toBe(true);
        });

        test('should disable the button of add goal if a goal was selected already', () => {
            spectator.component.vm$ = of(getVmMock());
            spectator.detectComponentChanges();

            const addButton = spectator.query(byTestId('goals-add-button')) as HTMLButtonElement;
            expect(addButton.disabled).toBe(true);
            expect(spectator.query(DotExperimentsDetailsTableComponent)).toExist();
        });

        test('should disable the button of add goal if there is an error', () => {
            spectator.component.vm$ = of(getVmMock(null, 'error'));
            spectator.detectComponentChanges();

            const addButton = spectator.query(byTestId('goals-add-button')) as HTMLButtonElement;
            expect(addButton.disabled).toBe(true);
            expect(spectator.query(Tooltip).disabled).toEqual(false);
        });

        test('should call openSelectGoalSidebar if you click the add goal button', () => {
            jest.spyOn(spectator.component, 'openSelectGoalSidebar');

            const addButton = spectator.query(byTestId('goals-add-button')) as HTMLButtonElement;
            spectator.click(addButton);

            expect(spectator.component.openSelectGoalSidebar).toHaveBeenCalledTimes(1);
        });

        test('should show sidebar and close (remove it)', () => {
            store.setSidebarStatus({
                experimentStep: ExperimentSteps.GOAL,
                isOpen: true
            });

            expect(spectator.query(DotExperimentsConfigurationGoalSelectComponent)).toExist();

            store.setSidebarStatus({
                experimentStep: ExperimentSteps.GOAL,
                isOpen: false
            });

            expect(spectator.query(DotExperimentsConfigurationGoalSelectComponent)).not.toExist();
        });
    });

    describe('Goal selected', () => {
        beforeEach(() => {
            dotExperimentsService.getById.mockReturnValue(of(EXPERIMENT_MOCK_WITH_GOAL));
            dotExperimentsService.deleteGoal.mockReturnValue(of(EXPERIMENT_MOCK));

            store.loadExperiment(EXPERIMENT_MOCK_WITH_GOAL.id);

            spectator.detectChanges();
        });
        test('should show a confirmation to delete a goal', () => {
            jest.spyOn(store, 'deleteGoal');
            jest.spyOn(confirmationService, 'confirm');

            spectator.detectComponentChanges();
            const deleteIcon = spectator.query(byTestId('goal-delete-button'));

            expect(deleteIcon).toExist();

            spectator.dispatchMouseEvent(deleteIcon, 'click');
            spectator.detectComponentChanges();

            expect(confirmationService.confirm).toHaveBeenCalled();
        });

        test('should render the params header correctly', () => {
            spectator.component.vm$ = of(
                getVmMock({
                    ...GoalsMock,
                    primary: { ...GoalsMock.primary, type: GOAL_TYPES.URL_PARAMETER }
                })
            );
            spectator.detectComponentChanges();
            const paramsHeader = spectator.query(byTestId('goal-header-parameter'));

            expect(paramsHeader).toContainText('Query Parameter');
        });
    });
});
