import { SpectatorElement } from '@ngneat/spectator';
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
import { CardModule } from 'primeng/card';
import { DropdownModule } from 'primeng/dropdown';
import { Sidebar } from 'primeng/sidebar';

import { DotMessageService } from '@dotcms/data-access';
import { DefaultGoalConfiguration, ExperimentSteps, GOAL_TYPES } from '@dotcms/dotcms-models';
import { DotExperimentsService } from '@dotcms/portlets/dot-experiments/data-access';
import { DotMessagePipe } from '@dotcms/ui';
import {
    ACTIVE_ROUTE_MOCK_CONFIG,
    getExperimentMock,
    MockDotMessageService
} from '@dotcms/utils-testing';
import { DotDropdownDirective } from '@portlets/shared/directives/dot-dropdown.directive';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import { DotExperimentsConfigurationGoalSelectComponent } from './dot-experiments-configuration-goal-select.component';

import { DotExperimentsGoalsComingSoonComponent } from '../../../shared/ui/dot-experiments-goals-coming-soon/dot-experiments-goals-coming-soon.component';
import { DotExperimentsConfigurationStore } from '../../store/dot-experiments-configuration-store';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.goals.sidebar.header': 'Select a goal',
    'experiments.configure.goals.sidebar.header.button': 'Apply',
    'experiments.configure.goals.name.default': 'Primary goal',
    'experiments.goal.conditions.maximize.reach.page': 'Maximize Reaching a Page',
    'experiments.goal.conditions.minimize.bounce.rate': 'Minimize Bounce Rate',
    'experiments.goal.conditions.detect.exit.rate': 'Detect exit rate',
    'experiments.goal.conditions.detect.queryparam.in.url': 'Detect URL Parameter'
});

const EXPERIMENT_MOCK = getExperimentMock(0);

describe('DotExperimentsConfigurationGoalSelectComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationGoalSelectComponent>;
    let store: DotExperimentsConfigurationStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;
    let sidebar: Sidebar;

    const createComponent = createComponentFactory({
        imports: [ButtonModule, CardModule, DropdownModule, DotMessagePipe, DotDropdownDirective],
        component: DotExperimentsConfigurationGoalSelectComponent,
        providers: [
            DotExperimentsConfigurationStore,
            mockProvider(DotExperimentsService),
            mockProvider(MessageService),
            mockProvider(ActivatedRoute, ACTIVE_ROUTE_MOCK_CONFIG),
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            mockProvider(DotHttpErrorManagerService),
            mockProvider(ConfirmationService),
            DotMessagePipe
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        store = spectator.inject(DotExperimentsConfigurationStore);
        dotExperimentsService = spectator.inject(DotExperimentsService);
        dotExperimentsService.getById.mockReturnValue(of(EXPERIMENT_MOCK));
        dotExperimentsService.setGoal.mockReturnValue(of());

        store.loadExperiment(EXPERIMENT_MOCK.id);
        store.setSidebarStatus({
            experimentStep: ExperimentSteps.GOAL,
            isOpen: true
        });
        spectator.detectChanges();
    });

    it('should have a form & autofocus', () => {
        expect(spectator.query(byTestId('select-goal-form'))).toExist();
        expect((spectator.query(byTestId('goal-name-input')) as HTMLInputElement).value).toEqual(
            ''
        );
    });

    it('should set the default name when type change', () => {
        expect((spectator.query(byTestId('goal-name-input')) as HTMLInputElement).value).toEqual(
            ''
        );

        const optionsRendered = spectator.queryAll(byTestId('dot-options-item-header'));

        spectator.click(optionsRendered[0]);
        expect((spectator.query(byTestId('goal-name-input')) as HTMLInputElement).value).toEqual(
            'Minimize Bounce Rate'
        );

        spectator.click(optionsRendered[1]);
        expect((spectator.query(byTestId('goal-name-input')) as HTMLInputElement).value).toEqual(
            'Detect exit rate'
        );

        spectator.click(optionsRendered[2]);
        expect((spectator.query(byTestId('goal-name-input')) as HTMLInputElement).value).toEqual(
            'Maximize Reaching a Page'
        );

        spectator.click(optionsRendered[3]);
        expect((spectator.query(byTestId('goal-name-input')) as HTMLInputElement).value).toEqual(
            'Detect URL Parameter'
        );
    });

    it('should not change the name if the user set one', () => {
        const customName = 'Test Goal';

        spectator.typeInElement(
            customName,
            spectator.query(byTestId('goal-name-input')) as SpectatorElement
        );

        const optionsRendered = spectator.queryAll(byTestId('dot-options-item-header'));

        spectator.click(optionsRendered[0]);
        spectator.click(optionsRendered[1]);

        expect((spectator.query(byTestId('goal-name-input')) as HTMLInputElement).value).toEqual(
            customName
        );
    });

    it('should have rendered BOUNCE_RATE and REACH_PAGE and URL_PARAMETER options items', () => {
        const optionsRendered = spectator.queryAll(byTestId('dot-options-item-header'));
        const EXPECTED_GOAL_OPTIONS_COUNT = 4;

        expect(optionsRendered.length).toBe(EXPECTED_GOAL_OPTIONS_COUNT);
    });

    it('should be a form valid in case of click on a No content option item', () => {
        const bounceRateOption = spectator.query(byTestId('dot-options-item-header'));

        spectator.component.form.get('primary.name').setValue('default');
        spectator.component.form.updateValueAndValidity();

        spectator.click(bounceRateOption);

        const applyBtn = spectator.query(byTestId('add-goal-button')) as HTMLButtonElement;
        spectator.detectComponentChanges();

        expect(spectator.component.form.valid).toEqual(true);
        expect(applyBtn.disabled).toEqual(false);
    });

    it('should be a form invalid in case of click on an option item with content', () => {
        const reachPageOption = spectator.queryLast(byTestId('dot-options-item-header'));

        // eslint-disable-next-line @typescript-eslint/no-non-null-assertion
        spectator.click(reachPageOption!);

        const applyBtn = spectator.query(byTestId('add-goal-button')) as HTMLButtonElement;
        spectator.detectComponentChanges();

        expect(spectator.component.form.valid).toEqual(false);
        expect(applyBtn.disabled).toEqual(true);
    });

    it('should be a form valid in case of click on an option item with content and all input filled', () => {
        const bounceRateOption = spectator.queryLast(byTestId('dot-options-item-header'));

        spectator.click(bounceRateOption);

        const applyBtn = spectator.query(byTestId('add-goal-button')) as HTMLButtonElement;
        spectator.detectComponentChanges();

        const formValues = {
            primary: {
                ...DefaultGoalConfiguration.primary,
                name: 'default',
                type: GOAL_TYPES.BOUNCE_RATE
            }
        };
        spectator.component.form.setValue(formValues);
        spectator.component.form.updateValueAndValidity();

        spectator.detectComponentChanges();

        expect(spectator.component.form.valid).toEqual(true);
        expect(applyBtn.disabled).toEqual(false);
    });

    it('should call setSelectedGoal from the store when a item is selected and the button of apply is clicked', () => {
        jest.spyOn(store, 'setSelectedGoal');
        const expectedGoal = {
            experimentId: EXPERIMENT_MOCK.id,
            goals: {
                primary: {
                    name: 'default',
                    type: GOAL_TYPES.BOUNCE_RATE
                }
            }
        };

        spectator.component.form.get('primary.name').setValue('default');
        spectator.component.form.updateValueAndValidity();

        spectator.detectComponentChanges();

        const bounceRateOption = spectator.query(byTestId('dot-options-item-header'));

        spectator.click(bounceRateOption);

        const applyBtn = spectator.query(byTestId('add-goal-button')) as HTMLButtonElement;
        spectator.detectComponentChanges();

        spectator.click(applyBtn);

        expect(spectator.component.form.valid).toEqual(true);
        expect(applyBtn.disabled).toEqual(false);
        expect(store.setSelectedGoal).toHaveBeenCalledWith(expectedGoal);
    });

    it('should disable submit button if the input name of the goal has more than MAX_INPUT_DESCRIPTIVE_LENGTH constant', () => {
        const invalidFormValues = {
            primary: {
                ...DefaultGoalConfiguration.primary,
                name: 'Really really really really really Really really really really really Really really Really really really really really Really really really really really Really really Really really really really really Really really really really really Really really Really really really really really Really really really really really Really really Really really really really really Really really really really really Really really Really really really really really Really really really really really Really really really really really long name for a goal',
                type: GOAL_TYPES.BOUNCE_RATE
            }
        };

        spectator.component.form.setValue(invalidFormValues);
        spectator.component.form.updateValueAndValidity();
        spectator.detectComponentChanges();

        expect(
            (spectator.query(byTestId('add-goal-button')) as HTMLButtonElement).disabled
        ).toEqual(true);
        expect(spectator.component.goalNameControl.hasError('maxlength')).toEqual(true);
        expect(spectator.component.form.valid).toEqual(false);
    });

    it('should add the class expand to an option clicked that contains content', () => {
        const goalsOption = spectator.queryAll(byTestId('dot-options-item-header'));
        const OPTION_INDEX_WITH_CONTENT = 2;

        spectator.click(goalsOption[OPTION_INDEX_WITH_CONTENT]);
        spectator.detectComponentChanges();

        const firstOptionContentRendered = spectator.query(byTestId('dot-options-item-content'));

        expect(firstOptionContentRendered).toHaveClass('expanded');
    });

    it('should emit closedSidebar when the sidebar its closed', async () => {
        sidebar = spectator.query(Sidebar);
        jest.spyOn(spectator.component, 'closeSidebar');
        store.setSidebarStatus({
            experimentStep: ExperimentSteps.GOAL,
            isOpen: false
        });

        spectator.detectComponentChanges();
        await spectator.fixture.whenStable();

        expect(sidebar.visible).toEqual(false);

        sidebar.onHide.subscribe(() => {
            expect(spectator.component.closeSidebar).toHaveBeenCalled();
        });
    });

    it('should render coming soon placeholder', () => {
        expect(spectator.query(DotExperimentsGoalsComingSoonComponent)).not.toBeNull();
    });
});
