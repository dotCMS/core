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
import { CardModule } from 'primeng/card';
import { Sidebar } from 'primeng/sidebar';

import { DotMessageService } from '@dotcms/data-access';
import { DefaultGoalConfiguration, ExperimentSteps, GOAL_TYPES } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { ExperimentMocks } from '@portlets/dot-experiments/test/mocks';

import { DotExperimentsConfigurationGoalSelectComponent } from './dot-experiments-configuration-goal-select.component';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.goals.sidebar.header': 'Select a goal',
    'experiments.configure.goals.sidebar.header.button': 'Add'
});
const EXPERIMENT_ID = ExperimentMocks[0].id;
describe('DotExperimentsConfigurationGoalSelectComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationGoalSelectComponent>;
    let store: DotExperimentsConfigurationStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;
    let sidebar: Sidebar;

    const createComponent = createComponentFactory({
        imports: [ButtonModule, CardModule],
        component: DotExperimentsConfigurationGoalSelectComponent,
        componentProviders: [],
        providers: [
            DotExperimentsConfigurationStore,
            mockProvider(DotExperimentsService),
            mockProvider(MessageService),
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
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
        store.setSidebarStatus({
            experimentStep: ExperimentSteps.GOAL,
            isOpen: true
        });
        spectator.detectChanges();
    });
    it('should have a form', () => {
        expect(spectator.query(byTestId('select-goal-form'))).toExist();
    });
    it('should call saveForm() on add button click and form valid', () => {
        const selectedGoal = GOAL_TYPES.REACH_PAGE;
        const expectedGoal = {
            experimentId: EXPERIMENT_ID,
            goals: {
                ...DefaultGoalConfiguration,
                primary: {
                    ...DefaultGoalConfiguration.primary,
                    type: selectedGoal
                }
            }
        };
        spyOn(store, 'setSelectedGoal');

        const formValues = {
            goal: selectedGoal
        };

        spectator.component.form.setValue(formValues);
        spectator.detectComponentChanges();

        const submitButton = spectator.query(byTestId('add-goal-button')) as HTMLButtonElement;
        expect(submitButton.disabled).toBeFalse();

        expect(submitButton).toContainText('Add');
        expect(spectator.component.form.valid).toBeTrue();

        spectator.click(submitButton);
        expect(store.setSelectedGoal).toHaveBeenCalledWith(expectedGoal);
    });

    it('should emit closedSidebar when the sidebar its closed', (done) => {
        spyOn(spectator.component, 'closeSidebar');
        store.setSidebarStatus({
            experimentStep: ExperimentSteps.GOAL,
            isOpen: false
        });

        spectator.detectComponentChanges();
        sidebar = spectator.query(Sidebar);
        expect(sidebar.visible).toEqual(false);

        sidebar.onHide.subscribe(() => {
            expect(spectator.component.closeSidebar).toHaveBeenCalled();
            done();
        });
    });
});
