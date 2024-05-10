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
import { Tooltip, TooltipModule } from 'primeng/tooltip';

import {
    DotExperimentsService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { DotExperimentStatus, ExperimentSteps } from '@dotcms/dotcms-models';
import { DotDynamicDirective } from '@dotcms/ui';
import {
    ACTIVE_ROUTE_MOCK_CONFIG,
    getExperimentMock,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { DotExperimentsConfigurationSchedulingComponent } from './dot-experiments-configuration-scheduling.component';

import { DotExperimentsConfigurationStore } from '../../store/dot-experiments-configuration-store';
import { DotExperimentsConfigurationSchedulingAddComponent } from '../dot-experiments-configuration-scheduling-add/dot-experiments-configuration-scheduling-add.component';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.scheduling.name': 'Scheduling',
    'experiments.configure.scheduling.start': 'When the experiment start',
    'experiments.configure.scheduling.setup': 'Setup'
});

const EXPERIMENT_MOCK = getExperimentMock(0);
describe('DotExperimentsConfigurationSchedulingComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationSchedulingComponent>;
    let store: DotExperimentsConfigurationStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;

    const createComponent = createComponentFactory({
        imports: [
            ButtonModule,
            CardModule,
            TooltipModule,
            DotExperimentsConfigurationSchedulingAddComponent,
            DotDynamicDirective
        ],
        component: DotExperimentsConfigurationSchedulingComponent,
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
            mockProvider(DotHttpErrorManagerService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        store = spectator.inject(DotExperimentsConfigurationStore);

        dotExperimentsService = spectator.inject(DotExperimentsService);
        dotExperimentsService.getById.mockReturnValue(
            of({ ...EXPERIMENT_MOCK, ...{ scheduling: null } })
        );

        store.loadExperiment(EXPERIMENT_MOCK.id);
        spectator.detectChanges();
    });

    it('should render the card and split and allocation rows', () => {
        expect(spectator.queryAll(Card).length).toEqual(2);
        expect(spectator.query(byTestId('scheduling-card-name'))).toHaveText('Scheduling');
        expect(spectator.query(byTestId('scheduling-card-title-row'))).toHaveText(
            'When the experiment start'
        );
        expect(spectator.query(byTestId('scheduling-setup-button'))).toContainText('Setup');
        expect(spectator.query(byTestId('schedule-step-done'))).not.toHaveClass('isDone');
    });

    it('should open sidebar on button click', () => {
        jest.spyOn(store, 'openSidebar');
        spectator.click(byTestId('scheduling-setup-button'));

        expect(store.openSidebar).toHaveBeenCalledWith(ExperimentSteps.SCHEDULING);
    });

    it('should disable tooltip if is on draft', () => {
        expect(spectator.query(Tooltip).disabled).toEqual(true);
    });

    it('should disable button and show tooltip when there is an error', () => {
        dotExperimentsService.getById.mockReturnValue(
            of({
                ...EXPERIMENT_MOCK,
                ...{ scheduling: null, status: DotExperimentStatus.RUNNING }
            })
        );

        store.loadExperiment(EXPERIMENT_MOCK.id);

        spectator.detectChanges();

        expect(spectator.query(byTestId('scheduling-setup-button'))).toHaveAttribute('disabled');
        expect(spectator.query(Tooltip).disabled).toEqual(false);
    });

    it('should set indicator in green', () => {
        dotExperimentsService.getById.mockReturnValue(of(getExperimentMock(0)));

        store.loadExperiment(EXPERIMENT_MOCK.id);
        spectator.detectChanges();

        expect(spectator.query(byTestId('schedule-step-done'))).toHaveClass('isDone');
    });
});
