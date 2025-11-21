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
import { Drawer } from 'primeng/drawer';
import { SelectModule } from 'primeng/select';

import {
    DotExperimentsService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import {
    DefaultGoalConfiguration,
    ExperimentSteps,
    GOAL_OPERATORS,
    GOAL_PARAMETERS,
    GOAL_TYPES
} from '@dotcms/dotcms-models';
import { DotDropdownDirective, DotMessagePipe } from '@dotcms/ui';
import {
    ACTIVE_ROUTE_MOCK_CONFIG,
    getExperimentMock,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { DotExperimentsConfigurationGoalSelectComponent } from './dot-experiments-configuration-goal-select.component';

import { DotExperimentsGoalConfigurationUrlParameterComponentComponent } from '../../../shared/ui/dot-experiments-goal-configuration-url-parameter-component/dot-experiments-goal-configuration-url-parameter-component.component';
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

    const createComponent = createComponentFactory({
        imports: [ButtonModule, CardModule, SelectModule, DotMessagePipe, DotDropdownDirective],
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

        const bounceRateOption = spectator.query(byTestId('dot-options-item-header_BOUNCE_RATE'));
        const exitRateOption = spectator.query(byTestId('dot-options-item-header_EXIT_RATE'));
        const reachPageOption = spectator.query(byTestId('dot-options-item-header_REACH_PAGE'));
        const urlParameterOption = spectator.query(
            byTestId('dot-options-item-header_URL_PARAMETER')
        );

        spectator.click(bounceRateOption);
        expect((spectator.query(byTestId('goal-name-input')) as HTMLInputElement).value).toEqual(
            'Minimize Bounce Rate'
        );

        spectator.click(exitRateOption);
        expect((spectator.query(byTestId('goal-name-input')) as HTMLInputElement).value).toEqual(
            'Detect exit rate'
        );

        spectator.click(reachPageOption);
        expect((spectator.query(byTestId('goal-name-input')) as HTMLInputElement).value).toEqual(
            'Maximize Reaching a Page'
        );

        spectator.click(urlParameterOption);
        expect((spectator.query(byTestId('goal-name-input')) as HTMLInputElement).value).toEqual(
            'Detect URL Parameter'
        );
    });

    it('should not change the Goal Name if the user set one', () => {
        const customName = 'Test Goal';

        spectator.typeInElement(
            customName,
            spectator.query(byTestId('goal-name-input')) as SpectatorElement
        );

        const bounceRateOption = spectator.query(byTestId('dot-options-item-header_BOUNCE_RATE'));
        const reachPageOption = spectator.query(byTestId('dot-options-item-header_REACH_PAGE'));

        spectator.click(bounceRateOption);
        spectator.click(reachPageOption);

        expect((spectator.query(byTestId('goal-name-input')) as HTMLInputElement).value).toEqual(
            customName
        );
    });

    it('should be a form valid in case of click on a No content option item', () => {
        const bounceRateOption = spectator.query(byTestId('dot-options-item-header_BOUNCE_RATE'));

        spectator.component.form.get('primary.name').setValue('default');
        spectator.component.form.updateValueAndValidity();

        spectator.click(bounceRateOption);

        const applyBtn = spectator.query(byTestId('add-goal-button')) as HTMLButtonElement;
        spectator.detectComponentChanges();

        expect(spectator.component.form.valid).toEqual(true);
        expect(applyBtn.disabled).toEqual(false);

        const exitRateOption = spectator.query(byTestId('dot-options-item-header_EXIT_RATE'));
        spectator.click(exitRateOption);
        spectator.detectComponentChanges();

        expect(spectator.component.form.valid).toEqual(true);
        expect(applyBtn.disabled).toEqual(false);
    });

    it('should be a form invalid in case of click on an option item with conditions', () => {
        const reachPageOption = spectator.query(byTestId('dot-options-item-header_REACH_PAGE'));

        spectator.click(reachPageOption);

        const applyBtn = spectator.query(byTestId('add-goal-button')) as HTMLButtonElement;
        spectator.detectComponentChanges();

        expect(spectator.component.form.valid).toEqual(false);
        expect(applyBtn.disabled).toEqual(true);

        const urlParameterOption = spectator.query(
            byTestId('dot-options-item-header_URL_PARAMETER')
        );
        spectator.click(urlParameterOption);

        spectator.detectComponentChanges();

        expect(spectator.component.form.valid).toEqual(false);
        expect(applyBtn.disabled).toEqual(true);
    });

    it('should be a valid form when select REACH_PAGE', async () => {
        spectator.detectChanges();

        const reachPageOption = spectator.query(byTestId('dot-options-item-header_REACH_PAGE'));
        spectator.click(reachPageOption);

        spectator.detectComponentChanges();

        const applyBtn = spectator.query(byTestId('add-goal-button')) as HTMLButtonElement;

        expect(applyBtn.disabled).toEqual(true);

        const invalidFormValues = {
            primary: {
                name: 'default',
                type: GOAL_TYPES.REACH_PAGE,
                conditions: [{ parameter: '', operator: null, value: '' }]
            }
        };

        const validFormValues = {
            primary: {
                name: 'default',
                type: GOAL_TYPES.REACH_PAGE,
                conditions: [
                    {
                        parameter: GOAL_PARAMETERS.URL,
                        operator: GOAL_OPERATORS.EQUALS,
                        value: '1111'
                    }
                ]
            }
        };

        await spectator.fixture.whenStable();

        // Invalid path
        spectator.component.form.patchValue(invalidFormValues, { emitEvent: false });
        spectator.component.form.updateValueAndValidity();
        spectator.detectChanges();

        expect(spectator.component.form.valid).toEqual(false);
        expect(applyBtn.disabled).toEqual(true);

        // Invalid path
        spectator.component.form.setValue(validFormValues, { emitEvent: false });
        spectator.component.form.updateValueAndValidity();
        spectator.detectChanges();

        expect(spectator.component.form.valid).toEqual(true);
        expect(applyBtn.disabled).toEqual(false);
    });

    it('should be a valid form when select URL_PARAMETER', async () => {
        spectator.detectChanges();

        const urlParameterOption = spectator.query(
            byTestId('dot-options-item-header_URL_PARAMETER')
        );
        spectator.click(urlParameterOption);

        spectator.detectComponentChanges();

        const applyBtn = spectator.query<HTMLButtonElement>(byTestId('add-goal-button'));
        expect(applyBtn.disabled).toEqual(true);

        const invalidFormValues = {
            primary: {
                name: 'default',
                type: GOAL_TYPES.URL_PARAMETER,
                conditions: [
                    {
                        parameter: '',
                        operator: null,
                        value: {
                            name: '',
                            value: ''
                        }
                    }
                ]
            }
        };

        const validFormValuesExistOperator = {
            primary: {
                name: 'default',
                type: GOAL_TYPES.URL_PARAMETER,
                conditions: [
                    {
                        parameter: GOAL_PARAMETERS.URL,
                        operator: GOAL_OPERATORS.EXISTS,
                        value: {
                            name: 'queryParam',
                            value: ''
                        }
                    }
                ]
            }
        };

        const component = spectator.query(
            DotExperimentsGoalConfigurationUrlParameterComponentComponent
        );

        expect(component).toExist();

        await spectator.fixture.whenStable();

        // Invalid path
        spectator.component.form.setValue(invalidFormValues, { emitEvent: false });
        spectator.component.form.updateValueAndValidity();
        spectator.detectChanges();

        expect(spectator.component.form.valid).toEqual(false);
        expect(applyBtn.disabled).toEqual(true);

        // Invalid path
        spectator.component.form.setValue(validFormValuesExistOperator, { emitEvent: false });
        spectator.component.form.updateValueAndValidity();
        spectator.detectChanges();

        expect(spectator.component.form.valid).toEqual(false);
        expect(applyBtn.disabled).toEqual(true);
    });

    it('should call setSelectedGoal from the store when a item is selected and the button of apply is clicked', async () => {
        jest.spyOn(store, 'setSelectedGoal');
        const expectedGoal = {
            experimentId: EXPERIMENT_MOCK.id,
            goals: {
                primary: {
                    name: 'default',
                    type: GOAL_TYPES.BOUNCE_RATE,
                    conditions: []
                }
            }
        };

        spectator.component.form.get('primary.name').setValue('default');
        spectator.component.form.updateValueAndValidity();

        spectator.detectChanges();

        const bounceRateOption = spectator.query(byTestId('dot-options-item-header_BOUNCE_RATE'));

        spectator.click(bounceRateOption);

        const applyBtn = spectator.query<HTMLButtonElement>(byTestId('add-goal-button'));
        spectator.detectChanges();

        spectator.click(applyBtn);
        await spectator.fixture.whenStable();

        expect(spectator.component.form.valid).toEqual(true);
        expect(applyBtn.disabled).toEqual(false);
        expect(store.setSelectedGoal).toHaveBeenCalledWith(expectedGoal);
    });

    it('should disable submit button if the input name of the goal has more than MAX_INPUT_DESCRIPTIVE_LENGTH constant', () => {
        const invalidFormValues = {
            primary: {
                ...DefaultGoalConfiguration.primary,
                name: 'Really really really really really Really really really really really Really really Really really really really really Really really really really really Really really Really really really really really Really really really really really Really really Really really really really really Really really really really really Really really Really really really really really Really really really really really Really really Really really really really really Really really really really really Really really really really really long name for a goal',
                type: GOAL_TYPES.BOUNCE_RATE,
                conditions: []
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
        const reachPageOption = spectator.query(byTestId('dot-options-item-header_REACH_PAGE'));

        spectator.click(reachPageOption);
        spectator.detectComponentChanges();

        const reachPageOptionContent = spectator.query(
            byTestId('dot-options-item-content_REACH_PAGE')
        );

        expect(reachPageOptionContent).toHaveClass('expanded');
    });

    it('should emit closedSidebar when the sidebar its closed', () => {
        spectator.detectChanges();

        const sidebar = spectator.query(Drawer);
        jest.spyOn(spectator.component, 'closeSidebar');

        store.setSidebarStatus({
            experimentStep: ExperimentSteps.GOAL,
            isOpen: false
        });

        spectator.detectChanges();

        expect(sidebar.visible).toEqual(false);
    });

    it('should render coming soon placeholder', () => {
        expect(spectator.query(DotExperimentsGoalsComingSoonComponent)).not.toBeNull();
    });
});
