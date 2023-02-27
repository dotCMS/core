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

import { DotMessageService } from '@dotcms/data-access';
import { DotExperimentStatusList } from '@dotcms/dotcms-models';
import { MockDotMessageService } from '@dotcms/utils-testing';
import { DotExperimentsConfigurationStore } from '@portlets/dot-experiments/dot-experiments-configuration/store/dot-experiments-configuration-store';
import { DotExperimentsService } from '@portlets/dot-experiments/shared/services/dot-experiments.service';
import { ExperimentMocks } from '@portlets/dot-experiments/test/mocks';
import { DotHttpErrorManagerService } from '@services/dot-http-error-manager/dot-http-error-manager.service';

import { DotExperimentsConfigurationTargetingComponent } from './dot-experiments-configuration-targeting.component';

const messageServiceMock = new MockDotMessageService({
    'experiments.configure.targeting.name': 'Targeting'
});
describe('DotExperimentsConfigurationTargetingComponent', () => {
    let spectator: Spectator<DotExperimentsConfigurationTargetingComponent>;
    let store: DotExperimentsConfigurationStore;
    let dotExperimentsService: SpyObject<DotExperimentsService>;

    const createComponent = createComponentFactory({
        imports: [ButtonModule, CardModule],
        component: DotExperimentsConfigurationTargetingComponent,
        componentProviders: [],
        providers: [
            DotExperimentsConfigurationStore,
            mockProvider(DotExperimentsService),
            mockProvider(DotHttpErrorManagerService),
            mockProvider(MessageService),
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
        dotExperimentsService.getById.and.returnValue(of({ ...ExperimentMocks[0] }));
        store.loadExperiment(ExperimentMocks[0].id);
        spectator.detectChanges();

        expect(spectator.queryAll(Card).length).toEqual(1);
        expect(spectator.query(byTestId('targeting-card-name'))).toHaveText('Targeting');
        expect(spectator.query(byTestId('targeting-add-button'))).toExist();
        expect(spectator.query(byTestId('tooltip-on-disabled'))).toHaveAttribute(
            'ng-reflect-disabled',
            'true'
        );
    });

    it('should disable button and show tooltip when experiment is not on draft', () => {
        dotExperimentsService.getById.and.returnValue(
            of({
                ...ExperimentMocks[0],
                ...{ status: DotExperimentStatusList.RUNNING }
            })
        );

        store.loadExperiment(ExperimentMocks[0].id);

        spectator.detectChanges();

        expect(spectator.query(byTestId('targeting-add-button'))).toHaveAttribute('disabled');
        expect(spectator.query(byTestId('tooltip-on-disabled'))).toHaveAttribute(
            'ng-reflect-disabled',
            'false'
        );
    });
});
