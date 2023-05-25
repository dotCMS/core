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
import { ButtonModule } from 'primeng/button';
import { Card, CardModule } from 'primeng/card';
import { ConfirmPopup, ConfirmPopupModule } from 'primeng/confirmpopup';
import { Tooltip, TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';

import { DotDynamicDirective } from '@portlets/shared/directives/dot-dynamic.directive';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';
import { ACTIVE_ROUTE_MOCK_CONFIG, getExperimentMock, GoalsMock, MockDotMessageService } from "@dotcms/utils-testing";
import { DotExperimentsConfigurationGoalsComponent } from "./dot-experiments-configuration-goals.component";
import { DotExperimentsConfigurationStore } from "../../store/dot-experiments-configuration-store";
import { DotExperimentsService } from "@dotcms/portlets/dot-experiments/data-access";
import {
    DotExperimentsConfigurationGoalSelectComponent
} from "../dot-experiments-configuration-goal-select/dot-experiments-configuration-goal-select.component";
import {
    DotExperimentsDetailsTableComponent
} from "../../../shared/ui/dot-experiments-details-table/dot-experiments-details-table.component";
import { DotMessagePipe } from "@dotcms/ui";
import { ComponentStatus, ExperimentSteps, Goals } from "@dotcms/dotcms-models";


const messageServiceMock = new MockDotMessageService({
    'experiments.configure.goals.name': 'Goal',
    'experiments.configure.goals.add': 'button add',
    'experiments.goal.reach_page.name': 'reach_page',
    'experiments.goal.reach_page.description': 'description',
    'experiments.configure.goals.no.seleted.goal.message': 'empty message'
});
const EXPERIMENT_MOCK = getExperimentMock(0);
const EXPERIMENT_MOCK_WITH_GOAL = getExperimentMock(2);
xdescribe('DotExperimentsConfigurationGoalsComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationGoalsComponent>;
    let store: DotExperimentsConfigurationStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;
    let confirmPopupComponent: ConfirmPopup;

    const createComponent = createComponentFactory({
        imports: [
            ButtonModule,
            CardModule,
            DotExperimentsConfigurationGoalSelectComponent,
            DotDynamicDirective,
            ConfirmPopupModule,
            TooltipModule,
            DotExperimentsDetailsTableComponent
        ],
        component: DotExperimentsConfigurationGoalsComponent,
        componentProviders: [],
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
            mockProvider(DotMessagePipe),
            mockProvider(DotHttpErrorManagerService)
        ]
    });
    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        store = spectator.inject(DotExperimentsConfigurationStore);

        dotExperimentsService = spectator.inject(DotExperimentsService);
        dotExperimentsService.getById.mockReturnValue(of(EXPERIMENT_MOCK));

        store.loadExperiment(EXPERIMENT_MOCK.id);

        spectator.detectChanges();
    });

    it('should render the card', () => {
        expect(spectator.queryAll(Card).length).toEqual(1);
        expect(spectator.query(byTestId('goals-card-name'))).toContainText('Goal');
        expect(spectator.query(byTestId('goals-card-name'))).toHaveClass('p-label-input-required');
        expect(spectator.query(byTestId('goals-add-button'))).toExist();
    });

    it('should render empty message of select a goal', () => {
        expect(spectator.query(byTestId('goals-empty-msg'))).toContainText('empty message');
    });

    it('should enabled the button of add goal', () => {
        const addButton = spectator.query(byTestId('goals-add-button')) as HTMLButtonElement;

        expect(addButton.disabled).not.toBe(true);
    });

    it('should disable the button of add goal if a goal was selected already', () => {
        const vmMock$: {
            experimentId: string;
            goals: Goals;
            status: StepStatus;
            isExperimentADraft: boolean;
        } = {
            experimentId: EXPERIMENT_MOCK.id,
            goals: GoalsMock,
            status: {
                status: ComponentStatus.IDLE,
                isOpen: false,
                experimentStep: null
            },
            isExperimentADraft: true
        };

        spectator.component.vm$ = of(vmMock$);
        spectator.detectComponentChanges();

        const addButton = spectator.query(byTestId('goals-add-button')) as HTMLButtonElement;
        expect(addButton.disabled).toBe(true);
        expect(spectator.query(DotExperimentsDetailsTableComponent)).toExist();
    });

    it('should call openSelectGoalSidebar if you click the add goal button', () => {
        jest.spyOn(spectator.component, 'openSelectGoalSidebar');

        const addButton = spectator.query(byTestId('goals-add-button')) as HTMLButtonElement;
        spectator.click(addButton);

        expect(spectator.component.openSelectGoalSidebar).toHaveBeenCalledTimes(1);
    });

    it('should show sidebar and close (remove it)', () => {
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

    it('should show a confirmation to delete a goal', () => {
        jest.spyOn(store, 'deleteGoal');
        const vmMock$: {
            experimentId: string;
            goals: Goals;
            status: StepStatus;
            isExperimentADraft: boolean;
        } = {
            experimentId: EXPERIMENT_MOCK.id,
            goals: GoalsMock,
            status: {
                status: ComponentStatus.IDLE,
                isOpen: false,
                experimentStep: null
            },
            isExperimentADraft: true
        };

        spectator.component.vm$ = of(vmMock$);

        spectator.detectComponentChanges();

        const deleteIcon = spectator.query(byTestId('goal-delete-button'));
        spectator.click(deleteIcon);

        expect(spectator.query(ConfirmPopup)).toExist();

        confirmPopupComponent = spectator.query(ConfirmPopup);
        confirmPopupComponent.accept();

        expect(store.deleteGoal).toHaveBeenCalled();
    });

    it('should disable delete button and show tooltip when experiment is nos on draft', () => {
        const vmMock$: {
            experimentId: string;
            goals: Goals;
            status: StepStatus;
            isExperimentADraft: boolean;
        } = {
            experimentId: EXPERIMENT_MOCK_WITH_GOAL.id,
            goals: EXPERIMENT_MOCK_WITH_GOAL.goals,
            status: {
                status: ComponentStatus.IDLE,
                isOpen: false,
                experimentStep: null
            },
            isExperimentADraft: false
        };

        spectator.component.vm$ = of(vmMock$);
        spectator.detectComponentChanges();

        expect(spectator.query(byTestId('goal-delete-button'))).toHaveAttribute('disabled');
        expect(spectator.query(Tooltip)?.disabled).toEqual(false);
    });

    it('should disable tooltip if is on draft', () => {
        expect(spectator.query(Tooltip)?.disabled).toEqual(true);
    });

    it('should disable button and show tooltip when experiment is nos on draft', () => {
        const vmMock$: {
            experimentId: string;
            goals: Goals | null;
            status: StepStatus;
            isExperimentADraft: boolean;
        } = {
            experimentId: EXPERIMENT_MOCK.id,
            goals: null,
            status: {
                status: ComponentStatus.IDLE,
                isOpen: false,
                experimentStep: null
            },
            isExperimentADraft: false
        };

        spectator.component.vm$ = of(vmMock$);

        spectator.detectComponentChanges();

        expect(spectator.query(byTestId('goals-add-button'))).toHaveAttribute('disabled');
        expect(spectator.query(Tooltip)?.disabled).toEqual(false);
    });
});
