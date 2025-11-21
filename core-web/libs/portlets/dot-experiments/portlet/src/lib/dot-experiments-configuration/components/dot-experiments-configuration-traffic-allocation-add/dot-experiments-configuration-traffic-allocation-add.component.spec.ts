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
import { InputTextModule } from 'primeng/inputtext';
import { Slider, SliderModule } from 'primeng/slider';

import {
    DotExperimentsService,
    DotHttpErrorManagerService,
    DotMessageService
} from '@dotcms/data-access';
import { ExperimentSteps } from '@dotcms/dotcms-models';
import {
    ACTIVE_ROUTE_MOCK_CONFIG,
    getExperimentMock,
    MockDotMessageService
} from '@dotcms/utils-testing';

import { DotExperimentsConfigurationTrafficAllocationAddComponent } from './dot-experiments-configuration-traffic-allocation-add.component';

import { DotExperimentsConfigurationStore } from '../../store/dot-experiments-configuration-store';

const messageServiceMock = new MockDotMessageService({
    Done: 'Done'
});

const EXPERIMENT_MOCK = getExperimentMock(0);

describe('DotExperimentsConfigurationTrafficAllocationAddComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationTrafficAllocationAddComponent>;
    let store: DotExperimentsConfigurationStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;
    let sidebar: Drawer;

    const createComponent = createComponentFactory({
        imports: [ButtonModule, CardModule, SliderModule, InputTextModule],
        component: DotExperimentsConfigurationTrafficAllocationAddComponent,
        componentProviders: [],
        providers: [
            DotExperimentsConfigurationStore,
            mockProvider(DotExperimentsService),
            mockProvider(MessageService),
            mockProvider(ActivatedRoute, ACTIVE_ROUTE_MOCK_CONFIG),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(ConfirmationService),
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            }
        ]
    });

    beforeEach(async () => {
        spectator = createComponent({
            detectChanges: false
        });

        store = spectator.inject(DotExperimentsConfigurationStore);
        dotExperimentsService = spectator.inject(DotExperimentsService);
        dotExperimentsService.getById.mockReturnValue(of(EXPERIMENT_MOCK));
        dotExperimentsService.setTrafficAllocation.mockReturnValue(of(EXPERIMENT_MOCK));

        store.loadExperiment(EXPERIMENT_MOCK.id);
        store.setSidebarStatus({
            experimentStep: ExperimentSteps.TRAFFIC_LOAD,
            isOpen: true
        });
        spectator.detectChanges();
    });

    it('should load allocation value', () => {
        const slider: Slider = spectator.query(Slider);
        const input: HTMLInputElement = spectator.query(byTestId('traffic-allocation-input'));

        expect(slider.value).toEqual(EXPERIMENT_MOCK.trafficAllocation);
        expect(parseInt(input.value)).toEqual(EXPERIMENT_MOCK.trafficAllocation);
    });

    it('should save form when is valid ', () => {
        jest.spyOn(store, 'setSelectedAllocation');
        const submitButton = spectator.query(
            byTestId('add-trafficAllocation-button')
        ) as HTMLButtonElement;

        expect(submitButton.disabled).toEqual(false);
        expect(submitButton).toContainText('Done');
        expect(spectator.component.form.valid).toEqual(true);

        spectator.click(submitButton);
        expect(store.setSelectedAllocation).toHaveBeenCalledWith({
            trafficAllocation: EXPERIMENT_MOCK.trafficAllocation,
            experimentId: EXPERIMENT_MOCK.id
        });
    });

    it('should set inputs limits', () => {
        const slider: Slider = spectator.query(Slider);

        expect(slider.min).toEqual(1);
        expect(slider.max).toEqual(100);
    });

    it('should close sidebar ', () => {
        jest.spyOn(store, 'closeSidebar');
        sidebar = spectator.query(Drawer);
        sidebar.hide();

        expect(store.closeSidebar).toHaveBeenCalledTimes(1);
    });
});
