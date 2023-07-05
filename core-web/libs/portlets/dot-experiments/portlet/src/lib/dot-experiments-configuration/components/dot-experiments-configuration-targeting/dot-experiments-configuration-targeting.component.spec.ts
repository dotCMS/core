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

import { DotMessageService } from '@dotcms/data-access';
import { DotExperimentStatus } from '@dotcms/dotcms-models';
import { DotExperimentsService } from '@dotcms/portlets/dot-experiments/data-access';
import {
    ACTIVE_ROUTE_MOCK_CONFIG,
    getExperimentMock,
    MockDotMessageService
} from '@dotcms/utils-testing';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import { DotExperimentsConfigurationTargetingComponent } from './dot-experiments-configuration-targeting.component';

import { DotExperimentsConfigurationStore } from '../../store/dot-experiments-configuration-store';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.targeting.name': 'Targeting'
});

const EXPERIMENT_MOCK = getExperimentMock(0);

describe('DotExperimentsConfigurationTargetingComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationTargetingComponent>;
    let store: DotExperimentsConfigurationStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;

    const createComponent = createComponentFactory({
        imports: [ButtonModule, CardModule, TooltipModule],
        component: DotExperimentsConfigurationTargetingComponent,
        componentProviders: [],
        providers: [
            DotExperimentsConfigurationStore,
            mockProvider(DotExperimentsService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(MessageService),
            mockProvider(ActivatedRoute, ACTIVE_ROUTE_MOCK_CONFIG),
            {
                provide: DotMessageService,
                useValue: messageServiceMock
            },
            mockProvider(ConfirmationService)
        ]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
        store = spectator.inject(DotExperimentsConfigurationStore);
        dotExperimentsService = spectator.inject(DotExperimentsService);
    });

    it('should render the card and disabled tooltip', () => {
        dotExperimentsService.getById.mockReturnValue(of(EXPERIMENT_MOCK));
        store.loadExperiment(EXPERIMENT_MOCK.id);
        spectator.detectChanges();

        expect(spectator.queryAll(Card).length).toEqual(1);
        expect(spectator.query(byTestId('targeting-card-name'))).toHaveText('Targeting');
        expect(spectator.query(byTestId('targeting-add-button'))).toExist();

        expect(spectator.query(Tooltip).disabled).toEqual(true);
    });

    it('should disable button and show tooltip when experiment is not on draft', () => {
        dotExperimentsService.getById.mockReturnValue(
            of({
                ...EXPERIMENT_MOCK,
                ...{ status: DotExperimentStatus.RUNNING }
            })
        );

        store.loadExperiment(EXPERIMENT_MOCK.id);

        spectator.detectChanges();

        expect(spectator.query(byTestId('targeting-add-button'))).toHaveAttribute('disabled');
        expect(spectator.query(Tooltip).disabled).toEqual(false);
    });
});
