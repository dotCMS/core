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
import { Tooltip } from 'primeng/tooltip';

import {
    DotExperimentsService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { DotExperimentStatus, ExperimentSteps } from '@dotcms/dotcms-models';
import {
    ACTIVE_ROUTE_MOCK_CONFIG,
    getExperimentMock,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { DotExperimentsConfigurationTrafficComponent } from './dot-experiments-configuration-traffic.component';

import { DotExperimentsConfigurationStore } from '../../store/dot-experiments-configuration-store';
import { DotExperimentsConfigurationTrafficSplitAddComponent } from '../dot-experiments-configuration-traffic-split-add/dot-experiments-configuration-traffic-split-add.component';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.traffic.name': 'Traffic',
    'experiments.configure.traffic.split.name': 'Split'
});

const EXPERIMENT_MOCK = getExperimentMock(0);

describe('DotExperimentsConfigurationTrafficComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationTrafficComponent>;
    let store: DotExperimentsConfigurationStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;

    const createComponent = createComponentFactory({
        imports: [ButtonModule, CardModule],
        component: DotExperimentsConfigurationTrafficComponent,
        componentProviders: [],
        providers: [
            DotExperimentsConfigurationStore,
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            mockProvider(DotExperimentsService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(ActivatedRoute, ACTIVE_ROUTE_MOCK_CONFIG),
            mockProvider(MessageService),
            mockProvider(ConfirmationService)
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

    it('should render split and allocation rows', () => {
        expect(spectator.queryAll(Card).length).toEqual(2);
        expect(spectator.query(byTestId('traffic-card-title'))).toHaveText('Traffic');
        expect(spectator.query(byTestId('traffic-allocation-button'))).toExist();
        expect(spectator.query(byTestId('traffic-step-done'))).toHaveClass('isDone');
    });

    it('should render indicator in gray', () => {
        dotExperimentsService.getById.mockReturnValue(
            of({ ...EXPERIMENT_MOCK, ...{ trafficAllocation: null } })
        );

        store.loadExperiment(EXPERIMENT_MOCK.id);
        spectator.detectChanges();

        expect(spectator.query(byTestId('traffic-step-done'))).not.toHaveClass('isDone');
    });

    it('should open sidebar of traffic allocation', () => {
        jest.spyOn(store, 'openSidebar');
        spectator.click(byTestId('traffic-allocation-button'));

        expect(store.openSidebar).toHaveBeenCalledWith(ExperimentSteps.TRAFFIC_LOAD);
    });

    it('should open sidebar of traffic split', () => {
        //tested this way because the sidebar is called from variant component
        store.setSidebarStatus({
            experimentStep: ExperimentSteps.TRAFFICS_SPLIT,
            isOpen: true
        });
        expect(spectator.query(DotExperimentsConfigurationTrafficSplitAddComponent)).toExist();
    });

    it('should disable tooltip if is on draft', () => {
        expect(spectator.query(Tooltip).disabled).toEqual(true);
    });

    it('should disable button and show tooltip when experiment has an error label', () => {
        dotExperimentsService.getById.mockReturnValue(
            of({
                ...EXPERIMENT_MOCK,
                ...{ status: DotExperimentStatus.RUNNING }
            })
        );

        store.loadExperiment(EXPERIMENT_MOCK.id);

        spectator.detectChanges();

        expect(spectator.query(byTestId('traffic-allocation-button'))).toHaveAttribute('disabled');

        expect(spectator.query(Tooltip).disabled).toEqual(false);
    });
});
