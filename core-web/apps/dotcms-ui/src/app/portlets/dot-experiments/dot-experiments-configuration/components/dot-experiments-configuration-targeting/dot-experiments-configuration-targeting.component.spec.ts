import {
    byTestId,
    createComponentFactory,
    mockProvider,
    Spectator,
    SpyObject
} from '@ngneat/spectator';
import { of } from 'rxjs';

import { ActivatedRoute } from '@angular/router';

import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { Card, CardModule } from 'primeng/card';
import { Tooltip, TooltipModule } from 'primeng/tooltip';

import { DotMessageService } from '@dotcms/data-access';
import { DotExperimentStatusList } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { ACTIVE_ROUTE_MOCK_CONFIG, getExperimentMock } from '@portlets/dot-experiments/test/mocks';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import { DotExperimentsConfigurationTargetingComponent } from './dot-experiments-configuration-targeting.component';

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
            }
        ]
    });

    beforeEach(() => {
        spectator = createComponent({ detectChanges: false });
        store = spectator.inject(DotExperimentsConfigurationStore);
        dotExperimentsService = spectator.inject(DotExperimentsService);
    });

    it('should render the card and disabled tooltip', () => {
        dotExperimentsService.getById.and.returnValue(of(EXPERIMENT_MOCK));
        store.loadExperiment(EXPERIMENT_MOCK.id);
        spectator.detectChanges();

        expect(spectator.queryAll(Card).length).toEqual(1);
        expect(spectator.query(byTestId('targeting-card-name'))).toHaveText('Targeting');
        expect(spectator.query(byTestId('targeting-add-button'))).toExist();

        expect(spectator.query(Tooltip).disabled).toEqual(true);
    });

    it('should disable button and show tooltip when experiment is not on draft', () => {
        dotExperimentsService.getById.and.returnValue(
            of({
                ...EXPERIMENT_MOCK,
                ...{ status: DotExperimentStatusList.RUNNING }
            })
        );

        store.loadExperiment(EXPERIMENT_MOCK.id);

        spectator.detectChanges();

        expect(spectator.query(byTestId('targeting-add-button'))).toHaveAttribute('disabled');
        expect(spectator.query(Tooltip).disabled).toEqual(false);
    });
});
