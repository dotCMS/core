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
import { DropdownModule } from 'primeng/dropdown';
import { Sidebar } from 'primeng/sidebar';

import { DotMessagePipe } from '@dotcms/app/view/pipes';
import { DotMessageService } from '@dotcms/data-access';
import { DefaultGoalConfiguration, ExperimentSteps, GOAL_TYPES } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { getExperimentMock } from '@portlets/dot-experiments/test/mocks';
import { DotDropdownDirective } from '@portlets/shared/directives/dot-dropdown.directive';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import { DotExperimentsConfigurationGoalSelectComponent } from './dot-experiments-configuration-goal-select.component';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.goals.sidebar.header': 'Select a goal',
    'experiments.configure.goals.sidebar.header.button': 'Apply',
    'experiments.configure.goals.name.default': 'Primary goal',
    'experiments.goal.conditions.maximize.reach.page': 'Maximize Reaching a Page',
    'experiments.goal.conditions.minimize.bounce.rate': 'Minimize Bounce Rate'
});

const EXPERIMENT_MOCK = getExperimentMock(0);

describe('DotExperimentsConfigurationGoalSelectComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationGoalSelectComponent>;
    let store: DotExperimentsConfigurationStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;
    let sidebar: Sidebar;

    const createComponent = createComponentFactory({
        imports: [
            ButtonModule,
            CardModule,
            DropdownModule,
            DotMessagePipeModule,
            DotDropdownDirective
        ],
        component: DotExperimentsConfigurationGoalSelectComponent,
        providers: [
            DotExperimentsConfigurationStore,
            mockProvider(DotExperimentsService),
            mockProvider(MessageService),
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            mockProvider(DotHttpErrorManagerService),
            DotMessagePipe
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
            'Maximize Reaching a Page'
        );
    });

    it('should not change the name if the user set one', () => {
        const customName = 'Test Goal';

        spectator.typeInElement(customName, spectator.query(byTestId('goal-name-input')));

        const optionsRendered = spectator.queryAll(byTestId('dot-options-item-header'));

        spectator.click(optionsRendered[0]);
        spectator.click(optionsRendered[1]);

        expect((spectator.query(byTestId('goal-name-input')) as HTMLInputElement).value).toEqual(
            customName
        );
    });

    it('should have rendered BOUCE_RATE and REACH_PAGE options items', () => {
        const optionsRendered = spectator.queryAll(byTestId('dot-options-item-header'));

        expect(optionsRendered.length).toBe(2);
    });

    it('should be a form valid in case of click on a No content option item', () => {
        const bounceRateOption = spectator.query(byTestId('dot-options-item-header'));

        spectator.component.form.get('primary.name').setValue('default');
        spectator.component.form.updateValueAndValidity();

        spectator.click(bounceRateOption);

        const applyBtn = spectator.query(byTestId('add-goal-button')) as HTMLButtonElement;
        spectator.detectComponentChanges();

        expect(spectator.component.form.valid).toBeTrue();
        expect(applyBtn.disabled).toBeFalse();
    });

    it('should be a form invalid in case of click on an option item with content', () => {
        const reachPageOption = spectator.queryLast(byTestId('dot-options-item-header'));

        spectator.click(reachPageOption);

        const applyBtn = spectator.query(byTestId('add-goal-button')) as HTMLButtonElement;
        spectator.detectComponentChanges();

        expect(spectator.component.form.valid).toBeFalse();
        expect(applyBtn.disabled).toBeTrue();
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

        expect(spectator.component.form.valid).toBeTrue();
        expect(applyBtn.disabled).toBeFalse();
    });

    it('should call setSelectedGoal from the store when a item is selected and the button of apply is clicked', () => {
        spyOn(store, 'setSelectedGoal');
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

        expect(spectator.component.form.valid).toBeTrue();
        expect(applyBtn.disabled).toBeFalse();
        expect(store.setSelectedGoal).toHaveBeenCalledWith(expectedGoal);
    });

    it('should add the class expand to an option clicked that contains content', () => {
        const reachPageOption = spectator.queryLast(byTestId('dot-options-item-header'));

        spectator.click(reachPageOption);
        spectator.detectComponentChanges();

        expect(spectator.query(byTestId('dot-options-item-content'))).toHaveClass('expanded');
    });

    it('should emit closedSidebar when the sidebar its closed', async () => {
        sidebar = spectator.query(Sidebar);
        spyOn(spectator.component, 'closeSidebar');
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
});
