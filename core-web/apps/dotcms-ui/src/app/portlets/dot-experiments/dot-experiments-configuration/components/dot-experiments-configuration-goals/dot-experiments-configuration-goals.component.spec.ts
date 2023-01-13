import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator';
import { of } from 'rxjs';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Card, CardModule } from 'primeng/card';

import { DotMessageService } from '@dotcms/data-access';
import { ExperimentSteps, Goals, Status, StepStatus } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotExperimentsConfigurationGoalSelectComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-goal-select/dot-experiments-configuration-goal-select.component';
import { DotExperimentsConfigurationGoalsComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-goals/dot-experiments-configuration-goals.component';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { ExperimentMocks, GoalsMock } from '@portlets/dot-experiments/test/mocks';
import { DotDynamicDirective } from '@portlets/shared/directives/dot-dynamic.directive';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.goals.name': 'Goals',
    'experiments.configure.goals.add': 'button add',
    'experiments.goal.reach_page.name': 'reach_page',
    'experiments.goal.reach_page.description': 'description',
    'experiments.configure.goals.no.seleted.goal.message': 'empty message'
});
describe('DotExperimentsConfigurationGoalsComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationGoalsComponent>;
    let store: DotExperimentsConfigurationStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;

    const createComponent = createComponentFactory({
        imports: [
            ButtonModule,
            CardModule,
            DotExperimentsConfigurationGoalSelectComponent,
            DotDynamicDirective
        ],
        component: DotExperimentsConfigurationGoalsComponent,
        componentProviders: [],
        providers: [
            DotExperimentsConfigurationStore,
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            mockProvider(DotExperimentsService),
            mockProvider(MessageService)
        ]
    });
    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        store = spectator.inject(DotExperimentsConfigurationStore);

        dotExperimentsService = spectator.inject(DotExperimentsService);
        dotExperimentsService.getById.and.returnValue(of(ExperimentMocks[0]));

        store.loadExperiment(ExperimentMocks[0].id);
    });

    it('should render the card', () => {
        spectator.detectComponentChanges();
        expect(spectator.queryAll(Card).length).toEqual(1);
        expect(spectator.query(byTestId('goals-card-name'))).toContainText('Goals');
        expect(spectator.query(byTestId('goals-add-button'))).toExist();
    });

    it('should render empty message of select a goal', () => {
        spectator.detectComponentChanges();

        expect(spectator.query(byTestId('goals-empty-msg'))).toContainText('empty message');
    });
    it('should enabled the button of add goal', () => {
        spectator.detectComponentChanges();

        const addButton = spectator.query(byTestId('goals-add-button')) as HTMLButtonElement;

        expect(addButton.disabled).not.toBe(true);
    });
    it('should disable the button of add goal if a goal was selected already', () => {
        const vmMock$: { goals: Goals; status: StepStatus } = {
            goals: GoalsMock,
            status: {
                status: Status.IDLE,
                isOpen: false,
                experimentStep: null
            }
        };

        spectator.component.vm$ = of(vmMock$);
        spectator.detectComponentChanges();

        const addButton = spectator.query(byTestId('goals-add-button')) as HTMLButtonElement;

        expect(addButton.disabled).toBe(true);
    });

    it('should call openSelectGoalSidebar if you click the add goal button', () => {
        spyOn(spectator.component, 'openSelectGoalSidebar');

        spectator.detectComponentChanges();

        const addButton = spectator.query(byTestId('goals-add-button')) as HTMLButtonElement;
        spectator.click(addButton);

        spectator.detectComponentChanges();

        expect(spectator.component.openSelectGoalSidebar).toHaveBeenCalledTimes(1);
    });
    it('should show sidebar and close (remove it)', () => {
        store.setSidebarStatus({
            experimentStep: ExperimentSteps.GOAL,
            isOpen: true
        });

        spectator.detectComponentChanges();
        expect(spectator.query(DotExperimentsConfigurationGoalSelectComponent)).toExist();

        store.setSidebarStatus({
            experimentStep: ExperimentSteps.GOAL,
            isOpen: false
        });

        spectator.detectComponentChanges();
        expect(spectator.query(DotExperimentsConfigurationGoalSelectComponent)).not.toExist();
    });
});
