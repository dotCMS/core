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
import { Tooltip } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotExperimentStatusList, ExperimentSteps } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotExperimentsConfigurationTrafficSplitAddComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-traffic-split-add/dot-experiments-configuration-traffic-split-add.component';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { getExperimentMock } from '@portlets/dot-experiments/test/mocks';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import { DotExperimentsConfigurationTrafficComponent } from './dot-experiments-configuration-traffic.component';

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
            mockProvider(MessageService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent({
            detectChanges: false
        });

        store = spectator.inject(DotExperimentsConfigurationStore);

        dotExperimentsService = spectator.inject(DotExperimentsService);
        dotExperimentsService.getById.and.returnValue(
            of({ ...EXPERIMENT_MOCK, ...{ scheduling: null } })
        );

        store.loadExperiment(EXPERIMENT_MOCK.id);
        spectator.detectChanges();
    });

    it('should render split and allocation rows', () => {
        expect(spectator.queryAll(Card).length).toEqual(2);
        expect(spectator.query(byTestId('traffic-card-title'))).toHaveText('Traffic');
        expect(spectator.query(byTestId('traffic-allocation-button'))).toExist();
        expect(spectator.query(byTestId('traffic-split-title'))).toHaveText('Split');
        expect(spectator.query(byTestId('traffic-split-change-button'))).toExist();
        expect(spectator.query(byTestId('traffic-step-done'))).toHaveClass('isDone');
    });

    it('should render indicator in gray', () => {
        dotExperimentsService.getById.and.returnValue(
            of({ ...EXPERIMENT_MOCK, ...{ trafficAllocation: null } })
        );

        store.loadExperiment(EXPERIMENT_MOCK.id);
        spectator.detectChanges();

        expect(spectator.query(byTestId('traffic-step-done'))).not.toHaveClass('isDone');
    });

    it('should open sidebar of traffic allocation', () => {
        spyOn(store, 'openSidebar');
        spectator.click(byTestId('traffic-allocation-button'));

        expect(store.openSidebar).toHaveBeenCalledOnceWith(ExperimentSteps.TRAFFIC_LOAD);
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

    it('should disable button and show tooltip when experiment is nos on draft', () => {
        dotExperimentsService.getById.and.returnValue(
            of({
                ...EXPERIMENT_MOCK,
                ...{ status: DotExperimentStatusList.RUNNING }
            })
        );

        store.loadExperiment(EXPERIMENT_MOCK.id);

        spectator.detectChanges();

        expect(spectator.query(byTestId('traffic-allocation-button'))).toHaveAttribute('disabled');

        expect(spectator.query(Tooltip).disabled).toEqual(false);
    });
});
