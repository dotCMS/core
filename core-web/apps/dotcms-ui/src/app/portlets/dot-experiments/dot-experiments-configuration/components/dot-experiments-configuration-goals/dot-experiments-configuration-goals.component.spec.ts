import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator';
import { of } from 'rxjs';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Card, CardModule } from 'primeng/card';
import { ConfirmPopup, ConfirmPopupModule } from 'primeng/confirmpopup';
import { Tooltip, TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe } from '@dotcms/app/view/pipes';
import { DotMessageService } from '@dotcms/data-access';
import { ComponentStatus, ExperimentSteps, Goals, StepStatus } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotExperimentsConfigurationGoalSelectComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-goal-select/dot-experiments-configuration-goal-select.component';
import { DotExperimentsConfigurationGoalsComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-goals/dot-experiments-configuration-goals.component';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { getExperimentMock, GoalsMock } from '@portlets/dot-experiments/test/mocks';
import { DotDynamicDirective } from '@portlets/shared/directives/dot-dynamic.directive';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.goals.name': 'Goals',
    'experiments.configure.goals.add': 'button add',
    'experiments.goal.reach_page.name': 'reach_page',
    'experiments.goal.reach_page.description': 'description',
    'experiments.configure.goals.no.seleted.goal.message': 'empty message'
});
const EXPERIMENT_MOCK = getExperimentMock(0);
describe('DotExperimentsConfigurationGoalsComponent', () => {
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
            TooltipModule
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
        dotExperimentsService.getById.and.returnValue(of(EXPERIMENT_MOCK));

        store.loadExperiment(EXPERIMENT_MOCK.id);

        spectator.detectChanges();
    });

    it('should render the card', () => {
        expect(spectator.queryAll(Card).length).toEqual(1);
        expect(spectator.query(byTestId('goals-card-name'))).toContainText('Goals');
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
    });

    it('should call openSelectGoalSidebar if you click the add goal button', () => {
        spyOn(spectator.component, 'openSelectGoalSidebar');

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
        spyOn(store, 'deleteGoal');
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

    it('should disable tooltip if is on draft', () => {
        expect(spectator.query(Tooltip).disabled).toEqual(true);
    });

    it('should disable button and show tooltip when experiment is nos on draft', () => {
        const vmMock$: {
            experimentId: string;
            goals: Goals;
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
        expect(spectator.query(Tooltip).disabled).toEqual(false);
    });
});
