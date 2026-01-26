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
import {
    DotExperimentStatus,
    ExperimentSteps,
    TrafficProportionTypes
} from '@dotcms/dotcms-models';
import { DotDynamicDirective } from '@dotcms/ui';
import {
    ACTIVE_ROUTE_MOCK_CONFIG,
    getExperimentMock,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { DotExperimentsConfigurationTrafficComponent } from './dot-experiments-configuration-traffic.component';

import { DotExperimentsConfigurationStore } from '../../store/dot-experiments-configuration-store';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.traffic.name': 'Traffic',
    'experiments.configure.traffic.load.name': 'Traffic Load'
});

const EXPERIMENT_MOCK = getExperimentMock(0);
const EXPERIMENT_MOCK_WITH_TRAFFIC = {
    ...getExperimentMock(0),
    trafficAllocation: 100,
    trafficProportion: {
        type: TrafficProportionTypes.SPLIT_EVENLY,
        variants: []
    }
};

describe('DotExperimentsConfigurationTrafficComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationTrafficComponent>;
    let store: DotExperimentsConfigurationStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;

    const createComponent = createComponentFactory({
        imports: [ButtonModule, CardModule, TooltipModule, DotDynamicDirective],
        component: DotExperimentsConfigurationTrafficComponent,
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
        dotExperimentsService.getById.mockReturnValue(of(EXPERIMENT_MOCK));

        store.loadExperiment(EXPERIMENT_MOCK.id);
        spectator.detectChanges();
    });

    it('should render split and allocation rows', () => {
        expect(spectator.queryAll(Card).length).toEqual(2);
        expect(spectator.query(byTestId('traffic-card-title'))).toContainText('Traffic');
        expect(spectator.query(byTestId('card-title'))).toContainText('Traffic Load');
        expect(spectator.query(byTestId('traffic-allocation-button'))).toExist();
    });

    it('should render indicator in gray', () => {
        // Mock experiment without trafficAllocation to show gray indicator
        dotExperimentsService.getById.mockReturnValue(
            of({
                ...EXPERIMENT_MOCK,
                trafficAllocation: null
            })
        );
        store.loadExperiment(EXPERIMENT_MOCK.id);
        spectator.detectChanges();

        const indicator = spectator.query(byTestId('traffic-card-title')).querySelector('i');
        expect(indicator).toHaveClass('text-gray-500');
    });

    it('should open sidebar of traffic allocation', () => {
        jest.spyOn(store, 'openSidebar');

        const allocationButton = spectator.query(byTestId('traffic-allocation-button'));
        const button = allocationButton.querySelector('button') || allocationButton;
        spectator.click(button);

        expect(store.openSidebar).toHaveBeenCalledWith(ExperimentSteps.TRAFFIC_LOAD);
    });

    it('should open sidebar of traffic split', () => {
        dotExperimentsService.getById.mockReturnValue(of(EXPERIMENT_MOCK_WITH_TRAFFIC));
        store.loadExperiment(EXPERIMENT_MOCK.id);
        spectator.detectChanges();

        jest.spyOn(store, 'openSidebar');

        const allocationButton = spectator.query(byTestId('traffic-allocation-button'));
        const button = allocationButton.querySelector('button') || allocationButton;
        spectator.click(button);

        expect(store.openSidebar).toHaveBeenCalledWith(ExperimentSteps.TRAFFIC_LOAD);
    });

    it('should disable tooltip if is on draft', () => {
        expect(spectator.query(Tooltip).disabled).toEqual(true);
    });

    it('should disable button and show tooltip when experiment has an error label', () => {
        dotExperimentsService.getById.mockReturnValue(
            of({
                ...EXPERIMENT_MOCK,
                status: DotExperimentStatus.RUNNING
            })
        );

        store.loadExperiment(EXPERIMENT_MOCK.id);
        spectator.detectChanges();

        const allocationButton = spectator.query(byTestId('traffic-allocation-button'));
        const button = allocationButton.querySelector('button') || allocationButton;
        expect(button.hasAttribute('disabled')).toBe(true);
        expect(spectator.query(Tooltip).disabled).toEqual(false);
    });
});
