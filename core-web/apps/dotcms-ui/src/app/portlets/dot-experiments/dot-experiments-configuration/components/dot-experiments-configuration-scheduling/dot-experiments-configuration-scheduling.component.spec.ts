import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator';
import { of } from 'rxjs';

import { ConfirmationService, MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Card, CardModule } from 'primeng/card';
import { Tooltip, TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotExperimentStatusList, ExperimentSteps } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotExperimentsConfigurationSchedulingAddComponent } from '@portlets/dot-experiments/dot-experiments-configuration/components/dot-experiments-configuration-scheduling-add/dot-experiments-configuration-scheduling-add.component';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { getExperimentMock } from '@portlets/dot-experiments/test/mocks';
import { DotDynamicDirective } from '@portlets/shared/directives/dot-dynamic.directive';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import { DotExperimentsConfigurationSchedulingComponent } from './dot-experiments-configuration-scheduling.component';

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
        dotExperimentsService.getById.and.returnValue(
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
        spyOn(store, 'openSidebar');
        spectator.click(byTestId('scheduling-setup-button'));

        expect(store.openSidebar).toHaveBeenCalledOnceWith(ExperimentSteps.SCHEDULING);
    });

    it('should disable tooltip if is on draft', () => {
        expect(spectator.query(Tooltip).disabled).toEqual(true);
    });

    it('should disable button and show tooltip when experiment is nos on draft', () => {
        dotExperimentsService.getById.and.returnValue(
            of({
                EXPERIMENT_MOCK,
                ...{ scheduling: null, status: DotExperimentStatusList.RUNNING }
            })
        );

        store.loadExperiment(EXPERIMENT_MOCK.id);

        spectator.detectChanges();

        expect(spectator.query(byTestId('scheduling-setup-button'))).toHaveAttribute('disabled');
        expect(spectator.query(Tooltip).disabled).toEqual(false);
    });

    it('should set indicator in green', () => {
        dotExperimentsService.getById.and.returnValue(of(getExperimentMock(0)));

        store.loadExperiment(EXPERIMENT_MOCK.id);
        spectator.detectChanges();

        expect(spectator.query(byTestId('schedule-step-done'))).toHaveClass('isDone');
    });
});
